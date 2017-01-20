package org.marsik.ham.kx3tool.audio;

import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class FftResult {
    private float samplingRate;
    Complex[] fftResult;

    public double frequency(long baseFrequency, int stepOffset) {
        return baseFrequency + (double)samplingRate / fftResult.length;
    }

    public double[] amplitudes() {
        return Arrays.stream(fftResult).mapToDouble(Complex::abs).toArray();
    }
}
