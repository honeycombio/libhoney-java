package io.honeycomb.libhoney;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class IsClassicParameterizedTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                "full ingest key string, non classic",
                "hcxik_01hqk4k20cjeh63wca8vva5stw70nft6m5n8wr8f5mjx3762s8269j50wc",
                false
            },
            {
                "ingest key id, non classic",
                "hcxik_01hqk4k20cjeh63wca8vva5stw",
                false
            },
            {
                "full ingest key string,classic",
                "hcaic_1234567890123456789012345678901234567890123456789012345678",
                true
            },
            {
                "ingest key id, classic",
                "hcaic_12345678901234567890123456",
                false
            },
            {
                "v2 configuration key",
                "kgvSpPwegJshQkuowXReLD",
                false
            },
            {
                "classic key",
                "12345678901234567890123456789012",
                true
            },
            {
                "empty string",
                "",
                true
            }
        });
    }

    private final String input;
    private final boolean expected;

    public IsClassicParameterizedTest(String name, String input, boolean expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void test() {
        assertThat(Options.isClassic(input)).isEqualTo(expected);
    }
}
