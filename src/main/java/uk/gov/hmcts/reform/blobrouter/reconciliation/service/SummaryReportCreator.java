package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
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

        var processedEnvelopeSet = newHashSet(processedEnvelopes);
        var supplierEnvelopeSet = newHashSet(supplierEnvelopes);

        Set<SummaryReportItem> receivedButNotReported = difference(processedEnvelopeSet, supplierEnvelopeSet);
        Set<SummaryReportItem> reportedButNotProcessed = difference(supplierEnvelopeSet, processedEnvelopeSet);

        return new SummaryReport(
            actualCount,
            reportedCount,
            newArrayList(receivedButNotReported),
            newArrayList(reportedButNotProcessed)
        );
    }

}
