package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Collections.singletonList;
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
        // upload file with unique name and without signature
        String fileName = "reject_bulkscan" + randomFileName();

        byte[] wrappingZipContent = createZipArchive(singletonList("test-data/envelope/envelope.zip"));

        // when
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(() -> !blobExists(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName));

        // and
        assertNotificationIsSent(fileName);
    }

    private void assertNotificationIsSent(String fileName) {
        //        RestAssured
        //            .given()
        //            .baseUri(config.blobRouterUrl)
        //            .relaxedHTTPSValidation()
        //            .queryParam("file_name", fileName)
        //            .queryParam("container", BULK_SCAN_CONTAINER)
        //            .get("/envelopes")
        //            .then()
        //            .statusCode(OK.value())
        //            .body("data[0].status", equalTo(REJECTED.name()))
        //            .body("data[0].pending_notification", equalTo(false))
        //            .body(
        //                "data[0].events.event",
        //                hasItems(EventType.REJECTED.name(),
        //                EventType.NOTIFICATION_SENT.name())
        //            );

        Response response = RestAssured
            .given()
            .baseUri(config.blobRouterUrl)
            .relaxedHTTPSValidation()
            .queryParam("file_name", fileName)
            .queryParam("container", BULK_SCAN_CONTAINER)
            .get("/envelopes");

        await()
            .atMost(30, TimeUnit.SECONDS) // Adjust the time duration as needed
            .until(() -> {
                boolean isResponse200 = response.getStatusCode() == 200;
                String responseData = response.getBody().asString();
                String pendingNotificationValue = response.then().extract().path("data[0].pending_notification");
                String statusValue = response.then().extract().path("data[0].status");
                List<String> events = response.then().extract().path("data[0].events.event");

                // Replace the expected values with the ones you want to wait for
                return isResponse200
                    && responseData != null
                    && "false".equals(pendingNotificationValue)
                    && REJECTED.name().equals(statusValue)
                    && events.contains(EventType.REJECTED.name())
                    && events.contains(EventType.NOTIFICATION_SENT.name());
            });
    }
}
