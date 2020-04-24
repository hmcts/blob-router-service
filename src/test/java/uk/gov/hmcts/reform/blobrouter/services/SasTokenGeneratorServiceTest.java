package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.StorageImplUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;

import java.time.OffsetDateTime;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SasTokenGeneratorServiceTest {
    private static ServiceConfiguration serviceConfiguration;
    private static SasTokenGeneratorService tokenGeneratorService;

    private static final String VALID_SERVICE = "bulkscan";
    private static final String DISABLED_SERVICE = "disabled-service";

    @BeforeAll
    static void setUp() {
        serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setStorageConfig(
            asList(
                cfg(VALID_SERVICE, 300, true),
                cfg(DISABLED_SERVICE, 300, false)
            )
        );

        var storageCredentials = new StorageSharedKeyCredential(
            "testAccountName", "dGVzdGtleQ=="
        );

        tokenGeneratorService = new SasTokenGeneratorService(
            storageCredentials,
            serviceConfiguration,
            true
        );
    }

    @Test
    void should_generate_sas_token_when_service_configuration_is_available() {
        String sasToken = tokenGeneratorService.generateSasToken(VALID_SERVICE);

        String decodedSasToken = Utility.urlDecode(sasToken);
        Map<String, String[]> queryParams = StorageImplUtils.parseQueryStringSplitValues(decodedSasToken);
        OffsetDateTime now = OffsetDateTime.now();

        assertThat(queryParams.get("sig")).isNotNull();//this is a generated hash of the resource string
        assertThat(queryParams.get("sv")).contains("2019-07-07");//azure api version is latest
        OffsetDateTime expiresAt = OffsetDateTime.parse(queryParams.get("se")[0]); //expiry datetime for the signature
        assertThat(expiresAt).isBetween(now, now.plusSeconds(300));
        assertThat(queryParams.get("sp")).contains("wl");//access permissions(write-w,list-l)
        assertThat(queryParams.get("spr")).containsExactlyInAnyOrder("https", "http");
    }

    @Test
    void should_set_the_right_permissions_in_token() {
        String tokenWithWritePermission =
            tokenGeneratorService(true).generateSasToken(VALID_SERVICE);

        assertTokenHasPermissions(tokenWithWritePermission, "wl");

        String tokenWithoutWritePermission =
            tokenGeneratorService(false).generateSasToken(VALID_SERVICE);

        assertTokenHasPermissions(tokenWithoutWritePermission, "cl");
    }

    @Test
    void should_throw_exception_when_requested_service_is_not_configured() {
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken("nonexistingservice"))
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessage("No service configuration found for nonexistingservice");
    }

    @Test
    void should_throw_exception_when_service_is_disabled() {
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken(DISABLED_SERVICE))
            .isInstanceOf(ServiceDisabledException.class)
            .hasMessageContaining("Service " + DISABLED_SERVICE + " has been disabled.");
    }

    @Test
    void should_throw_exception_when_requested_sas_credentials_are_not_configured() {
        tokenGeneratorService = new SasTokenGeneratorService(null, serviceConfiguration, true);
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken(VALID_SERVICE))
            .isInstanceOf(UnableToGenerateSasTokenException.class)
            .hasMessage("Unable to generate SAS token for service " + VALID_SERVICE);
    }

    private static StorageConfigItem cfg(String name, int validity, boolean enabled) {
        StorageConfigItem config = new StorageConfigItem();
        config.setSasValidity(validity);
        config.setSourceContainer(name);
        config.setEnabled(enabled);
        return config;
    }

    private SasTokenGeneratorService tokenGeneratorService(boolean includeWritePermissionInSasToken) {
        var storageCredentials = new StorageSharedKeyCredential(
            "testAccountName", "dGVzdGtleQ=="
        );

        return new SasTokenGeneratorService(
            storageCredentials,
            serviceConfiguration,
            includeWritePermissionInSasToken
        );
    }

    private void assertTokenHasPermissions(String sasToken, String expectedPermissions) {
        String decodedSasToken = Utility.urlDecode(sasToken);
        Map<String, String[]> queryParams = StorageImplUtils.parseQueryStringSplitValues(decodedSasToken);
        assertThat(queryParams.get("sp")).isEqualTo(new String[]{expectedPermissions});
    }
}
