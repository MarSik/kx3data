package org.marsik.ham.kx3tool.audio;

import org.apache.commons.math3.complex.Complex;

public class HammingIqSampler implements IQReader.IQSample{
    private double[] windowCoefficients;

    /**
     *
     * @param size FFT size, MUST be power of two
     */
    public HammingIqSampler(int size) {
        windowCoefficients = new double[size];
        for (int i = 0; i < size; i++) {
            windowCoefficients[i] = hammingWindow(i, size);
        }
    }

    private double hammingWindow(int n, int N) {
        return 0.53836 - 0.46164 * Math.cos(2 * Math.PI * n / (N - 1));
    }

    @Override
    public Complex convert(double dataI, double dataQ, int sample, int outOf) {
        return Complex.valueOf(dataI * windowCoefficients[sample],
                dataQ * windowCoefficients[sample]);
    }
}
