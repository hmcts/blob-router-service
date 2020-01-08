package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerProcessor;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class BlobDispatcherTaskTest {

    @Mock
    private ContainerProcessor containerProcessor;

    private BlobDispatcherTask task;

    void setUp(ServiceConfiguration serviceConfiguration) {
        task = new BlobDispatcherTask(containerProcessor, serviceConfiguration);
    }

    @DisplayName("Get all configured containers and process only 1 available container")
    @Test
    void should_get_all_containers_but_process_only_available_ones(CapturedOutput output) {
        // given
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setStorageConfig(
            asList(
                configure("bulkscan", true),
                configure("disabled-service", false)
            )
        );
        setUp(serviceConfiguration);

        // when
        task.run();

        // then
        verify(containerProcessor, times(1)).process(any());

        // and
        String simpleLoggerName = BlobDispatcherTask.class.getSimpleName();
        assertThat(output).contains(simpleLoggerName + " Started " + BlobDispatcherTask.TASK_NAME + " job");
        assertThat(output).contains(simpleLoggerName + " Finished " + BlobDispatcherTask.TASK_NAME + " job");
    }

    @DisplayName("Get all 2 configured containers and do not process when all containers are disabled")
    @Test
    void should_not_call_container_processor_when_no_available_containers_found(CapturedOutput output) {
        // given
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setStorageConfig(
            asList(
                configure("bulkscan", false),
                configure("disabled-service", false)
            )
        );
        setUp(serviceConfiguration);

        // when
        task.run();

        // then
        verify(containerProcessor, never()).process(any()); // no available containers

        // and
        String simpleLoggerName = BlobDispatcherTask.class.getSimpleName();
        assertThat(output).contains(simpleLoggerName + " Started " + BlobDispatcherTask.TASK_NAME + " job");
        assertThat(output).contains(simpleLoggerName + " Finished " + BlobDispatcherTask.TASK_NAME + " job");
    }

    private static ServiceConfiguration.StorageConfig configure(String name, boolean enabled) {
        ServiceConfiguration.StorageConfig config = new ServiceConfiguration.StorageConfig();
        config.setSasValidity(300);
        config.setName(name);
        config.setEnabled(enabled);
        return config;
    }
}
