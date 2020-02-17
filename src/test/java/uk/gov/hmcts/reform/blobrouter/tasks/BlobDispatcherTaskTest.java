package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerProcessor;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BlobDispatcherTaskTest {

    @Mock private ContainerProcessor containerProcessor;
    @Mock private ServiceConfiguration conf;

    @Test
    void should_process_all_available_enabled_containers() {
        // given
        given(conf.getEnabledSourceContainers())
            .willReturn(asList("a", "b", "c"));

        // when
        new BlobDispatcherTask(containerProcessor, conf).run();

        // then
        verify(containerProcessor).process("a");
        verify(containerProcessor).process("b");
        verify(containerProcessor).process("c");

        verifyNoMoreInteractions(containerProcessor);
    }
}
