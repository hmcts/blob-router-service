package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.SasProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;

import java.time.OffsetDateTime;

@EnableConfigurationProperties(ServiceConfiguration.class)
@Service
public class SasTokenGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    private final StorageSharedKeyCredential storageSharedKeyCredential;
    private final ServiceConfiguration serviceConfiguration;

    public SasTokenGeneratorService(
        StorageSharedKeyCredential storageSharedKeyCredential,
        ServiceConfiguration serviceConfiguration
    ) {
        this.storageSharedKeyCredential = storageSharedKeyCredential;
        this.serviceConfiguration = serviceConfiguration;
    }

    public String generateSasToken(String serviceName) {
        log.info("Generating SAS Token for {} service", serviceName);

        BlobServiceSasSignatureValues sasSignatureBuilder = getBlobServiceSasSignatureValues(serviceName);
        try {
            return sasSignatureBuilder.generateSasQueryParameters(storageSharedKeyCredential).encode();
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new UnableToGenerateSasTokenException("Unable to generate SAS token for service " + serviceName, e);
        }
    }

    private BlobServiceSasSignatureValues getBlobServiceSasSignatureValues(String serviceName) {
        StorageConfigItem config = getConfigForService(serviceName);

        var permissions = new BlobContainerSasPermission()
            .setReadPermission(true)
            .setWritePermission(true)
            .setListPermission(true);

        return new BlobServiceSasSignatureValues()
            .setContainerName(serviceName)
            .setExpiryTime(OffsetDateTime.now().plusSeconds(config.getSasValidity()))
            .setProtocol(SasProtocol.HTTPS_HTTP)
            .setPermissions(permissions);
    }

    private StorageConfigItem getConfigForService(String serviceName) {
        StorageConfigItem config = serviceConfiguration.getStorageConfig().get(serviceName);
        if (config == null) {
            throw new ServiceConfigNotFoundException("No service configuration found for " + serviceName);
        } else {
            return config;
        }
    }
}
