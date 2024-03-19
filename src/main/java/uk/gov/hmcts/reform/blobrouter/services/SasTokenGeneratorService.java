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

/**
 * The `SasTokenGeneratorService` class in Java generates SAS tokens for different services using Blob service SAS
 * signature values and handles exceptions related to token generation and configuration retrieval.
 */
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

    /**
     * The function generates a SAS token for a given service using BlobServiceSasSignatureValues
     * and throws an exception if unable to generate the token.
     *
     * @param serviceName The `serviceName` parameter is used to specify the name of the service for
     *                    which the SAS token is being generated. It is passed to the `generateSasToken`
     *                    method to indicate the specific service for which the SAS token is needed.
     * @return The method `generateSasToken` is returning a SAS token as a String for the
     *      specified `serviceName`.
     */
    public String generateSasToken(String serviceName) {
        log.info("Generating SAS Token for {} service", serviceName);

        BlobServiceSasSignatureValues sasSignatureBuilder = getBlobServiceSasSignatureValues(serviceName);
        try {
            return sasSignatureBuilder.generateSasQueryParameters(storageSharedKeyCredential).encode();
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new UnableToGenerateSasTokenException("Unable to generate SAS token for service " + serviceName, e);
        }
    }

    /**
     * The function generates Blob service SAS signature values with read, write, and list permissions for a specified
     * service.
     *
     * @param serviceName The `serviceName` parameter is used to identify the name of the blob
     *                    service for which you want to generate a Shared Access Signature (SAS) token.
     *                    It is passed to the `getBlobServiceSasSignatureValues` method to retrieve the
     *                    configuration settings specific to that service.
     * @return A BlobServiceSasSignatureValues object is being returned with the specified container name, expiry time,
     *      protocol, and permissions set based on the configuration for the given service name.
     */
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

    /**
     * The function `getConfigForService` retrieves the storage configuration for a given service name, throwing an
     * exception if the configuration is not found.
     *
     * @param serviceName The `serviceName` parameter is a String that represents the name of a service for
     *                    which the configuration needs to be retrieved.
     * @return The method `getConfigForService` is returning a `StorageConfigItem` object for the
     *      specified `serviceName`.
     */
    private StorageConfigItem getConfigForService(String serviceName) {
        StorageConfigItem config = serviceConfiguration.getStorageConfig().get(serviceName);
        if (config == null) {
            throw new ServiceConfigNotFoundException("No service configuration found for " + serviceName);
        } else {
            return config;
        }
    }
}
