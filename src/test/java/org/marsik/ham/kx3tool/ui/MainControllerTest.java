package org.marsik.ham.kx3tool.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MainControllerTest {
    @Test
    public void findEndPrefixMatchExact() throws Exception {
        assertThat(MainController.findEndPrefixMatch("ABCDEF", "ABCDEF"))
                .isEqualTo(6);
    }

    @Test
    public void findEndPrefixMatchPartialLeft() throws Exception {
        assertThat(MainController.findEndPrefixMatch("XYZABCDEF", "ABCDEF"))
                .isEqualTo(6);
    }

    @Test
    public void findEndPrefixMatchPartialRight() throws Exception {
        assertThat(MainController.findEndPrefixMatch("ABCDEF", "ABCDEFGHI"))
                .isEqualTo(6);
    }

    @Test
    public void findEndPrefixMatchPartialBoth() throws Exception {
        assertThat(MainController.findEndPrefixMatch("XYZABCDEF", "ABCDEFGHI"))
                .isEqualTo(6);
    }

    @Test
    public void findEndPrefixMatchMissing() throws Exception {
        assertThat(MainController.findEndPrefixMatch("XABCDF", "ABCDEF"))
                .isEqualTo(0);
    }

    @Test
    public void findEndPrefixMatchMissingLeft() throws Exception {
        assertThat(MainController.findEndPrefixMatch("", "ABCDEAF"))
                .isEqualTo(0);
    }

    @Test
    public void findEndPrefixMatchMissingRight() throws Exception {
        assertThat(MainController.findEndPrefixMatch("ABCDEF", ""))
                .isEqualTo(0);
    }
}
