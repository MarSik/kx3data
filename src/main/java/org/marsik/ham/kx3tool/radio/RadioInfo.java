package org.marsik.ham.kx3tool.radio;

import javax.inject.Singleton;

import lombok.Data;

@Singleton
@Data
public class RadioInfo {
    private RadioModel radioModel = RadioModel.UNKNOWN;
    private long frequency;
    private Mode mode = Mode.UNKNOWN;
    private boolean tx;
    private boolean rit;
    private boolean xit;
    private int offset;

    public enum RadioModel {
        K2,
        K3,
        KX3,
        KX2,
        UNKNOWN
    }

    public enum Mode {
        CW,
        SSB,
        FM,
        AM,
        PSK31,
        PSK63,
        RTTY,
        DATAA,
        AFSKA,
        UNKNOWN
    }
}
