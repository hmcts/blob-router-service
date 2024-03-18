package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import static java.util.stream.Collectors.toList;

/**
 * The `ServiceConfiguration` class in Java manages storage configuration items and provides methods to interact with
 * source containers.
 */
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

    /**
     * The function `setStorageConfig` sets the storage configuration by converting a list of `StorageConfigItem`
     * objects into a map with the source container as the key.
     *
     * @param storageConfigItems The `storageConfigItems` parameter is a list of `StorageConfigItem` objects.
     */
    public void setStorageConfig(List<StorageConfigItem> storageConfigItems) {
        this.storageConfig = storageConfigItems
            .stream()
            .collect(Collectors.toMap(StorageConfigItem::getSourceContainer, Function.identity()));
    }

    /**
     * The function `getEnabledSourceContainers` returns a list of source containers that are enabled based
     * on the storage configuration.
     *
     * @return A list of source container names that are enabled in the storage configuration.
     */
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
