package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import static java.util.stream.Collectors.toList;

@ConfigurationProperties(prefix = "service")
public class ServiceConfiguration {

    private Map<String, StorageConfigItem> storageConfig;

    public Map<String, StorageConfigItem> getStorageConfig() {
        return storageConfig;
    }

    private  List<String> sourceContainers;

    @PostConstruct
    void init() {
        sourceContainers = this.storageConfig
            .values()
            .stream()
            .map(StorageConfigItem::getSourceContainer)
            .collect(toList());
    }

    public void setStorageConfig(List<StorageConfigItem> storageConfigItems) {
        this.storageConfig = storageConfigItems
            .stream()
            .collect(Collectors.toMap(StorageConfigItem::getSourceContainer, Function.identity()));
    }

    public List<String> getEnabledSourceContainers() {
        return this.storageConfig
            .values()
            .stream()
            .filter(StorageConfigItem::isEnabled)
            .map(StorageConfigItem::getSourceContainer)
            .collect(toList());
    }

    public List<String> getSourceContainers() {
        return sourceContainers;
    }
}
