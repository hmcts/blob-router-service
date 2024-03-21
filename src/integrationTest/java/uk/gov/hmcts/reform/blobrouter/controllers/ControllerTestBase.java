package uk.gov.hmcts.reform.blobrouter.controllers;

import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.blobrouter.config.ShedLockConfig;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

/**
 * The `ControllerTestBase` class contains mock bean declarations for `EnvelopeService`, `LockProvider`, and
 * `ShedLockConfig` for use in controller tests.
 */
public class ControllerTestBase {
    @MockBean protected EnvelopeService envelopeService;
    @MockBean protected LockProvider lockProvider;
    @MockBean protected ShedLockConfig shedLockConfig;
}
