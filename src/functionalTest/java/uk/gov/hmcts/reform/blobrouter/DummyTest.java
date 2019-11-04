package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DummyTest {

    @Test
    void should_verify_being_dummy_test() {
        assertThat("I am dummy").isNotBlank();
    }
}
