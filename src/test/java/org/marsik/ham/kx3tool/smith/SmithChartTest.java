package org.marsik.ham.kx3tool.smith;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.math3.complex.Complex;
import org.assertj.core.data.Offset;
import org.junit.Test;

public class SmithChartTest {
    private Offset<Double> e = Offset.offset(5e-6);

    @Test
    public void test50ohm() throws Exception {
        SmithChart smith = new SmithChart(Complex.valueOf(50));
        Complex plot = smith.plot(smith.resistanceToImpedance(50));

        assertThat(plot.getReal())
                .isEqualTo(0, e);

        assertThat(plot.getImaginary())
                .isEqualTo(0, e);
    }

    @Test
    public void test75ohm() throws Exception {
        SmithChart smith = new SmithChart(Complex.valueOf(50));
        Complex plot = smith.plot(smith.resistanceToImpedance(75));

        assertThat(plot.getReal())
                .isEqualTo(0.2, e);

        assertThat(plot.getImaginary())
                .isEqualTo(0, e);
    }
}
