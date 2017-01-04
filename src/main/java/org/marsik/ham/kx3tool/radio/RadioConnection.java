package org.marsik.ham.kx3tool.radio;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jssc.SerialPortException;
import lombok.Data;
import org.marsik.ham.kx3tool.serial.SerialConnection;
import org.marsik.ham.kx3tool.serial.SerialUtil;

@Data
@Singleton
public class RadioConnection {
    @Inject
    SerialUtil serialUtil;

    @Inject
    RadioInfo info;

    WeakHashMap<InfoUpdated, Void> listeners = new WeakHashMap<>();
    WeakHashMap<DataDecoded, Void> dataListeners = new WeakHashMap<>();

    private SerialConnection serialPort;

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
            serialPort.write(cmd);
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    private void receiveData(SerialConnection conn) {
        processReceivedData();
    }

    private void processReceivedData() {
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
            } else if (serialPort.startsWith("FA")) {
                String resp = readSimpleResponse();
                if (resp.isEmpty()) return;

                info.setFrequency(Long.parseLong(resp.substring(2, 13)));
                notifyListeners();
            } else if (serialPort.startsWith("IF")) {
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
                if (serialPort.readyCount() < 5) return;
                String resp = serialPort.peek(5).substring(2);
                int txPending = Integer.valueOf(resp.substring(0, 1));
                int rxReady = Integer.valueOf(resp.substring(1));

                if (serialPort.readyCount() < (6 + rxReady)) return;
                resp = serialPort.readLength(6 + rxReady);
                resp = resp.substring(5);

                for (DataDecoded listener: dataListeners.keySet()) {
                    listener.received(resp);
                }
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

    public void addDataListener(DataDecoded listener) {
        dataListeners.put(listener, null);
    }

    public void notifyListeners() {
        for (InfoUpdated listener: listeners.keySet()) {
            listener.notify(info);
        }
    }

    public interface InfoUpdated {
        void notify(RadioInfo info);
    }

    public interface DataDecoded {
        void received(String s);
    }
}
