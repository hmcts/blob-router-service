package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelopeRepository;

import java.util.List;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
class RejectedEnvelopeRepositoryTest {
    @Autowired
    private EnvelopeRepository envelopeRepo;
    @Autowired
    private EnvelopeEventRepository eventRepo;
    @Autowired
    private RejectedEnvelopeRepository rejectedEnvelopeRepo;
    @Autowired
    private DbHelper dbHelper;

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @Test
    void should_get_the_rejected_envelopes() {
        // given
        envelopeRepo.insert(
            new NewEnvelope("c1", "file1.zip", now(), now().plusSeconds(100), Status.REJECTED)
        );
        var envelopeId2 = envelopeRepo.insert(
            new NewEnvelope("c1", "file2.zip", now(), now().plusSeconds(100), Status.DISPATCHED)
        );
        var envelopeId3 = envelopeRepo.insert(
            new NewEnvelope("c1", "file3.zip", now(), now().plusSeconds(100), Status.REJECTED)
        );
        var envelopeId4 = envelopeRepo.insert(
            new NewEnvelope("c2", "file4.zip", now(), now().plusSeconds(100), Status.REJECTED)
        );

        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.DISPATCHED, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.DELETED, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.FILE_PROCESSING_STARTED, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.REJECTED, "notes2"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId4, EventType.REJECTED, "notes3"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId4, EventType.DELETED_FROM_REJECTED, "notes3"));

        // when
        List<RejectedEnvelope> rejectedEnvelopes = rejectedEnvelopeRepo.getRejectedEnvelopes();

        // then
        assertThat(rejectedEnvelopes)
            .hasSize(2)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new RejectedEnvelope(envelopeId3, "c1", "file3.zip", "notes2"),
                new RejectedEnvelope(envelopeId4, "c2", "file4.zip", "notes3")
            );
    }

    @Test
    void should_get_empty_results_when_no_rejected_envelopes_found() {
        // given
        var envelopeId1 = envelopeRepo.insert(
            new NewEnvelope("c1", "file1.zip", now(), now().plusSeconds(100), Status.CREATED)
        );
        var envelopeId2 = envelopeRepo.insert(
            new NewEnvelope("c1", "file2.zip", now(), now().plusSeconds(100), Status.DISPATCHED)
        );
        var envelopeId3 = envelopeRepo.insert(
            new NewEnvelope("c1", "file3.zip", now(), now().plusSeconds(100), Status.REJECTED)
        );

        eventRepo.insert(new NewEnvelopeEvent(envelopeId1, EventType.FILE_PROCESSING_STARTED, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.DISPATCHED, "notes2"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.ERROR, "notes3"));

        // when
        List<RejectedEnvelope> rejectedEnvelopes = rejectedEnvelopeRepo.getRejectedEnvelopes();

        // then
        assertThat(rejectedEnvelopes).isEmpty();
    }

    @Test
    void should_filter_the_rejected_envelopes_when_notification_is_already_sent() {
        // given
        var envelopeId1 = envelopeRepo.insert(
            new NewEnvelope("c1", "file1.zip", now(), now().plusSeconds(100), Status.REJECTED)
        );
        var envelopeId2 = envelopeRepo.insert(
            new NewEnvelope("c2", "file2.zip", now(), now().plusSeconds(100), Status.REJECTED)
        );
        var envelopeId3 = envelopeRepo.insert(
            new NewEnvelope("c1", "file3.zip", now(), now().plusSeconds(100), Status.DISPATCHED)
        );

        /* rejected and notification_sent */
        eventRepo.insert(new NewEnvelopeEvent(envelopeId1, EventType.REJECTED, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId1, EventType.DELETED, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId1, EventType.NOTIFICATION_SENT, null));

        /* rejected but notification not sent */
        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.FILE_PROCESSING_STARTED, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.REJECTED, "notes2"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.DELETED, null));

        /* not rejected */
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.FILE_PROCESSING_STARTED, "notes3"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.DISPATCHED, null));

        // when
        List<RejectedEnvelope> rejectedEnvelopes = rejectedEnvelopeRepo.getRejectedEnvelopes();

        // then
        assertThat(rejectedEnvelopes)
            .hasSize(1)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new RejectedEnvelope(envelopeId2, "c2", "file2.zip", "notes2")
            );
    }
}
