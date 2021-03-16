package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.*;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class ReportRepositoryTest {

    @Autowired private ReportRepository repo;
    @Autowired private DbHelper dbHelper;

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }


    @Test
    void should_return_envelopes_received_and_status_summary_by_selected_date_created() {

        //given
        LocalDate dateCreated = LocalDate.of(2021, 1, 18);

        final EnvelopeCountSummaryReportItem countSummary1 = new EnvelopeCountSummaryReportItem(
            12, 6, "container1", dateCreated
        );
        final EnvelopeCountSummaryReportItem countSummary2 = new EnvelopeCountSummaryReportItem(
            20, 8, "container2", dateCreated
        );
        final EnvelopeCountSummaryReportItem countSummary3 = new EnvelopeCountSummaryReportItem(
            14, 0, "container3", dateCreated
        );
        final EnvelopeCountSummaryReportItem countSummary4 = new EnvelopeCountSummaryReportItem(
            16, 4, "TEST_CONTAINER", dateCreated
        );
        List<EnvelopeCountSummaryReportItem> envelopeCountSummaryList = new ArrayList<>();
        envelopeCountSummaryList.add(countSummary1);
        envelopeCountSummaryList.add(countSummary2);
        envelopeCountSummaryList.add(countSummary3);
        envelopeCountSummaryList.add(countSummary4);

        given(repo.getReportFor(dateCreated))
            .willReturn(envelopeCountSummaryList);

        // then
        assertThat(repo.getReportFor(dateCreated))
            .extracting(env -> env.container)
            .containsExactlyInAnyOrder("container1", "container3", "container2");
    }

    @Test
    void should_get_empty_result_when_no_incomplete_envelopes_are_there_in_db() {
        //given
        LocalDate dateCreated = LocalDate.of(2021, 1, 18);

        final EnvelopeCountSummaryReportItem countSummary1 = new EnvelopeCountSummaryReportItem(
            16, 4, "TEST_CONTAINER", dateCreated
        );
        List<EnvelopeCountSummaryReportItem> envelopeCountSummaryList = new ArrayList<>();
        envelopeCountSummaryList.add(countSummary1);
        given(repo.getReportFor(dateCreated))
            .willReturn(envelopeCountSummaryList);

        assertThat(repo.getReportFor(dateCreated)).isEmpty();
    }
}
