package uk.gov.hmcts.reform.blobrouter.controllers;

import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.StorageImplUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobContainerClientProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
public class SasTokenControllerTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlobContainerClientProvider blobContainerClientProvider;

    @Test
    public void should_generate_sas_token_when_service_is_configured() throws Exception {
        String sasTokenResponse = mockMvc
            .perform(get("/token/bulkscan"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        final ObjectNode node = new ObjectMapper().readValue(sasTokenResponse, ObjectNode.class);
        final String sasToken = node.get("sas_token").asText();
        assertThat(sasToken).isNotNull();
        Map<String, String[]> queryParams = StorageImplUtils.parseQueryStringSplitValues(Utility.urlDecode(sasToken));

        assertThat(queryParams.get("sig")).isNotNull();//this is a generated hash of the resource string
    }

    @Test
    public void should_throw_exception_when_service_is_not_configured() throws Exception {
        mockMvc
            .perform(get("/token/not-configured-service"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("No service configuration found for not-configured-service"))
            .andExpect(jsonPath("$.cause").value(ServiceConfigNotFoundException.class.getName()));
    }
}
