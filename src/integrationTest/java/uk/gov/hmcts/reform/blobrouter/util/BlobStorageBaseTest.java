package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

/**
 * Spins up test blob storage on Docker.
 */
public abstract class BlobStorageBaseTest {

    protected static final String CONTAINER_NAME = "bulkscan";

    private static DockerComposeContainer dockerComposeContainer;

    protected BlobServiceClient storageClient;
    protected BlobContainerClient containerClient;

    @BeforeAll
    protected static void startBlobStorage() {
        dockerComposeContainer =
            new DockerComposeContainer(new File("src/integrationTest/resources/docker-compose.yml"))
                .withExposedService("azure-storage", 10000);
        dockerComposeContainer.start();
    }

    @AfterAll
    protected static void stopBlobStorage() {
        dockerComposeContainer.stop();
    }

    protected void createContainer() {
        storageClient = new BlobServiceClientBuilder().connectionString("UseDevelopmentStorage=true").buildClient();
        containerClient = storageClient.createBlobContainer(CONTAINER_NAME);
    }

    protected void deleteContainer() {
        storageClient.deleteBlobContainer(CONTAINER_NAME);
    }
}
