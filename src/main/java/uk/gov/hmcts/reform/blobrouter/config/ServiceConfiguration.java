package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ConfigurationProperties
public class ServiceConfiguration {

    @Value("service")
    private Map<String, ServiceConfig> servicesConfig;

    public Map<String, ServiceConfig> getServicesConfig() {
        return servicesConfig;
    }

    public void setServicesConfig(List<ServiceConfig> servicesConfig) {
        this.servicesConfig = servicesConfig
            .stream()
            .collect(Collectors.toMap(ServiceConfig::getName, config -> config));
    }

    public static class ServiceConfig {
        private String name;
        private int sasValidity;

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
    }
}
