package org.marsik.ham.kx3tool.serial;


import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

@Singleton
public class SerialUtil {
    Map<String, SerialConnection> openPorts = new HashMap<>();

    public List<String> getAvailablePorts() {
        return Collections.unmodifiableList(
                Arrays.asList(SerialPortList.getPortNames()).stream()
                        .filter(p -> !openPorts.containsKey(p))
                        .collect(Collectors.toList()));
    }

    public List<Integer> getAvailableBaudRates() {
        return Collections.unmodifiableList(Arrays.asList(
                SerialPort.BAUDRATE_1200,
                SerialPort.BAUDRATE_4800,
                SerialPort.BAUDRATE_9600,
                SerialPort.BAUDRATE_14400,
                SerialPort.BAUDRATE_19200,
                SerialPort.BAUDRATE_38400,
                SerialPort.BAUDRATE_57600,
                SerialPort.BAUDRATE_115200));
    }

    public SerialConnection open(String portName, int baudRate) throws SerialPortException {
        SerialConnection port = new SerialConnection(portName, baudRate);
        port.addCloseListener(p -> openPorts.remove(p.getPortName()));
        openPorts.put(portName, port);
        return port;
    }
}
