package uk.gov.hmcts.reform.blobrouter.services.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryItem;
import uk.gov.hmcts.reform.blobrouter.services.email.EmailSender;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReportSenderTest {
    private ReportSender reportSender;

    @Mock
    private ReportService reportService;

    @Mock
    private ReportCsvWriter reportCsvWriter;

    @Mock
    private EmailSender emailSender;

    private static final String FROM = "From";

    private String[] recipients = new String[]{"rec1", "rec2"};

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

    @BeforeEach
    void setUp() {
        reportSender = new ReportSender(
            reportService,
            reportCsvWriter,
            emailSender,
            FROM,
            recipients
        );
    }

    @Test
    void sendReport_should_call_email_sender() throws Exception {
        // given
        given(reportService.getDailyReport(any(LocalDate.class))).willReturn(report);
        given(reportCsvWriter.writeEnvelopesSummaryToCsv(report)).willReturn(reportFile);

        // when
        reportSender.sendReport();

        // then
        verify(reportService).getDailyReport(any(LocalDate.class));
        verify(reportCsvWriter).writeEnvelopesSummaryToCsv(report);
        verify(emailSender).sendMessageWithAttachments(
            subjectCaptor.capture(),
            bodyCaptor.capture(),
            fromCaptor.capture(),
            recipientsCaptor.capture(),
            attachmentsCaptor.capture()
        );

        assertThat(subjectCaptor.getValue()).isEqualTo(ReportSender.EMAIL_SUBJECT);
        assertThat(bodyCaptor.getValue()).isEqualTo(ReportSender.EMAIL_BODY);
        assertThat(fromCaptor.getValue()).isEqualTo(FROM);
        assertThat(recipientsCaptor.getValue()).isEqualTo(recipients);

        final Map<String, File> attachments = attachmentsCaptor.getValue();
        assertThat(attachments).hasSize(1);

        String attachmentName = attachments.keySet().iterator().next();
        assertThat(attachmentName.startsWith(ReportSender.ATTACHMENT_PREFIX)).isTrue();
        assertThat(attachmentName.endsWith(ReportSender.ATTACHMENT_SUFFIX)).isTrue();

        File attachment = attachments.values().iterator().next();
        assertThat(attachment).isEqualTo(reportFile);
    }

    @Test
    void sendReport_should_not_call_email_sender_if_csv_writer_throws() throws Exception {
        // given
        given(reportService.getDailyReport(any(LocalDate.class))).willReturn(report);
        given(reportCsvWriter.writeEnvelopesSummaryToCsv(report)).willThrow(new IOException());

        // when
        reportSender.sendReport();

        // then
        verify(reportService).getDailyReport(any(LocalDate.class));
        verify(reportCsvWriter).writeEnvelopesSummaryToCsv(report);
        verifyNoInteractions(emailSender);
    }

    @Test
    void sendReport_should_not_call_email_sender_if_empty_recipients() throws Exception {
        // given
        reportSender = new ReportSender(
            reportService,
            reportCsvWriter,
            emailSender,
            FROM,
            new String[]{}
        );

        // when
        reportSender.sendReport();

        // then
        verifyNoInteractions(reportService);
        verifyNoInteractions(reportCsvWriter);
        verifyNoInteractions(emailSender);
    }
}
