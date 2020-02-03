package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfig;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerProcessor;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobDispatcherTaskTest {

    @Mock
    private ContainerProcessor containerProcessor;

    private BlobDispatcherTask task;

    void setUp(ServiceConfiguration serviceConfiguration) {
        task = new BlobDispatcherTask(containerProcessor, serviceConfiguration);
    }

    @DisplayName("Get all configured containers and process only 1 available container")
    @Test
    void should_get_all_containers_but_process_only_available_ones() {
        // given
        var serviceConfiguration = new ServiceConfiguration();
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
    }

    @DisplayName("Get all 2 configured containers and do not process when all containers are disabled")
    @Test
    void should_not_call_container_processor_when_no_available_containers_found() {
        // given
        var serviceConfiguration = new ServiceConfiguration();
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
    }

    private StorageConfig configure(String name, boolean enabled) {
        StorageConfig config = new StorageConfig();
        config.setSasValidity(300);
        config.setSourceContainer(name);
        config.setEnabled(enabled);
        return config;
    }
}
