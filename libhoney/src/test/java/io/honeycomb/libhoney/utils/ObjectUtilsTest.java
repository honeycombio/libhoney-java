package io.honeycomb.libhoney.utils;

import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;


public class ObjectUtilsTest {

    @Test
    public void checkThatTheFormatterCanHandleZuluTime() throws ParseException {
        final Date parse = ObjectUtils.getRFC3339DateTimeFormatter().parse("2018-05-01T12:01:43.925Z");

        assertThat(parse).hasYear(2018);
        assertThat(parse).hasMonth(05);
        assertThat(parse).hasDayOfMonth(1);
        assertThat(parse).hasHourOfDay(5);
        assertThat(parse).hasMinute(01); 
        assertThat(parse).hasSecond(43);
        assertThat(parse).hasMillisecond(925);

    }

    @Test
    public void checkThatTheFormatterCanHandleZonedTime() throws ParseException {
        final Date parse = ObjectUtils.getRFC3339DateTimeFormatter().parse("2018-05-01T12:01:43.925+01:00");

        assertThat(parse).hasYear(2018);
        assertThat(parse).hasMonth(05);
        assertThat(parse).hasDayOfMonth(1);
        assertThat(parse).hasHourOfDay(4);
        assertThat(parse).hasMinute(01); 
        assertThat(parse).hasSecond(43);
        assertThat(parse).hasMillisecond(925);
    }
}
