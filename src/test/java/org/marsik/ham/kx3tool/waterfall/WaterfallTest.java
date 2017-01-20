package org.marsik.ham.kx3tool.waterfall;


import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.Test;

public class WaterfallTest {
    @Test
    public void testFullPower() throws Exception {
        Waterfall waterfall = new Waterfall();
        double intensity = waterfall.scale(1.0);
        assertThat(intensity)
                .isCloseTo(1.0, Offset.offset(0.1));
    }

    @Test
    public void testHalfPower() throws Exception {
        Waterfall waterfall = new Waterfall();
        double intensity = waterfall.scale(0.5);
        assertThat(intensity)
                .isLessThan(1.0);
    }
}
