package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.clients.pcq.PcqClient;
import uk.gov.hmcts.reform.blobrouter.clients.response.SasTokenResponse;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSasTokenException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SasTokenCacheTest {

    @Mock
    private BulkScanProcessorClient bulkScanProcessorClient;

    @Mock
    private PcqClient pcqClient;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private SasTokenCache sasTokenCache;

    private long refreshSasBeforeExpiry = 30;

    @BeforeEach
    void setUp() {
        this.sasTokenCache = new SasTokenCache(
            bulkScanProcessorClient,
            pcqClient,
            authTokenGenerator,
            refreshSasBeforeExpiry
        );
    }

    @Test
    void should_create_sas_token_when_no_error() {
        String containerName = "container123";
        String token = "sig=examplesign%3D&se=2020-03-05T14%3A54%3A20Z&sv=2019-02-02&sp=wl&sr=c";
        var sasTokenResponse = new SasTokenResponse(token);

        given(bulkScanProcessorClient.getSasToken(containerName)).willReturn(sasTokenResponse);

        String sasToken =
            sasTokenCache.getSasToken(containerName);

        assertThat(sasToken).isEqualTo(token);
        verify(bulkScanProcessorClient).getSasToken(containerName);
    }

    @Test
    void should_throw_exception_when_sas_expiry_missing() {
        String containerName = "container123";
        String token = "sig=examplesign%3D&sv=2019-02-02&sp=wl&sr=c";
        var sasTokenResponse = new SasTokenResponse(token);

        given(bulkScanProcessorClient.getSasToken(containerName)).willReturn(sasTokenResponse);

        assertThatThrownBy(() -> sasTokenCache.getSasToken(containerName))
            .isInstanceOf(InvalidSasTokenException.class)
            .hasMessageContaining("Invalid SAS, the SAS expiration time parameter not found.");
    }

    @Test
    void should_retrieve_sas_token_from_cache_when_value_in_cache() {
        String containerName = "container123";

        String expiryDate = Constants.ISO_8601_UTC_DATE_FORMATTER
            .format(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(refreshSasBeforeExpiry).plusSeconds(30));

        var sasTokenResponse =
            new SasTokenResponse("sig=explesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=kk&sr=k");
        given(bulkScanProcessorClient.getSasToken(containerName)).willReturn(sasTokenResponse);

        String sasToken =
            sasTokenCache.getSasToken(containerName);

        assertThat(sasToken).isNotNull();

        String sasToken2 =
            sasTokenCache.getSasToken(containerName);

        assertThat(sasToken).isSameAs(sasToken2);
        verify(bulkScanProcessorClient, times(1)).getSasToken(containerName);
    }

    @Test
    void should_create_new_sas_token_when_cache_is_expired() {
        String containerName = "container123";

        String expiredDate = Constants.ISO_8601_UTC_DATE_FORMATTER
            .format(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(refreshSasBeforeExpiry));

        String expiryDate = Constants.ISO_8601_UTC_DATE_FORMATTER
            .format(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(refreshSasBeforeExpiry).plusSeconds(1));

        var sasTokenResponse1 =
            new SasTokenResponse("sig=explesign%3D&se=" + Utility.urlEncode(expiredDate) + "&sv=2019-02-02&sp=kk&sr=k");
        var sasTokenResponse2 =
            new SasTokenResponse("sig=explesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=kk&sr=k");

        given(bulkScanProcessorClient.getSasToken(containerName))
            .willReturn(sasTokenResponse1).willReturn(sasTokenResponse2);

        String sasToken1 =
            sasTokenCache.getSasToken(containerName);

        String sasToken2 =
            sasTokenCache.getSasToken(containerName);
        assertThat(sasToken1).isNotNull();
        assertThat(sasToken2).isNotNull();

        assertThat(sasToken1).isNotSameAs(sasToken2);
        verify(bulkScanProcessorClient, times(2)).getSasToken(containerName);
    }

    @Test
    void should_remove_sas_token_from_cache_when_it_is_invalidated() {
        String containerName = "container123";

        String expiryDate = Constants.ISO_8601_UTC_DATE_FORMATTER
            .format(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(refreshSasBeforeExpiry).plusSeconds(30));

        String token1 = "sig=explesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=kk&sr=k";

        String token2 = "sig=edddamplesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=wl&sr=c";

        given(bulkScanProcessorClient.getSasToken(containerName))
            .willReturn(new SasTokenResponse(token1), new SasTokenResponse(token2));

        String sasToken1 =
            sasTokenCache.getSasToken(containerName);

        sasTokenCache.removeFromCache(containerName);

        String sasToken2 =
            sasTokenCache.getSasToken(containerName);

        assertThat(sasToken1).isEqualTo(token1);
        assertThat(sasToken2).isEqualTo(token2);
        assertThat(sasToken1).isNotEqualTo(sasToken2);

        verify(bulkScanProcessorClient, times(2)).getSasToken(containerName);
    }

    @Test
    void should_create_pcq_sas_token_when_token_does_not_exist_in_cache() {
        // given
        String token = "sig=examplesign%3D&se=2020-03-05T14%3A54%3A20Z&sv=2019-02-02&sp=wl&sr=c";
        var sasTokenResponse = new SasTokenResponse(token);

        String authToken = "blob-router-auth-token";
        given(authTokenGenerator.generate()).willReturn(authToken);
        given(pcqClient.getSasToken(authToken)).willReturn(sasTokenResponse);

        // when
        String sasToken = sasTokenCache.getPcqSasToken("pcq");

        // then
        assertThat(sasToken).isEqualTo(token);
        verify(pcqClient).getSasToken(authToken);
    }

    @Test
    void should_retrieve_pcq_sas_token_from_cache_when_token_exists_in_cache() {
        // given
        String containerName = "pcq";
        String authToken = "blob-router-auth-token";

        String expiryDate = Constants.ISO_8601_UTC_DATE_FORMATTER.format(
            OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(refreshSasBeforeExpiry).plusSeconds(30)
        );
        var sasTokenResponse = new SasTokenResponse(
            "sig=explesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=kk&sr=k"
        );

        given(authTokenGenerator.generate()).willReturn(authToken);
        given(pcqClient.getSasToken(authToken)).willReturn(sasTokenResponse);

        // when
        String sasToken = sasTokenCache.getPcqSasToken(containerName);

        // then
        assertThat(sasToken).isNotNull();
        String sasToken2 = sasTokenCache.getPcqSasToken(containerName);

        assertThat(sasToken).isSameAs(sasToken2);
        verify(pcqClient, times(1)).getSasToken(authToken);
    }

    @Test
    void should_remove_pcq_sas_token_from_cache_when_it_is_invalidated() {
        // given
        String containerName = "pcq";
        String authToken = "blob-router-auth-token";

        String expiryDate = Constants.ISO_8601_UTC_DATE_FORMATTER
            .format(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(refreshSasBeforeExpiry).plusSeconds(30));

        String token1 = "sig=explesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=kk&sr=k";
        String token2 = "sig=edddamplesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=wl&sr=c";

        given(authTokenGenerator.generate()).willReturn(authToken);
        given(pcqClient.getSasToken(authToken))
            .willReturn(new SasTokenResponse(token1), new SasTokenResponse(token2));

        // when
        String sasToken1 = sasTokenCache.getPcqSasToken(containerName);
        // invalidate cache for pcq container
        sasTokenCache.removeFromCache(containerName);
        String sasToken2 = sasTokenCache.getPcqSasToken(containerName);

        // then
        assertThat(sasToken1).isEqualTo(token1);
        assertThat(sasToken2).isEqualTo(token2);
        assertThat(sasToken1).isNotEqualTo(sasToken2);

        verify(pcqClient, times(2)).getSasToken(authToken);
    }

}
