package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.services.storage.DuplicateFileHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RejectDuplicatesTaskTest {

    @Test
    void should_call_handler() {
        // given
        var handler = mock(DuplicateFileHandler.class);
        var task = new RejectDuplicatesTask(handler);

        // when
        task.run();

        // then
        verify(handler, times(1)).handle();
    }
}
