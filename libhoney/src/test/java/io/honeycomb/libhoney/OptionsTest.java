package io.honeycomb.libhoney;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionsTest {

    @Test
    public void Given_null_writekey_and_no_dataset_getDataset_returns_empty() {
        Options options = Options.builder()
            .setWriteKey(null)
            .build();
        assertThat(options.getDataset()).isEqualTo(Options.DEFAULT_DATASET);
    }

    @Test
    public void Given_empty_writekey_and_no_dataset_getDataset_returns_empty() {
        Options options = Options.builder()
            .setWriteKey("")
            .build();
        assertThat(options.getDataset()).isEqualTo(Options.DEFAULT_DATASET);
    }

    @Test
    public void Given_classic_writekey_and_no_dataset_getDataset_returns_empty() {
        Options options = Options.builder()
            .setWriteKey("c1a551c000d68f9ed1e96432ac1a3380")
            .build();
        assertThat(options.getDataset()).isEqualTo(Options.DEFAULT_DATASET);
    }

    @Test
    public void Given_classic_writekey_and_a_whitespace_dataset_getDataset_does_not_trim_whitespace() {
        Options options = Options.builder()
            .setWriteKey("c1a551c000d68f9ed1e96432ac1a3380")
            .setDataset("   ")
            .build();
        assertThat(options.getDataset()).isEqualTo("   ");
    }

    @Test
    public void Given_classic_writekey_and_a_dataset_getDataset_does_not_trim_whitespace() {
        Options options = Options.builder()
            .setWriteKey("c1a551c000d68f9ed1e96432ac1a3380")
            .setDataset(" my-service ")
            .build();
        assertThat(options.getDataset()).isEqualTo(" my-service ");
    }

    @Test
    public void Given_non_classic_writekey_and_null_dataset_getDataset_returns_unknown_service() {
        Options options = Options.builder()
            .setWriteKey("d68f9ed1e96432ac1a3380")
            .setDataset(null)
            .build();
        assertThat(options.getDataset()).isEqualTo(Options.DEFAULT_NON_CLASSIC_DATASET);
    }

    @Test
    public void Given_non_classic_writekey_and_empty_dataset_getDataset_returns_unknown_service() {
        Options options = Options.builder()
            .setWriteKey("d68f9ed1e96432ac1a3380")
            .setDataset("")
            .build();
        assertThat(options.getDataset()).isEqualTo(Options.DEFAULT_NON_CLASSIC_DATASET);
    }

    @Test
    public void Given_non_classic_writekey_and_whitespace_dataset_getDataset_returns_unknown_service() {
        Options options = Options.builder()
            .setWriteKey("d68f9ed1e96432ac1a3380")
            .setDataset("   ")
            .build();
        assertThat(options.getDataset()).isEqualTo(Options.DEFAULT_NON_CLASSIC_DATASET);
    }

    @Test
    public void Given_non_classic_writekey_and_a_dataset_getDataset_trims_whitespace() {
        Options options = Options.builder()
            .setWriteKey("d68f9ed1e96432ac1a3380")
            .setDataset(" my-service ")
            .build();
        assertThat(options.getDataset()).isEqualTo("my-service");
    }
}
