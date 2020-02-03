package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerCleaner;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeleteDispatchedFilesTaskTest {
    @Mock
    private ContainerCleaner containerCleaner;

    private DeleteDispatchedFilesTask task;

    void setUp(ServiceConfiguration serviceConfiguration) {
        task = new DeleteDispatchedFilesTask(containerCleaner, serviceConfiguration);
    }

    @DisplayName("Get all configured containers and process only 1 available container")
    @Test
    void should_process_all_enabled_containers_and_should_not_process_any_disabled_container() {
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
        verify(containerCleaner).process("bulkscan");
        verifyNoMoreInteractions(containerCleaner);
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
        verifyNoInteractions(containerCleaner); // no available containers
    }

    private StorageConfigItem configure(String name, boolean enabled) {
        StorageConfigItem config = new StorageConfigItem();
        config.setSasValidity(300);
        config.setSourceContainer(name);
        config.setEnabled(enabled);
        return config;
    }
}
