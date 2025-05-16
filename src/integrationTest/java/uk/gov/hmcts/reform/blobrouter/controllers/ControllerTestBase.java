package uk.gov.hmcts.reform.blobrouter.controllers;

import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.blobrouter.config.ShedLockConfig;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

/**
 * The `ControllerTestBase` class contains mock bean declarations for `EnvelopeService`, `LockProvider`, and
 * `ShedLockConfig` for use in controller tests.
 */
public class ControllerTestBase {
    @MockitoBean protected EnvelopeService envelopeService;
    @MockitoBean protected LockProvider lockProvider;
    @MockitoBean protected ShedLockConfig shedLockConfig;
}
