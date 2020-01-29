package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "service")
public class ServiceConfiguration {

    private Map<String, StorageConfig> storageConfig;

    public Map<String, StorageConfig> getStorageConfig() {
        return storageConfig;
    }

    public void setStorageConfig(List<StorageConfig> storageConfig) {
        this.storageConfig = storageConfig
            .stream()
            .collect(Collectors.toMap(StorageConfig::getName, Function.identity()));
    }

    public static class StorageConfig {
        private String name;
        private int sasValidity;

        @NotNull
        private TargetStorageAccount targetStorageAccount;

        @NotNull String targetContainer;

        private boolean isEnabled = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSasValidity() {
            return sasValidity;
        }

        public void setSasValidity(int sasValidity) {
            this.sasValidity = sasValidity;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public void setEnabled(boolean enabled) {
            isEnabled = enabled;
        }

        public TargetStorageAccount getTargetStorageAccount() {
            return targetStorageAccount;
        }

        public void setTargetStorageAccount(TargetStorageAccount targetStorageAccount) {
            this.targetStorageAccount = targetStorageAccount;
        }

        public String getTargetContainer() {
            return targetContainer;
        }

        public void setTargetContainer(String targetContainer) {
            this.targetContainer = targetContainer;
        }
    }
}
