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
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration.ServiceConfig;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;

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
        log.info("SAS Token request received for service {} ", serviceName);

        BlobServiceSasSignatureValues sasSignatureBuilder = getBlobServiceSasSignatureValues(serviceName);
        return sasSignatureBuilder.generateSasQueryParameters(storageSharedKeyCredential).encode();
    }

    private BlobServiceSasSignatureValues getBlobServiceSasSignatureValues(String serviceName) {
        ServiceConfig config = getConfigForService(serviceName);

        BlobContainerSasPermission permissions = new BlobContainerSasPermission()
            .setListPermission(true)
            .setWritePermission(true);

        return new BlobServiceSasSignatureValues()
            .setProtocol(SasProtocol.HTTPS_ONLY) // Users MUST use HTTPS (not HTTP).
            .setContainerName(serviceName)
            .setExpiryTime(OffsetDateTime.now().plusSeconds(config.getSasValidity()))
            .setPermissions(permissions);
    }

    private ServiceConfig getConfigForService(String serviceName) {
        if (!serviceConfiguration.getServicesConfig().containsKey(serviceName)) {
            throw new ServiceConfigNotFoundException("No service configuration found for service " + serviceName);
        }
        return serviceConfiguration.getServicesConfig().get(serviceName);
    }
}
