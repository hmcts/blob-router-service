package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Component
public class SummaryReportCreator {

    public SummaryReport createSummaryReport(
        List<Envelope> receivedEnvelopes,
        List<uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope> reportedEnvelopes
    ) {
        List<Envelope> processedEnvelopes =
            receivedEnvelopes == null ? emptyList() : receivedEnvelopes;
        List<uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope> supplierEnvelopes
            = reportedEnvelopes == null ? emptyList() : reportedEnvelopes;

        int actualCount = processedEnvelopes.size();
        int reportedCount = supplierEnvelopes.size();

        List<SummaryReportItem> receivedButNotReported = processedEnvelopes
            .stream()
            .filter(e ->
                supplierEnvelopes.stream().noneMatch(s -> isEqualFile(e, s)))
            .map(e -> new SummaryReportItem(e.fileName, e.container))
            .collect(Collectors.toList());

        List<SummaryReportItem> reportedButNotProcessed = supplierEnvelopes
            .stream()
            .filter(s ->
                processedEnvelopes.stream().noneMatch(e -> isEqualFile(e, s)))
            .map(s -> new SummaryReportItem(s.zipFileName, s.container))
            .collect(Collectors.toList());

        return new SummaryReport(actualCount, reportedCount, receivedButNotReported, reportedButNotProcessed);
    }

    private boolean isEqualFile(
        Envelope envelope,
        uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope supplierReportedEnvelope
    ) {
        return envelope.fileName.equals(supplierReportedEnvelope.zipFileName)
            && envelope.container.equals(supplierReportedEnvelope.container);
    }

}
