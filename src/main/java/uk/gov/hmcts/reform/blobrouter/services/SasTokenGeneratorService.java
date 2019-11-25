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
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration.StorageConfig;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;

import java.time.OffsetDateTime;
import java.util.Optional;

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
        StorageConfig config = getConfigForService(serviceName);

        BlobContainerSasPermission permissions = new BlobContainerSasPermission()
            .setListPermission(true)
            .setWritePermission(true);

        return new BlobServiceSasSignatureValues()
            .setProtocol(SasProtocol.HTTPS_ONLY) // Users MUST use HTTPS (not HTTP).
            .setContainerName(serviceName)
            .setExpiryTime(OffsetDateTime.now().plusSeconds(config.getSasValidity()))
            .setPermissions(permissions);
    }

    private StorageConfig getConfigForService(String serviceName) {
        return Optional
            .ofNullable(serviceConfiguration.getStorageConfig().get(serviceName))
            .filter(StorageConfig::isEnabled)
            .orElseThrow(() -> new ServiceConfigNotFoundException("No service configuration found for " + serviceName));
    }
}
