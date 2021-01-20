package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BlobMetaDataHandlerTest {

    @Mock private BlobClient blobClient;
    @Mock private BlobProperties blobProperties;

    private Map<String, String> blobMetaData;

    private static final String LEASE_EXPIRATION_TIME = "leaseExpirationTime";

    private BlobMetaDataHandler blobMetaDataHandler;
    private static int leaseTimeoutInMin = 15;

    @BeforeEach
    void setUp() {
        blobMetaDataHandler = new BlobMetaDataHandler(leaseTimeoutInMin);
        blobMetaData = new HashMap<>();
    }

    @Test
    void should_return_true_when_no_expiry_in_metadata() {
        //given
        given(blobClient.getProperties()).willReturn(blobProperties);
        given(blobProperties.getMetadata()).willReturn(blobMetaData);
        String etag = "etag-21321312";
        given(blobProperties.getETag()).willReturn(etag);

        LocalDateTime minExpiryTime = LocalDateTime.now(EUROPE_LONDON_ZONE_ID)
            .plusMinutes(leaseTimeoutInMin);
        //when
        boolean isReady = blobMetaDataHandler.isBlobReadyToUse(blobClient);

        //then
        assertThat(isReady).isTrue();
        var mapCapturer = ArgumentCaptor.forClass(Map.class);
        var conditionCapturer = ArgumentCaptor.forClass(BlobRequestConditions.class);
        verify(blobClient)
            .setMetadataWithResponse(mapCapturer.capture(), conditionCapturer.capture(), eq(null), eq(Context.NONE));
        Map<String, String> map = mapCapturer.getValue();
        LocalDateTime leaseExpiresAt = LocalDateTime.parse(map.get(LEASE_EXPIRATION_TIME));
        assertThat(minExpiryTime).isBefore(leaseExpiresAt);
        BlobRequestConditions con = conditionCapturer.getValue();
        assertThat(con.getIfMatch()).isEqualTo("\"" + etag + "\"");
    }

    @Test
    void should_return_false_when_expiry_in_metadata_valid() {
        //given
        given(blobClient.getProperties()).willReturn(blobProperties);
        blobMetaData.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).plusSeconds(40).toString());
        given(blobProperties.getMetadata()).willReturn(blobMetaData);

        //when
        boolean isReady = blobMetaDataHandler.isBlobReadyToUse(blobClient);

        //then
        assertThat(isReady).isFalse();
        verify(blobClient,never()).setMetadata(any());
    }

    @Test
    void should_return_true_when_metadata_lease_expiration_expired() {
        //given
        String etag = "etag-21321312";

        given(blobClient.getProperties()).willReturn(blobProperties);
        blobMetaData.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).toString());
        given(blobProperties.getETag()).willReturn(etag);
        given(blobProperties.getMetadata()).willReturn(blobMetaData);
        LocalDateTime minExpiryTime = LocalDateTime.now(EUROPE_LONDON_ZONE_ID)
            .plusMinutes(leaseTimeoutInMin);
        //when
        boolean isReady = blobMetaDataHandler.isBlobReadyToUse(blobClient);

        //then
        assertThat(isReady).isTrue();
        var mapCapturer = ArgumentCaptor.forClass(Map.class);
        var conditionCapturer = ArgumentCaptor.forClass(BlobRequestConditions.class);
        verify(blobClient)
            .setMetadataWithResponse(mapCapturer.capture(), conditionCapturer.capture(), eq(null), eq(Context.NONE));
        Map<String, String> map = mapCapturer.getValue();
        LocalDateTime leaseExpiresAt = LocalDateTime.parse(map.get(LEASE_EXPIRATION_TIME));
        assertThat(minExpiryTime).isBefore(leaseExpiresAt);
        assertThat(conditionCapturer.getValue().getIfMatch()).isEqualTo("\"" + etag + "\"");
    }

    @Test
    void should_clear_blob_metadata_when_clear_successful() {
        //given
        doNothing().when(blobClient).setMetadata(any());

        //when
        blobMetaDataHandler.clearAllMetaData(blobClient);

        //then
        verify(blobClient).setMetadata(null);

    }
}
