package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
        repo = mock(ReportRepository.class);
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
            .containsExactlyInAnyOrder("container1", "container3", "container2", "TEST_CONTAINER");
    }

    @Test
    void should_get_empty_result_when_no_envelopes_are_there_in_db() {
        //given
        repo = mock(ReportRepository.class);
        LocalDate dateCreated = LocalDate.of(2021, 1, 18);
        List<EnvelopeCountSummaryReportItem> envelopeCountSummaryList = new ArrayList<>();
        given(repo.getReportFor(dateCreated))
            .willReturn(envelopeCountSummaryList);

        assertThat(repo.getReportFor(dateCreated)).isEmpty();
    }
}
