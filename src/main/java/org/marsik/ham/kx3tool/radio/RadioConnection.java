package org.marsik.ham.kx3tool.radio;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jssc.SerialPortException;
import lombok.Data;
import org.marsik.ham.kx3tool.serial.SerialConnection;
import org.marsik.ham.kx3tool.serial.SerialUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@Singleton
public class RadioConnection {
    private static final Logger logger = LoggerFactory.getLogger(RadioConnection.class);
    private static final int DATA_POLL_DELAY_MS = 1500;
    private static final int TX_POLL_DELAY_MS = 500;
    private static final int SAFETY_POLL_DELAY = 250;
    private static final String EOT = "\04"; // Ctrl-D, End of Transmission

    @Inject
    SerialUtil serialUtil;

    @Inject
    RadioInfo info;

    Map<InfoUpdated, Void> listeners = new HashMap<>();
    Map<AdditionalDataEvent, Void> rxDataListeners = new HashMap<>();
    Map<AdditionalDataEvent, Void> queuedDataListeners = new HashMap<>();

    Map<DataPushedFromQueue, Void> dataSentListeners = new HashMap<>();
    Map<DataPushedFromQueue, Void> dataTransmittedListeners = new HashMap<>();

    Timer receiveDataTimer = new Timer("Radio data poller", true);
    ReceiveDataTimerTask receiveDataTimerTask;

    private void clearReceiveTimer() {
        if (receiveDataTimerTask != null) {
            logger.debug("Clearing data receive timer.");
            receiveDataTimerTask.cancel();
            receiveDataTimer.purge();
            receiveDataTimerTask = null;
        }
    }

    private SerialConnection serialPort;

    private String txBuffer = "";
    private int lastRadioTxBufferSize = 0;

    public static Pattern IF_PATTERN = Pattern.compile("IF(?<f>[0-9]{11})     (?<offset>[+-][0-9]{4})(?<rit>[01])(?<xit>[01]) 00(?<tx>[01])(?<mode>.)(?<vfo>[01])(?<scan>[01])(?<split>[01]).(?<data>[0123])1 ;");

    public void open(String port, int baudRate) throws SerialPortException {
        serialPort = serialUtil.open(port, baudRate);
        serialPort.addReceiveListener(this::receiveData);
        sendCommand("K3;"); // Try detecting whether the radio is K3 family (K3, KX3)
    }

    public void close() throws Exception {
        serialPort.close();
        serialPort = null;
    }

