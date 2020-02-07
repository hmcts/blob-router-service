package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.RejectedContainerCleaner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteRejectedFilesTaskTest {

    @Mock RejectedContainerCleaner cleaner;

    @Test
    void should_call_cleaner() {
        new DeleteRejectedFilesTask(cleaner).run();
        verify(cleaner,times(1)).cleanUp();
    }
}
