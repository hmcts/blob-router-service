package uk.gov.hmcts.reform.blobrouter.controllers;

import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.blobrouter.config.ShedLockConfig;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;

public class ControllerTestBase {
    @MockBean protected EnvelopeRepository envelopeRepo;
    @MockBean protected LockProvider lockProvider;
    @MockBean protected ShedLockConfig shedLockConfig;
}
