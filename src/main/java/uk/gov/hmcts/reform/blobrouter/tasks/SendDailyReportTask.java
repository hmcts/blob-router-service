package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryItem;
import uk.gov.hmcts.reform.blobrouter.services.email.EmailSender;
import uk.gov.hmcts.reform.blobrouter.services.report.ReportCsvWriter;
import uk.gov.hmcts.reform.blobrouter.services.report.ReportService;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("!'${spring.mail.host}'.equals(null) "
    + "&& !'${spring.mail.host}'.equals('false') "
    + "&& ${scheduling.task.send-daily-report.enabled:true}")
public class SendDailyReportTask {
    private static final Logger logger = LoggerFactory.getLogger(SendDailyReportTask.class);

    static final String EMAIL_SUBJECT = "Reform Scan daily report";
    static final String EMAIL_BODY = "This is an auto generated email. Do not respond to it.";
    static final String ATTACHMENT_PREFIX = "reform_scan_envelopes_";
    static final String ATTACHMENT_SUFFIX = ".csv";

    private final ReportService reportService;
    private final ReportCsvWriter reportCsvWriter;
    private final EmailSender emailSender;
    private final String from;
    private final String[] recipients;

    // region constructor
    public SendDailyReportTask(
        ReportService reportService,
        ReportCsvWriter reportCsvWriter,
        EmailSender emailSender,
        @Value("${spring.mail.username}") String from,
        @Value("${reports.recipients}") String[] recipients
    ) {
        this.reportService = reportService;
        this.reportCsvWriter = reportCsvWriter;
        this.emailSender = emailSender;
        this.from = from;

        if (recipients == null) {
            this.recipients = new String[0];
        } else {
            this.recipients = Arrays.copyOf(recipients, recipients.length);
        }

        if (this.recipients.length == 0) {
            logger.warn("No recipients configured for reports");
        }
    }
    // endregion

    @Scheduled(cron = "${scheduling.task.send-daily-report.cron}")
    @SchedulerLock(name = "report-sender")
    public void sendReport() {
        if (recipients.length == 0) {
            logger.warn("No recipients configured for reports");
            return;
        }

        final LocalDate reportDate = LocalDate.now();

        final List<EnvelopeSummaryItem> report = reportService.getDailyReport(reportDate);

        try {
            final File reportFile = reportCsvWriter.writeEnvelopesSummaryToCsv(report);

            emailSender.sendMessageWithAttachments(
                EMAIL_SUBJECT,
                EMAIL_BODY,
                from,
                recipients,
                Map.of(getReportAttachmentName(reportDate), reportFile)
            );
        } catch (Exception ex) {
            logger.error(
                "Error sending daily report: {}",
                reportDate,
                ex
            );
        }
    }

    private String getReportAttachmentName(LocalDate reportDate) {
        return ATTACHMENT_PREFIX + reportDate + ATTACHMENT_SUFFIX;
    }
}
