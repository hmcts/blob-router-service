package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.SasProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.blobrouter.config.AccessTokenProperties.TokenConfig;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;

import java.time.OffsetDateTime;

@EnableConfigurationProperties(AccessTokenProperties.class)
@Service
public class SasTokenGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    private final StorageSharedKeyCredential storageSharedKeyCredential;
    private final AccessTokenProperties accessTokenProperties;

    public SasTokenGeneratorService(
        StorageSharedKeyCredential storageSharedKeyCredential,
        AccessTokenProperties accessTokenProperties
    ) {
        this.storageSharedKeyCredential = storageSharedKeyCredential;
        this.accessTokenProperties = accessTokenProperties;
    }

    public String generateSasToken(String serviceName) {
        log.info("SAS Token request received for service {} ", serviceName);

        BlobServiceSasSignatureValues sasSignatureBuilder = getBlobServiceSasSignatureValues(serviceName);
        return sasSignatureBuilder.generateSasQueryParameters(storageSharedKeyCredential).encode();
    }

    private BlobServiceSasSignatureValues getBlobServiceSasSignatureValues(String serviceName) {
        TokenConfig config = getTokenConfigForService(serviceName);

        BlobContainerSasPermission permissions = new BlobContainerSasPermission()
            .setListPermission(true)
            .setWritePermission(true);

        return new BlobServiceSasSignatureValues()
            .setProtocol(SasProtocol.HTTPS_ONLY) // Users MUST use HTTPS (not HTTP).
            .setContainerName(serviceName)
            .setExpiryTime(OffsetDateTime.now().plusSeconds(config.getValidity()))
            .setPermissions(permissions);
    }

    private TokenConfig getTokenConfigForService(String serviceName) {
        return accessTokenProperties.getServiceConfig().stream()
            .filter(tokenConfig -> tokenConfig.getServiceName().equalsIgnoreCase(serviceName))
            .findFirst()
            .orElseThrow(
                () -> new ServiceConfigNotFoundException("No service configuration found for service " + serviceName)
            );
    }
}
