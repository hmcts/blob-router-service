package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElse;

@Component
public class SummaryReportCreator {

    public SummaryReport createSummaryReport(
        List<SummaryReportItem> receivedEnvelopes,
        List<SummaryReportItem> reportedEnvelopes
    ) {
        List<SummaryReportItem> processedEnvelopes = requireNonNullElse(receivedEnvelopes, emptyList());
        List<SummaryReportItem> supplierEnvelopes = requireNonNullElse(reportedEnvelopes, emptyList());

        int actualCount = processedEnvelopes.size();
        int reportedCount = supplierEnvelopes.size();

        var processedEnvelopeSet = Sets.newHashSet(processedEnvelopes);
        var supplierEnvelopeSet = Sets.newHashSet(supplierEnvelopes);

        Set<SummaryReportItem> receivedButNotReported = Sets.difference(processedEnvelopeSet, supplierEnvelopeSet);
        Set<SummaryReportItem> reportedButNotProcessed = Sets.difference(supplierEnvelopeSet, processedEnvelopeSet);

        return new SummaryReport(
            actualCount,
            reportedCount,
            new ArrayList<>(receivedButNotReported),
            new ArrayList<>(reportedButNotProcessed)
        );
    }

}
