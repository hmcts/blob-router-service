package uk.gov.hmcts.reform.blobrouter;

import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class CFTBlobDispatchingTest {

    private static final DateTimeFormatter FILE_NAME_DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

    @Test
    void should_move_extracted_crime_envelope_to_crime_storage() throws Exception {
        String fileName = randomFileName();

        byte[] wrappingZipContent = createZipArchive(
            asList("test-data/envelope/crime/envelope.zip", "test-data/envelope/crime/signature")
        );

        Response tokenResponse = RestAssured
            .given()
            .proxy("proxyout.reform.hmcts.net", 8080)
            .relaxedHTTPSValidation()
            .baseUri("http://bulk-scan-processor-aat.service.core-compute-aat.internal")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .when().get("/token/bulkscan")
            .andReturn();

        final ObjectNode node = new ObjectMapper().readValue(tokenResponse.getBody().asString(), ObjectNode.class);
        String sasToken = node.get("sas_token").asText();

        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
            .endpoint("https://bulkscanaat.blob.core.windows.net")
            .sasToken(sasToken)
            .containerName("bulkscan")
            .buildClient();

        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(fileName).getBlockBlobClient();
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(wrappingZipContent)) {
            blockBlobClient.upload(dataStream, wrappingZipContent.length);
        }

    }

    private String randomFileName() {
        return String.format(
            "%s_%s.test.zip",
            ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE),
            LocalDateTime.now().format(FILE_NAME_DATE_TIME_FORMAT)
        );
    }
}
