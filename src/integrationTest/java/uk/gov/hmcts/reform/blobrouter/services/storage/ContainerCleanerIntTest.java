package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.blobrouter.config.IntegrationTest;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@RunWith(SpringRunner.class)
public class ContainerCleanerIntTest {
    private static DockerComposeContainer dockerComposeContainer;

    @Test
    public void should_delete_dispatched_files() throws Exception {
        dockerComposeContainer =
            new DockerComposeContainer(new File("src/integrationTest/resources/docker-compose.yml"))
                .withExposedService("azure-storage", 10000);

        dockerComposeContainer.start();
        BlobServiceClient blobServiceClient =
            new BlobServiceClientBuilder()
                .connectionString("UseDevelopmentStorage=true")
                .buildClient();
        BlobContainerClient containerClient = blobServiceClient.createBlobContainer("cont1");
        BlobClient blobClient = containerClient.getBlobClient("test1.zip");
        blobClient.uploadFromFile("src/integrationTest/resources/storage/test1.zip");

        assertThat(containerClient.listBlobs()).hasSize(1);
        assertThat(containerClient.listBlobs().iterator().next().getName()).isEqualTo("test1.zip");

        dockerComposeContainer.stop();
    }
}
