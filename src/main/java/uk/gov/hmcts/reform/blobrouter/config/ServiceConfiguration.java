package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "service")
public class ServiceConfiguration {

    private Map<String, StorageConfig> storageConfig;

    public Map<String, StorageConfig> getStorageConfig() {
        return storageConfig;
    }

    public void setStorageConfig(List<StorageConfig> storageConfig) {
        this.storageConfig = storageConfig
            .stream()
            .collect(Collectors.toMap(StorageConfig::getSourceContainer, Function.identity()));
    }

}
