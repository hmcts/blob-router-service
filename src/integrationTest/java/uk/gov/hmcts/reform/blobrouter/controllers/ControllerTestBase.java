package uk.gov.hmcts.reform.blobrouter.controllers;

import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.blobrouter.config.ShedLockConfig;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.TeamEnvelopeService;

public class ControllerTestBase {
    @MockBean protected EnvelopeService envelopeService;
    @MockBean protected TeamEnvelopeService teamEnvelopeService;
    @MockBean protected LockProvider lockProvider;
    @MockBean protected ShedLockConfig shedLockConfig;
}
