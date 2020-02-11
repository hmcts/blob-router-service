package uk.gov.hmcts.reform.blobrouter.tasks.utils;

import org.slf4j.Logger;

public final class LoggingHelper {

    private LoggingHelper() {
        // util class
    }

    public static void wrapWithJobLog(Logger logger, String taskName, Runnable runnable) {
        logger.info("Started {} job", taskName); // DO NOT change this message. App insights alerts are based on it.
        runnable.run();
        logger.info("Finished {} job", taskName);
    }
}
