package uk.gov.hmcts.reform.blobrouter;

import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.restassured.RestAssured;
import uk.gov.hmcts.reform.blobrouter.config.TestConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.OK;

public abstract class FunctionalTestBase {

    protected static final String BULK_SCAN_CONTAINER = "bulkscan";

    protected static TestConfiguration config = new TestConfiguration();

    private static final DateTimeFormatter FILE_NAME_DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

    protected BlobServiceClient blobRouterStorageClient;

    protected void setUp() {
        var blobRouterStorageCredential = new StorageSharedKeyCredential(
            config.sourceStorageAccountName,
            config.sourceStorageAccountKey
        );

        this.blobRouterStorageClient = new BlobServiceClientBuilder()
            .httpClient(
                new NettyAsyncHttpClientBuilder()
                    .proxy(
                        new ProxyOptions(
                            ProxyOptions.Type.HTTP,
                            new InetSocketAddress(
                                "proxyout.reform.hmcts.net",
                                8080
                            )
                        )
                    )
                    .build()
            )
            .credential(blobRouterStorageCredential)
            .endpoint(config.sourceStorageAccountUrl)
            .buildClient();
    }

    protected String randomFileName() {
        return String.format(
            "%s_%s.test.zip",
            ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE),
            LocalDateTime.now().format(FILE_NAME_DATE_TIME_FORMAT)
        );
    }

    protected void assertFileInfoIsStored(String fileName, String containerName, Status status, boolean isDeleted) {
        RestAssured
            .given()
            .baseUri(config.blobRouterUrl)
            .relaxedHTTPSValidation()
            .queryParam("file_name", fileName)
            .queryParam("container", containerName)
            .get("/envelopes")
            .then()
            .statusCode(OK.value())
            .body("data[0].status", equalTo(status.name()))
            .body("data[0].is_deleted", equalTo(isDeleted));
    }
}
