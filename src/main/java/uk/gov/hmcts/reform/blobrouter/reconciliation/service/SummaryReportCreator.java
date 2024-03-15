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

/**
 * The `SummaryReportCreator` class in Java generates a summary report by comparing two lists of `SummaryReportItem`
 * objects representing envelopes.
 */
@Component
public class SummaryReportCreator {

    /**
     * The `createSummaryReport` function compares two lists of `SummaryReportItem`
     * objects and generates a summary report based on the differences between them.
     *
     * @param receivedEnvelopes receivedEnvelopes is a list of SummaryReportItem
     *                          objects representing envelopes that have been received and processed.
     * @param reportedEnvelopes The `createSummaryReport` method takes two parameters: `receivedEnvelopes`
     *                          and `reportedEnvelopes`.
     * @return A `SummaryReport` object is being returned, which contains the
     *      actual count of processed envelopes, reported count of supplier envelopes, a
     *      list of envelopes received but not reported, and a list of envelopes reported but not
     *      processed.
     */
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
