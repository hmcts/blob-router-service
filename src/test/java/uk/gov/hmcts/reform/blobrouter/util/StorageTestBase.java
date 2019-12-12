package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.core.test.TestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Test base to help out repetitive actions for unit testing storage related classes.
 */
public abstract class StorageTestBase extends TestBase {

    @BeforeAll
    protected static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @BeforeEach
    void setUp() {
        setupTest();
    }

    @AfterEach
    void tearDown() {
        teardownTest();
    }

    @Override
    protected String getTestName() {
        return this.getClass().getSimpleName();
    }
}
