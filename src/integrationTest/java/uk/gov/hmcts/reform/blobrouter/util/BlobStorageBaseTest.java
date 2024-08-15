package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import java.util.HashSet;
import java.util.Set;

/**
 * Spins up test blob storage on Docker.
 */
public abstract class BlobStorageBaseTest {

    private static GenericContainer<?> dockerComposeContainer =
        new GenericContainer<>("hmctspublic.azurecr.io/imported/azure-storage/azurite:3.31.0")
        .withExposedPorts(10000);
    protected static BlobServiceClient storageClient;
    private static String dockerHost;
    private static final String STORAGE_CONN_STRING = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
        + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
        + "BlobEndpoint=http://%s:%d/devstoreaccount1;";

    private Set<String> createdContainers = new HashSet<>();

    @BeforeAll
    protected static void startBlobStorage() {

        dockerComposeContainer.withEnv("executable", "blob");
        dockerComposeContainer.withNetworkAliases("azurite");
        dockerComposeContainer.start();
        dockerHost = dockerComposeContainer.getHost();

        storageClient = new BlobServiceClientBuilder()
            .connectionString(
                String.format(STORAGE_CONN_STRING,
                              dockerHost,
                              dockerComposeContainer.getMappedPort(10000))
            )
            .buildClient();
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
