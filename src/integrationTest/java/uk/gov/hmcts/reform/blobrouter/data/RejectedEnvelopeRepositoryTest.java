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
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelopeRepository;

import java.util.List;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
    void should_get_the_rejected_envelopes_with_notifications_pending() {
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

        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.DISPATCHED, null, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.FILE_PROCESSING_STARTED, null, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.REJECTED, ErrorCode.ERR_AV_FAILED, "notes2"));
        eventRepo.insert(
            new NewEnvelopeEvent(envelopeId4, EventType.REJECTED, ErrorCode.ERR_SIG_VERIFY_FAILED, "notes3")
        );

        /* notifications pending */
        envelopeRepo.updatePendingNotification(envelopeId3, true);
        envelopeRepo.updatePendingNotification(envelopeId4, true);

        // when
        List<RejectedEnvelope> rejectedEnvelopes = rejectedEnvelopeRepo.getRejectedEnvelopes();

        // then
        assertThat(rejectedEnvelopes)
            .hasSize(2)
            .extracting(e -> tuple(e.envelopeId, e.container, e.fileName, e.errorCode, e.errorDescription))
            .containsExactlyInAnyOrder(
                tuple(envelopeId3, "c1", "file3.zip", ErrorCode.ERR_AV_FAILED, "notes2"),
                tuple(envelopeId4, "c2", "file4.zip", ErrorCode.ERR_SIG_VERIFY_FAILED, "notes3")
            );
    }

    @Test
    void should_get_empty_results_when_no_rejected_envelopes_with_pending_notifications_found() {
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

        eventRepo.insert(new NewEnvelopeEvent(envelopeId1, EventType.FILE_PROCESSING_STARTED, null, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.DISPATCHED, null, "notes2"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.ERROR, null, "notes3"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.REJECTED, ErrorCode.ERR_AV_FAILED, "notes2"));
        envelopeRepo.updatePendingNotification(envelopeId3, false); // notification sent

        // when
        List<RejectedEnvelope> rejectedEnvelopes = rejectedEnvelopeRepo.getRejectedEnvelopes();

        // then
        assertThat(rejectedEnvelopes).isEmpty();
    }

    @Test
    void should_filter_the_rejected_envelopes_when_notification_is_already_sent() {
        // given
        /* rejected and notification_sent */
        var envelopeId1 = envelopeRepo.insert(
            new NewEnvelope("c1", "file1.zip", now(), now().plusSeconds(100), Status.REJECTED)
        );
        eventRepo.insert(new NewEnvelopeEvent(envelopeId1, EventType.REJECTED, ErrorCode.ERR_AV_FAILED, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId1, EventType.DELETED, null, "notes1"));
        envelopeRepo.updatePendingNotification(envelopeId1, false);

        /* rejected but notification not sent */
        var envelopeId2 = envelopeRepo.insert(
            new NewEnvelope("c2", "file2.zip", now(), now().plusSeconds(100), Status.REJECTED)
        );
        eventRepo.insert(new NewEnvelopeEvent(envelopeId2, EventType.FILE_PROCESSING_STARTED, null, "notes1"));
        eventRepo.insert(new NewEnvelopeEvent(
            envelopeId2,
            EventType.REJECTED,
            ErrorCode.ERR_METAFILE_INVALID,
            "notes2"
        ));
        envelopeRepo.updatePendingNotification(envelopeId2, true);

        /* not rejected */
        var envelopeId3 = envelopeRepo.insert(
            new NewEnvelope("c1", "file3.zip", now(), now().plusSeconds(100), Status.DISPATCHED)
        );
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.FILE_PROCESSING_STARTED, null, "notes3"));
        eventRepo.insert(new NewEnvelopeEvent(envelopeId3, EventType.DISPATCHED, null, null));

        // when
        List<RejectedEnvelope> rejectedEnvelopes = rejectedEnvelopeRepo.getRejectedEnvelopes();

        // then
        assertThat(rejectedEnvelopes)
            .hasSize(1)
            .extracting(e -> tuple(e.envelopeId, e.container, e.fileName, e.errorCode, e.errorDescription))
            .containsExactlyInAnyOrder(
                tuple(envelopeId2, "c2", "file2.zip", ErrorCode.ERR_METAFILE_INVALID, "notes2")
            );
    }
}
