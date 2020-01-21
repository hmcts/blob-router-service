package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobServiceClientProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
    "storage.account-name = ",
    "storage.account-key = "
})
public class SasTokenControllerExceptionTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlobServiceClientProvider blobServiceClientProvider;

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
