package org.marsik.ham.kx3tool.radio;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class TransmitStatus {
    String content;
    boolean successful;

    public TransmitStatus signalSuccess(boolean successful) {
        return new TransmitStatus(content, this.successful && successful);
    }
}