    public void sendCommand(String cmd) {
        try {
            if (serialPort != null) {
                serialPort.write(cmd);
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    private void receiveData(SerialConnection conn) {
        processReceivedData();
    }

    private void processReceivedData() {
        logger.debug("Process received data.");

        while (true) {
             if (serialPort.startsWith("K3")) {
                String resp = readSimpleResponse();
                if (resp.isEmpty()) return;

                if (!resp.equals("K31;")) {
                    sendCommand("K31;");
                }

                sendCommand("OM;AI2;IF;"); // Prepare K3 specific mode
                info.setRadioModel(RadioInfo.RadioModel.K3);
                notifyListeners();
                askForData(0); // TODO Ask only in data modes?
            } else if (serialPort.startsWith("FA")) {
                String resp = readSimpleResponse();
                if (resp.isEmpty()) return;

                info.setFrequency(Long.parseLong(resp.substring(2, 13)));
                notifyListeners();
            } else if (serialPort.startsWith("IF")) { // TODO why isn't IF received periodically?
                String resp = readSimpleResponse();
                if (resp.isEmpty()) return;

                Matcher matcher = IF_PATTERN.matcher(resp);
                if (!matcher.matches()) {
                    continue;
                }

                info.setFrequency(Long.parseLong(matcher.group("f")));
                info.setTx(matcher.group("tx").equals("1"));
                info.setRit(matcher.group("rit").equals("1"));
                info.setXit(matcher.group("xit").equals("1"));
                info.setOffset(Integer.parseInt(matcher.group("offset")));
                switch (matcher.group("mode")) {
                    case "1":
                    case "2":
                        info.setMode(RadioInfo.Mode.SSB);
                        break;
                    case "7":
                    case "3":
                        info.setMode(RadioInfo.Mode.CW);
                        break;
                    case "4":
                        info.setMode(RadioInfo.Mode.FM);
                        break;
                    case "5":
                        info.setMode(RadioInfo.Mode.AM);
                        break;
                    case "6":
                    case "9":
                        switch (matcher.group("data")) {
                            case "0":
                                info.setMode(RadioInfo.Mode.DATAA);
                                break;
                            case "1":
                                info.setMode(RadioInfo.Mode.AFSKA);
                                break;
                            case "2":
                                info.setMode(RadioInfo.Mode.RTTY);
                                break;
                            case "3":
                                info.setMode(RadioInfo.Mode.PSK31);
                                break;
                        }
                }
                notifyListeners();
            } else if (serialPort.startsWith("OM")) {
                String resp = readSimpleResponse();
                if (resp.isEmpty()) return;

                if (resp.endsWith("02;")) {
                    info.setRadioModel(RadioInfo.RadioModel.KX3);
                    sendCommand("EL1;"); // Enable KX3 error reporting
                    notifyListeners();
                }
            } else if (serialPort.startsWith("TB")) {
                 logger.debug("Checking readiness of TB: '{}'", serialPort.peek(serialPort.readyCount()));
                 if (serialPort.readyCount() < 5) {
                     logger.debug(".. Number fields not ready.");
                     return;
                 }

                 String resp = serialPort.peek(5).substring(2);
                 int txPending = Integer.valueOf(resp.substring(0, 1));
                 int rxReady = Integer.valueOf(resp.substring(1));

                 final int readyCount = serialPort.readyCount();
                 if (readyCount < (6 + rxReady)) {
                     logger.debug(".. Data block not ready (ready {}, needed {})", readyCount, 6 + rxReady);
                     askForData(SAFETY_POLL_DELAY);
                     return;
                 }

                 resp = serialPort.readLength(6 + rxReady);

                 if (!resp.endsWith(";")) {
                     logger.warn("Invalid message received!");
                     // Read until the next semicolon
                     readSimpleResponse();
                 }

                 resp = resp.substring(5);
                 resp = resp.substring(0, resp.length() - 1);

                 if (!resp.isEmpty()) {
                     for (AdditionalDataEvent listener : rxDataListeners.keySet()) {
                         listener.add(resp);
                     }
                 }

                 // Poll more often when the transmission is in progress, we only
                 // want to keep up-to 9 chars in the radio tx buffer
                 askForData(txPending > 0 ? TX_POLL_DELAY_MS : DATA_POLL_DELAY_MS);

                 // Compute how many characters were actually sent
                 int pushed = Math.max(lastRadioTxBufferSize - txPending, 0);
                 if (pushed > 0) {
                     for (DataPushedFromQueue listener : dataTransmittedListeners.keySet()) {
                         listener.pushed(pushed);
                     }
                     lastRadioTxBufferSize = txPending;
                 }

                 if (txPending < 9) {
                     // It is possible to terminate transmission if the last transmission sent data
                     // and there is nothing left to send
                     sendDataFromBuffer(9 - txPending, pushed > 0);
                 }
             } else if (serialPort.startsWith("?;")) {
                 logger.warn("The last command was not available in the current mode.");
                 readSimpleResponse();
             } else {
                 // Consume and drop the next message
                 String resp = readSimpleResponse();
                 if (resp.isEmpty()) return;
             }
        }
    }

    private String readSimpleResponse() {
        return serialPort.readUntilChar(';');
    }

    public void addInfoUpdateListener(InfoUpdated listener) {
        listeners.put(listener, null);
    }

    public void addReceivedDataListener(AdditionalDataEvent listener) {
        rxDataListeners.put(listener, null);
    }

    public void addQueuedDataListener(AdditionalDataEvent listener) {
        queuedDataListeners.put(listener, null);
    }

    public void addDataSentListener(DataPushedFromQueue listener) {
        dataSentListeners.put(listener, null);
    }

    public void addDataTransmittedListener(DataPushedFromQueue listener) {
        dataTransmittedListeners.put(listener, null);
    }

    public void notifyListeners() {
        for (InfoUpdated listener: listeners.keySet()) {
            listener.notify(info);
        }
    }

    public interface InfoUpdated {
        void notify(RadioInfo info);
    }

    /**
     * Additional data received and decoded or sent to queue for transmit
     */
    public interface AdditionalDataEvent {
        /**
         * @param s The new chunk of data that was received
         */
        void add(String s);
    }

    /**
     * Data from queue sent to radio or transmitted
     */
    public interface DataPushedFromQueue {
        /**
         * @param count Number of characters pushed to the next stage
         */
        void pushed(int count);
    }


    /**
     * Replace (!) the scheduled data poll timer with a new one.
     * @param delayMs The millisecond length of the new wait time period
     *                starting 'now'.
     */
    private void askForData(long delayMs) {
        logger.debug("Asking for more data in {} ms", delayMs);
        clearReceiveTimer();
        receiveDataTimerTask = new ReceiveDataTimerTask();
        receiveDataTimer.schedule(receiveDataTimerTask, delayMs);
    }

    private class ReceiveDataTimerTask extends TimerTask {
        @Override
        public void run() {
            sendCommand("TB;");
        }
    }

    /**
     * Send data from txBuffer to radio for transmission.
     *
     * @param count Number of characters to queue for transmission
     * @param cutoff Should the transmission be immediately terminated when there are no more
     *               characters to transmit?
     */
    private void sendDataFromBuffer(int count, boolean cutoff) {
        if (txBuffer.isEmpty()) {
            if (cutoff) {
                sendCommand("KY " + EOT);
            }
            return;
        }

        if (count == 0) {
            return;
        }

        String part = txBuffer.substring(0, Math.min(count, txBuffer.length()));
        sendCommand("KY " + part + ";");
        txBuffer = txBuffer.substring(part.length());
        askForData(TX_POLL_DELAY_MS);

        for (DataPushedFromQueue listener: dataSentListeners.keySet()) {
            listener.pushed(part.length());
        }

        lastRadioTxBufferSize += part.length();
    }

    public void sendData(String data) {
        txBuffer = txBuffer + data;

        for (AdditionalDataEvent listener: queuedDataListeners.keySet()) {
            listener.add(data);
        }

        processReceivedData();
        sendDataFromBuffer(9 - lastRadioTxBufferSize, false);
    }
}
