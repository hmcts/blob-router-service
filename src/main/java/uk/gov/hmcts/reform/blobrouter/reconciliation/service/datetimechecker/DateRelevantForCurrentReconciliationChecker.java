package uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker;

import java.time.ZonedDateTime;

/**
 * Checker to ensure the provided DateTime is relevant to currently running reconciliation process.
 * The DateTime will be relevant only if it's before SummaryReport generation time, i.e.:
 * <ul>
 * <li>If SummaryReport is generated every hour until 11:00 for yesterdays envelopes, and we ask if 10:45 yesterday
 * is relevant the result is true</li>
 * <li>If SummaryReport is generated every hour until 11:00 for yesterdays envelopes, and we ask if 11:15 yesterday
 * is relevant the result is false</li>
 * </ul>
 */
public interface DateRelevantForCurrentReconciliationChecker {
    boolean isTimeRelevant(ZonedDateTime dateTime);
}
