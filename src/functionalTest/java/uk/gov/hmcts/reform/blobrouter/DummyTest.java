package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

class DummyTest {

    private String crimeAccountName;

    @Test
    void should_verify_being_dummy_test() {
        assertThat("I am dummy").isBlank();
    }



    @Test
    void should_verify_config_value_being_dummy_test() {
        System.out.println("value ------>"+crimeAccountName);
        assertThat(crimeAccountName).isNotBlank();
        assertThat(crimeAccountName).isEqualTo("willfail");
    }


}
