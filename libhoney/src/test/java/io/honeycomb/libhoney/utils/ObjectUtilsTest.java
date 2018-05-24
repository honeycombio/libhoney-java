package io.honeycomb.libhoney.utils;

import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;


public class ObjectUtilsTest {

    @Test
    public void checkThatTheFormatterCanHandleZuluTime() throws ParseException {
        final Date parse = ObjectUtils.getRFC3339DateTimeFormatter().parse("2018-05-01T12:01:00.0925Z");

        assertThat(parse).isNotNull();
    }

    @Test
    public void checkThatTheFormatterCanHandleZonedTime() throws ParseException {
        final Date parse = ObjectUtils.getRFC3339DateTimeFormatter().parse("2018-05-01T12:01:00.0925+01:00");

        assertThat(parse).isNotNull();
    }
}
