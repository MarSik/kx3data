package org.marsik.ham.kx3tool.serial;

import java.util.WeakHashMap;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialConnection implements SerialPortEventListener, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SerialConnection.class);

    private final SerialPort port;
    private String serialBuffer = "";
    private final String portName;

    private final WeakHashMap<ReceiveListener, Void> receiveListeners = new WeakHashMap<>();
    private final WeakHashMap<CloseListener, Void> closeListeners = new WeakHashMap<>();

    public SerialConnection(String portName, int baudRate) throws SerialPortException {
        this.port = new SerialPort(portName);
        this.portName = portName;
        port.openPort();
        port.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        port.addEventListener(this);
        port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
    }

    public void addReceiveListener(ReceiveListener listener) {
        receiveListeners.put(listener, null);
    }

    public void addCloseListener(CloseListener listener) {
        closeListeners.put(listener, null);
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        logger.debug("SerialEvent rx {}: {}", event.isRXCHAR(), event.getEventValue());

        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                final String data = port.readString(event.getEventValue());
                logger.debug("Received: {}", data);
                synchronized (this) {
                    serialBuffer = serialBuffer + data;
                }
            } catch (SerialPortException e) {
                e.printStackTrace();
            }

            for (ReceiveListener listener: receiveListeners.keySet()) {
                listener.dataReceived(this);
            }
        }
    }

    public String readUntilChar(int character) {
        int index;
        synchronized (this) {
            index = serialBuffer.indexOf(character);
        }

        if (index < 0) {
            return "";
        }

        return readLength(index + 1);
    }

    public String readLength(int size) {
        String ret;
        synchronized (this) {
            ret = serialBuffer.substring(0, size);
            serialBuffer = serialBuffer.substring(size);
        }
        return ret;
    }

    public int readyCount() {
        synchronized (this) {
            return serialBuffer.length();
        }
    }

    public String peek(int chars) {
        synchronized (this) {
            return serialBuffer.substring(0, chars);
        }
    }

    public boolean startsWith(String prefix) {
        synchronized (this) {
            return serialBuffer.startsWith(prefix);
        }
    }

    public void write(String data) throws SerialPortException {
        port.writeString(data);
        logger.debug("Sent: {}", data);
    }

    @Override
    public void close() throws Exception {
        logger.info("Closing serial port " + portName);
        port.closePort();

        for (CloseListener listener: closeListeners.keySet()) {
            listener.closed(this);
        }
    }

    public interface ReceiveListener {
        void dataReceived(SerialConnection connection);
    }

    public interface CloseListener {
        void closed(SerialConnection connection);
    }

    public String getPortName() {
        return portName;
    }
}
