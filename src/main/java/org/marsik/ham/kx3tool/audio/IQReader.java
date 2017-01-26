package org.marsik.ham.kx3tool.audio;

import org.apache.commons.math3.complex.Complex;

public abstract class IQReader {
    public interface IQSample {
        Complex convert(double dataI, double dataQ, int sample, int outOf);
    }

    protected IQSample sampler;

    public IQReader(IQSample sampler) {
        this.sampler = sampler;
    }

    public abstract Complex[] read(Complex[] template, byte[] data, int offset, int len);

    protected Complex[] prepareArray(Complex[] template, int size) {
        if (template != null && template.length == size) {
            return template;
        } else {
            return new Complex[size];
        }
    }
}
