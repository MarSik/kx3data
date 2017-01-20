package org.marsik.ham.kx3tool.waterfall;

import java.util.stream.DoubleStream;

import org.marsik.ham.kx3tool.audio.FftResult;

import javafx.scene.image.PixelFormat;
import javafx.scene.paint.Color;
import lombok.Data;

@Data
public class Waterfall {
    private double referenceLevel = 1.0;
    private double dynamicRange = 1.0;

    private int valueToIntPixel(double ampl) {
        /*
          int Color = (alpha << 24)
                    + (red << 16)
                    + (green << 8)
                    + blue;
         */

        int c = (int)(255 * scale(ampl));
        return (0xFF << 24) + (c << 16) + (c << 8) + c;
    }

    private double scale(double ampl) {
        double value = Math.min(0.0, ampl - 1.0 - referenceLevel); // move reference level to 0
        value /= dynamicRange; // scale dynamic range
        return Math.max(0.0, value + 1.0); // Move back to 0.0 - 1.0 scale
    }

    public int[] pixelLine(FftResult fftResult) {
        return DoubleStream.concat(
                DoubleStream.of(fftResult.amplitudes()).skip(fftResult.amplitudes().length / 2),
                DoubleStream.of(fftResult.amplitudes()).limit(fftResult.amplitudes().length / 2)
        ).mapToInt(this::valueToIntPixel)
                .toArray();
    }
}
