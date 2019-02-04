package io.honeycomb.libhoney.utils;

import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;


public class ObjectUtilsTest {

    @Test
    public void checkThatTheFormatterCanHandleZuluTime() throws ParseException {
        final Date parse = ObjectUtils.getRFC3339DateTimeFormatter().parse("2018-05-01T12:01:00.925Z");

        assertThat(parse).hasMinute(01); 
        assertThat(parse).hasSecond(00);
        assertThat(parse).hasMillisecond(925);
        assertThat(parse).isNotNull();
    }

    @Test
    public void checkThatTheFormatterCanHandleZonedTime() throws ParseException {
        final Date parse = ObjectUtils.getRFC3339DateTimeFormatter().parse("2018-05-01T12:01:00.925+01:00");

        assertThat(parse).hasMinute(01); 
        assertThat(parse).hasSecond(00);
        assertThat(parse).hasMillisecond(925);
        assertThat(parse).isNotNull();
    }
}
