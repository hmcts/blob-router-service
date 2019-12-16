package uk.gov.hmcts.reform.blobrouter.tasks;

import com.azure.storage.blob.BlobServiceAsyncClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerProcessor;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;
import uk.gov.hmcts.reform.blobrouter.util.StorageTestBase;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class StorageContainerTaskTest extends StorageTestBase {

    private static ServiceConfiguration SERVICE_CONFIGURATION;

    @Mock
    private ContainerProcessor containerProcessor;

    private StorageContainerTask task;

    @BeforeAll
    public static void setUpClass() {
        setUpTestMode();

        SERVICE_CONFIGURATION = new ServiceConfiguration();
        SERVICE_CONFIGURATION.setStorageConfig(
            asList(
                configure("bulkscan", true),
                configure("disabled-service", false)
            )
        );
    }

    @Override
    protected void beforeTest() {
        BlobServiceAsyncClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);

        task = new StorageContainerTask(storageClient, containerProcessor, SERVICE_CONFIGURATION);
    }

    @DisplayName("Get all 4 configured containers and process only 1 available")
    @Test
    void should_get_all_containers_but_read_only_available_ones(CapturedOutput output) {
        // when
        task.run();

        // then
        verify(containerProcessor, times(1)).process(any());

        // and
        String simpleLoggerName = StorageContainerTask.class.getSimpleName();
        assertThat(output).contains(simpleLoggerName + " Started " + StorageContainerTask.TASK_NAME + " job");
        assertThat(output).contains(simpleLoggerName + " Finished " + StorageContainerTask.TASK_NAME + " job");
    }

    private static ServiceConfiguration.StorageConfig configure(String name, boolean enabled) {
        ServiceConfiguration.StorageConfig config = new ServiceConfiguration.StorageConfig();
        config.setSasValidity(300);
        config.setName(name);
        config.setEnabled(enabled);
        return config;
    }
}
