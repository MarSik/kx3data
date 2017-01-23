package org.marsik.ham.kx3tool.waterfall;

import java.util.stream.DoubleStream;

import lombok.Getter;
import lombok.Setter;
import org.marsik.ham.kx3tool.audio.FftResult;

import lombok.Data;

public class Waterfall {
    @Getter @Setter
    private double referenceLevel = 0.0;
    @Getter @Setter
    private double dynamicRange = 60.0;

    public void lowerReference(double delta) {
        referenceLevel -= delta;
    }

    public void higherReference(double delta) {
        referenceLevel += delta;
    }

    public void increateDynamicRange(double delta) {
        dynamicRange += delta;
    }

    public void decreaseDynamicRange(double delta) {
        dynamicRange -= delta;
    }

    public void reset() {
        referenceLevel = 0.0;
        dynamicRange = 60.0;
    }

    private int valueToIntPixel(double ampl) {
        /*
          int Color = (alpha << 24)
                    + (red << 16)
                    + (green << 8)
                    + blue;
         */

        int i = (int)(255 * scale(ampl));

        // level 0: black background
        if (i < 20)
            return rgb(i, i, i);
        // level 1: black -> blue
        else if ((i >= 20) && (i < 70))
            return rgb(0, 0, 140*(i-20)/50);
        // level 2: blue -> light-blue / greenish
        else if ((i >= 70) && (i < 100))
            return rgb(60*(i-70)/30, 125*(i-70)/30, 115*(i-70)/30 + 140);
        // level 3: light blue -> yellow
        else if ((i >= 100) && (i < 150))
            return rgb(195*(i-100)/50 + 60, 130*(i-100)/50 + 125, 255-(255*(i-100)/50));
        // level 4: yellow -> red
        else if ((i >= 150) && (i < 250))
            return rgb(255, 255-255*(i-150)/100, 0);
        // level 5: red -> white
        else
            return rgb(255, 255*(i-250)/5, 255*(i-250)/5);
    }

    private int rgb(int r, int g, int b) {
        return (0xFF << 24) + (r << 16) + (g << 8) + b;
    }

    /**
     * Convert absolute value to logarithmic scale and represent the logarithm as float between 0 and 1.
     *
     * Reference (maximum value, 0dB) = 1.0
     * Dynamic range limit (minimum value, -DR dB) = 0.0
     *
     * @param ampl absolute value
     * @return
     */
    protected double scale(double ampl) {
        ampl = 20 * Math.log(ampl); // compute the dB below reference level 1.0
        ampl = Math.max(referenceLevel - dynamicRange, ampl); // Limit the bottom
        ampl = Math.min(referenceLevel, ampl); // Limit the top
        ampl += -(referenceLevel - dynamicRange); // Move to positive space
        return ampl / dynamicRange;
    }

    public int[] pixelLine(FftResult fftResult) {
        return DoubleStream.concat(
                DoubleStream.of(fftResult.amplitudes()).skip(fftResult.amplitudes().length / 2),
                DoubleStream.of(fftResult.amplitudes()).limit(fftResult.amplitudes().length / 2)
        ).mapToInt(this::valueToIntPixel)
                .toArray();
    }
}
