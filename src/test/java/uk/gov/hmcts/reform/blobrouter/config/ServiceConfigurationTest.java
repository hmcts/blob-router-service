package uk.gov.hmcts.reform.blobrouter.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceConfigurationTest {

    @Test
    void should_return_names_of_enabled_source_containers() {
        // given
        var conf = new ServiceConfiguration();
        conf.setStorageConfig(
            asList(
                item("A", "A-rejected", true),
                item("B", "B-rejected", true),
                item("C", "C-rejected", true),
                item("D", "D-rejected", false),
                item("E", "E-rejected", false)
            )
        );

        // when
        List<String> result = conf.getEnabledSourceContainers();

        // then
        assertThat(result).containsExactlyInAnyOrder("A", "B", "C");
    }

    private StorageConfigItem item(String sourceContainer, String targetContainer, boolean enabled) {
        var item = new StorageConfigItem();
        item.setSourceContainer(sourceContainer);
        item.setTargetContainer(targetContainer);
        item.setEnabled(enabled);
        return item;
    }
}
