package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.services.NewEnvelopesFinder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CheckNewEnvelopesTaskTest {

    @Test
    void should_call_new_envelopes_finder() {
        // given
        var envelopesFinder = mock(NewEnvelopesFinder.class);
        var task = new CheckNewEnvelopesTask(envelopesFinder);

        // when
        task.run();

        // then
        verify(envelopesFinder, times(1)).checkNewCftEnvelopesCreated();
        verify(envelopesFinder, times(1)).checkNewEnvelopesCreatedInContainer("pcq", "PCQ");
    }
}
