package io.honeycomb.libhoney.transport.batch.impl;

import io.honeycomb.libhoney.TestUtils;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HoneycombBatchKeyStrategyTest {
    @Test
    public void GIVEN_anEventWithConfiguredHostSetAndKey_WHEN_invokingKeyStrategy_EXPECT_concatenatedKeyToBeProduced() throws Exception {
        final HoneycombBatchKeyStrategy honeycombBatchKeyStrategy = new HoneycombBatchKeyStrategy();
        final ResolvedEvent eventToKey = TestUtils.createTestEvent();

        final String key = honeycombBatchKeyStrategy.getKey(eventToKey);

        assertThat(key).isEqualTo("http://example.com;testkey;testset");
    }
}
