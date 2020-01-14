package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SasTokenController.class)
@ComponentScan(basePackages = "uk.gov.hmcts.reform.blobrouter")
@TestPropertySource(properties = {
    "storage.account-name = ",
    "storage.account-key = "
})
public class SasTokenControllerExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EnvelopeRepository envelopeRepo;

    @Test
    public void should_throw_exception_when_storage_is_not_configured() throws Exception {
        mockMvc
            .perform(get("/token/bulkscan"))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Exception occurred while generating SAS Token"))
            .andExpect(jsonPath("$.cause").value(UnableToGenerateSasTokenException.class.getName()));
    }
}
