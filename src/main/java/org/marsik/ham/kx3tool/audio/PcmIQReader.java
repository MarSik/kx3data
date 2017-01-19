package org.marsik.ham.kx3tool.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.apache.commons.math3.complex.Complex;

public class PcmIQReader extends IQReader {
    public static final int CHANNELS = 2;
    private boolean bigEndian;

    public PcmIQReader(IQSample sampler, boolean bigEndian) {
        super(sampler);
        this.bigEndian = bigEndian;
    }

    @Override
    public Complex[] read(Complex[] template, byte[] data) {
        final int expectedSize = data.length / (CHANNELS * Short.BYTES);
        Complex[] iqData = prepareArray(template, expectedSize);
        assert iqData.length == expectedSize;

        ShortBuffer shortBuffer = ByteBuffer.wrap(data)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer();

        for (int i = 0; i < expectedSize; i++) {
            iqData[i] = sampler.convert(shortBuffer.get(CHANNELS * i) / -Short.MIN_VALUE,
                    shortBuffer.get(CHANNELS * i + 1) / -Short.MIN_VALUE,
                    i, expectedSize);
        }

        return iqData;
    }
}
