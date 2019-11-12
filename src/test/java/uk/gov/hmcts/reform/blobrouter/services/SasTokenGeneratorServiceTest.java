package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.StorageImplUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SasTokenGeneratorServiceTest {

    private AccessTokenProperties accessTokenProperties;
    private SasTokenGeneratorService tokenGeneratorService;

    @BeforeEach
    void setUp() {
        StorageSharedKeyCredential storageCredentials = new StorageSharedKeyCredential(
            "testAccountName", "dGVzdGtleQ=="
        );

        createAccessTokenConfig();

        tokenGeneratorService = new SasTokenGeneratorService(
            storageCredentials,
            accessTokenProperties
        );
    }

    @Test
    void should_generate_sas_token_when_service_configuration_is_available() {
        String sasToken = tokenGeneratorService.generateSasToken("bulkscan");

        String decodedSasToken = Utility.urlDecode(sasToken);
        Map<String, String[]> queryParams = StorageImplUtils.parseQueryStringSplitValues(decodedSasToken);
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        assertThat(queryParams.get("sig")).isNotNull();//this is a generated hash of the resource string
        assertThat(queryParams.get("se")[0]).startsWith(currentDate);//the expiry date/time for the signature
        assertThat(queryParams.get("sv")).contains("2019-02-02");//azure api version is latest
        assertThat(queryParams.get("sp")).contains("wl");//access permissions(write-w,list-l)
    }

    @Test
    void should_throw_exception_when_requested_service_is_not_configured() {
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken("nonexistingservice"))
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessage("No service configuration found for service nonexistingservice");
    }

    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("bulkscan");

        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}
