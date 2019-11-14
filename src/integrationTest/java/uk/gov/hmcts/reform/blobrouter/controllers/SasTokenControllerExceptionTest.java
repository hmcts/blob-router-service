package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(controllers = SasTokenController.class)
@ComponentScan(basePackages = "uk.gov.hmcts.reform.blobrouter")
@TestPropertySource(properties = {
    "storage.account-name = ",
    "storage.account-key = "
})
public class SasTokenControllerExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void should_throw_exception_when_storage_is_not_configured() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/token/bulkscan"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(result.getResolvedException())
            .isInstanceOf(UnableToGenerateSasTokenException.class)
            .hasMessage("Unable to generate SAS token for service bulkscan");
    }
}
