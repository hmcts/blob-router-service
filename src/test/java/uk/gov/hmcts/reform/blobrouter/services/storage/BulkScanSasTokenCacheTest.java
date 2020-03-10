package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.SasTokenResponse;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSasTokenException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BulkScanSasTokenCacheTest {

    @Mock
    private BulkScanProcessorClient bulkScanProcessorClient;

    private BulkScanSasTokenCache bulkScanContainerClientCache;

    private long refreshSasBeforeExpiry = 30;

    @BeforeEach
    private void setUp() {
        this.bulkScanContainerClientCache = new BulkScanSasTokenCache(
            bulkScanProcessorClient,
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
            bulkScanContainerClientCache.getSasToken(containerName);

        assertThat(sasToken).isEqualTo(token);
        verify(bulkScanProcessorClient).getSasToken(containerName);
    }

    @Test
    void should_throw_exception_when_sas_expiry_missing() {
        String containerName = "container123";
        String token = "sig=examplesign%3D&sv=2019-02-02&sp=wl&sr=c";
        var sasTokenResponse = new SasTokenResponse(token);

        given(bulkScanProcessorClient.getSasToken(containerName)).willReturn(sasTokenResponse);

        assertThatThrownBy(() -> bulkScanContainerClientCache.getSasToken(containerName))
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
            bulkScanContainerClientCache.getSasToken(containerName);

        assertThat(sasToken).isNotNull();

        String sasToken2 =
            bulkScanContainerClientCache.getSasToken(containerName);

        assertThat(sasToken).isSameAs(sasToken2);
        verify(bulkScanProcessorClient,times(1)).getSasToken(containerName);
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
            bulkScanContainerClientCache.getSasToken(containerName);

        String sasToken2 =
            bulkScanContainerClientCache.getSasToken(containerName);
        assertThat(sasToken1).isNotNull();
        assertThat(sasToken2).isNotNull();

        assertThat(sasToken1).isNotSameAs(sasToken2);
        verify(bulkScanProcessorClient,times(2)).getSasToken(containerName);
    }

}
