package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class RejectedEnvelopesNotificationTest extends FunctionalTestBase {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
    void should_send_notification_for_rejected_envelope() throws Exception {
        System.out.println("[HERE] Starting should_send_notification_for_rejected_envelope");
        // upload file with unique name and without signature
        String fileName = "reject_bulkscan" + randomFileName();
        System.out.println("[HERE] Generated fileName: " + fileName);

        byte[] wrappingZipContent = createZipArchive(singletonList("test-data/envelope/envelope.zip"));
        System.out.println("[HERE] Created zip archive for upload, size: " + wrappingZipContent.length + " bytes");

        // when
        System.out.println("[HERE] Uploading file to container: " + BULK_SCAN_CONTAINER);
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, wrappingZipContent);
        System.out.println("[HERE] Upload complete. Waiting for blob to disappear from source container...");

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(() -> {
                boolean exists = blobExists(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName);
                System.out.println("[HERE] Blob exists after upload? " + exists);
                return !exists;
            });
        System.out.println("[HERE] Blob no longer exists in source container");

        // and
        assertNotificationIsSent(fileName);
        System.out.println("[HERE] Notification assertion complete for file: " + fileName);
    }

    private void assertNotificationIsSent(String fileName) {
        RestAssured
            .given()
            .baseUri(config.blobRouterUrl)
            .relaxedHTTPSValidation()
            .queryParam("file_name", fileName)
            .queryParam("container", BULK_SCAN_CONTAINER)
            .get("/envelopes")
            .then()
            .statusCode(OK.value())
            .body("data[0].status", equalTo(REJECTED.name()))
            .body("data[0].pending_notification", equalTo(false))
            .body("data[0].events.event", hasItems(EventType.REJECTED.name(), EventType.NOTIFICATION_SENT.name())
            );
    }
}
