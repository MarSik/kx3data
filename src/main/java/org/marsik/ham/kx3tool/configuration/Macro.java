package org.marsik.ham.kx3tool.configuration;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Macro {
    String name;
    String value;
}
