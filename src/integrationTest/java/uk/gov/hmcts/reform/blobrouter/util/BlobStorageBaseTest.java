package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Spins up test blob storage on Docker.
 */
public abstract class BlobStorageBaseTest {

    private static DockerComposeContainer dockerComposeContainer;
    protected static BlobServiceClient storageClient;

    private Set<String> createdContainers = new HashSet<>();

    @BeforeAll
    protected static void startBlobStorage() {
        dockerComposeContainer =
            new DockerComposeContainer(new File("src/integrationTest/resources/docker-compose.yml"))
                .withExposedService("azure-storage", 10000);
        dockerComposeContainer.start();

        storageClient = new BlobServiceClientBuilder().connectionString("UseDevelopmentStorage=true").buildClient();
    }

    @AfterAll
    protected static void stopBlobStorage() {
        dockerComposeContainer.stop();
    }

    protected BlobContainerClient createContainer(String containerName) {
        createdContainers.add(containerName);
        return storageClient.createBlobContainer(containerName);
    }

    protected void deleteAllContainers() {
        this.createdContainers.forEach(container -> storageClient.deleteBlobContainer(container));
    }
}
