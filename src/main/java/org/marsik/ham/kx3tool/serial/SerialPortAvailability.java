package org.marsik.ham.kx3tool.serial;

import lombok.Value;

@Value
public class SerialPortAvailability {
    String name;
    boolean available;
}
