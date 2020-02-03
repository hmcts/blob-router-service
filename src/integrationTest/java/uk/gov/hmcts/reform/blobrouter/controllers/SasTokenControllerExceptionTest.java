package uk.gov.hmcts.reform.blobrouter.controllers;

import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobContainerClientProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
    "storage.account-name = ",
    "storage.account-key = "
})
public class SasTokenControllerExceptionTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @BeforeEach
    void setUp() {
        WebRequestTrackingFilter filter = new WebRequestTrackingFilter();
        filter.init(new MockFilterConfig());
        mockMvc = webAppContextSetup(wac).addFilters(filter).build();
    }

    @MockBean
    private BlobContainerClientProvider blobContainerClientProvider;

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
