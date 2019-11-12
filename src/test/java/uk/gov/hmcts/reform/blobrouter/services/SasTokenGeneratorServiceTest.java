package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.StorageImplUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;

import java.time.OffsetDateTime;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SasTokenGeneratorServiceTest {
    private static ServiceConfiguration serviceConfiguration;
    private static SasTokenGeneratorService tokenGeneratorService;

    @BeforeAll
    static void setUp() {
        StorageSharedKeyCredential storageCredentials = new StorageSharedKeyCredential(
            "testAccountName", "dGVzdGtleQ=="
        );

        createAccessTokenConfig();

        tokenGeneratorService = new SasTokenGeneratorService(
            storageCredentials,
            serviceConfiguration
        );
    }

    @Test
    void should_generate_sas_token_when_service_configuration_is_available() {
        String sasToken = tokenGeneratorService.generateSasToken("bulkscan");

        String decodedSasToken = Utility.urlDecode(sasToken);
        Map<String, String[]> queryParams = StorageImplUtils.parseQueryStringSplitValues(decodedSasToken);
        OffsetDateTime now = OffsetDateTime.now();

        assertThat(queryParams.get("sig")).isNotNull();//this is a generated hash of the resource string
        assertThat(queryParams.get("sv")).contains("2019-02-02");//azure api version is latest
        OffsetDateTime expiresAt = OffsetDateTime.parse(queryParams.get("se")[0]); //expiry datetime for the signature
        assertThat(expiresAt).isBetween(now, now.plusSeconds(300));
        assertThat(queryParams.get("sp")).contains("wl");//access permissions(write-w,list-l)
    }

    @Test
    void should_throw_exception_when_requested_service_is_not_configured() {
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken("nonexistingservice"))
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessage("No service configuration found for nonexistingservice");
    }

    @Test
    void should_throw_exception_when_requested_sas_credentials_are_not_configured() {
        tokenGeneratorService = new SasTokenGeneratorService(null, serviceConfiguration);
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken("bulkscan"))
            .isInstanceOf(UnableToGenerateSasTokenException.class)
            .hasMessage("Unable to generate SAS token for service bulkscan");
    }

    private static void createAccessTokenConfig() {
        ServiceConfiguration.ServiceConfig serviceConfig = new ServiceConfiguration.ServiceConfig();
        serviceConfig.setSasValidity(300);
        serviceConfig.setName("bulkscan");

        serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setServicesConfig(singletonList(serviceConfig));
    }
}
