package org.marsik.ham.kx3tool.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.apache.commons.math3.complex.Complex;

/**
 * KX3 transmits Q in the Left channel and I in the right channel
 * PCM usually orders data as left (n), right (n+1), left (2*n), right (2*n + 1), ...
 */
public class PcmIQReader extends IQReader {
    public static final int CHANNELS = 2;
    private boolean bigEndian;

    public PcmIQReader(IQSample sampler, boolean bigEndian) {
        super(sampler);
        this.bigEndian = bigEndian;
    }

    @Override
    public Complex[] read(Complex[] template, byte[] data, int offset, int len) {
        final int expectedSize = len / (CHANNELS * Short.BYTES);
        Complex[] iqData = prepareArray(template, expectedSize);
        assert iqData.length == expectedSize;

        ShortBuffer shortBuffer = ByteBuffer.wrap(data, offset, len)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer();
        double maxValue = -Short.MIN_VALUE;

        for (int i = 0; i < expectedSize; i++) {
            iqData[i] = sampler.convert(shortBuffer.get(CHANNELS * i) / maxValue,
                    shortBuffer.get(CHANNELS * i + 1) / maxValue,
                    i, expectedSize);
        }

        return iqData;
    }
}
