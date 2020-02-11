package uk.gov.hmcts.reform.blobrouter.tasks.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggingHelperTest {

    @Mock Logger logger;
    @Mock Runnable action;

    @Test
    void should_run_action_and_surround_it_with_logs() {

        var jobName = "hello";

        LoggingHelper.wrapWithJobLog(logger, jobName, action);

        verify(logger).info("Started {} job", jobName);
        verify(action).run();
        verify(logger).info("Finished {} job", jobName);
    }
}
