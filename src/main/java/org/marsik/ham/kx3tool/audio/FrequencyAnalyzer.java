package org.marsik.ham.kx3tool.audio;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FrequencyAnalyzer {
    private FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);

    public Complex[] analyze(Complex[] input) {
        return transformer.transform(input, TransformType.FORWARD);
    }
}
