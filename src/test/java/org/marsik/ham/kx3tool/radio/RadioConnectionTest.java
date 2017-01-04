package org.marsik.ham.kx3tool.radio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;

import org.junit.Test;

public class RadioConnectionTest {
    @Test
    public void testIfParsing() throws Exception {
        final String exampleIf = "IF00014076173     -000000 0006000031 ;";
        final Matcher matcher = RadioConnection.IF_PATTERN.matcher(exampleIf);
        assertThat(matcher.matches())
                .isTrue();
        assertThat(matcher.group("f"))
                .isNotNull()
                .isEqualTo("00014076173");
    }
}
