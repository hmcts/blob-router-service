package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryItem;
import uk.gov.hmcts.reform.blobrouter.services.email.EmailSender;
import uk.gov.hmcts.reform.blobrouter.services.report.ReportCsvWriter;
import uk.gov.hmcts.reform.blobrouter.services.report.ReportService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SendDailyReportTaskTest {
    private SendDailyReportTask sendDailyReportTask;

    @Mock
    private ReportService reportService;

    @Mock
    private ReportCsvWriter reportCsvWriter;

    @Mock
    private EmailSender emailSender;

    @Mock
    private List<EnvelopeSummaryItem> report;

    @Mock
    private File reportFile;

    @Captor
    private ArgumentCaptor<String> subjectCaptor;

    @Captor
    private ArgumentCaptor<String> bodyCaptor;

    @Captor
    private ArgumentCaptor<String> fromCaptor;

    @Captor
    private ArgumentCaptor<String[]> recipientsCaptor;

    @Captor
    private ArgumentCaptor<Map<String, File>> attachmentsCaptor;

    @Test
    void sendReport_should_call_email_sender() throws Exception {
        // given
        String from = "From";

        String[] recipients = new String[]{"rec1", "rec2"};

        sendDailyReportTask = getSendDailyReportTask(from, recipients);

        given(reportService.getDailyReport(getYesterday())).willReturn(report);
        given(reportCsvWriter.writeEnvelopesSummaryToCsv(report)).willReturn(reportFile);

        // when
        sendDailyReportTask.sendReport();

        // then
        verify(emailSender).sendMessageWithAttachments(
            subjectCaptor.capture(),
            bodyCaptor.capture(),
            fromCaptor.capture(),
            recipientsCaptor.capture(),
            attachmentsCaptor.capture()
        );

        assertThat(subjectCaptor.getValue()).isEqualTo(SendDailyReportTask.EMAIL_SUBJECT);
        assertThat(bodyCaptor.getValue()).isEqualTo(SendDailyReportTask.EMAIL_BODY);
        assertThat(fromCaptor.getValue()).isEqualTo(from);
        assertThat(recipientsCaptor.getValue()).isEqualTo(recipients);

        final Map<String, File> attachments = attachmentsCaptor.getValue();
        assertThat(attachments).hasSize(1);

        String attachmentName = attachments.keySet().iterator().next();
        assertThat(attachmentName.startsWith(SendDailyReportTask.ATTACHMENT_PREFIX)).isTrue();
        assertThat(attachmentName.endsWith(SendDailyReportTask.ATTACHMENT_SUFFIX)).isTrue();

        File attachment = attachments.values().iterator().next();
        assertThat(attachment).isEqualTo(reportFile);
    }

    @Test
    void sendReport_should_not_call_email_sender_if_csv_writer_throws() throws Exception {
        // given
        String from = "From";

        String[] recipients = new String[]{"rec1", "rec2"};

        sendDailyReportTask = getSendDailyReportTask(from, recipients);

        given(reportService.getDailyReport(getYesterday())).willReturn(report);
        given(reportCsvWriter.writeEnvelopesSummaryToCsv(report)).willThrow(new IOException());

        // when
        sendDailyReportTask.sendReport();

        // then
        verifyNoInteractions(emailSender);
    }

    @Test
    void should_throw_if_empty_recipients() {
        assertThrows(
            RuntimeException.class,
            () -> getSendDailyReportTask("From", new String[]{})
        );
    }

    private LocalDate getYesterday() {
        return LocalDate.now().minusDays(1);
    }

    private SendDailyReportTask getSendDailyReportTask(String from, String[] recipients) {
        return new SendDailyReportTask(
            reportService,
            reportCsvWriter,
            emailSender,
            from,
            recipients
        );
    }
}
