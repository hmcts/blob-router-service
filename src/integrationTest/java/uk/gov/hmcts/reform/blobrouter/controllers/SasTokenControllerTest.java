package uk.gov.hmcts.reform.blobrouter.controllers;

import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.StorageImplUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig
@SpringBootTest
@AutoConfigureMockMvc
public class SasTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @Disabled // TODO Disabled until Exception handler is added
    public void should_throw_exception_when_service_is_not_configured() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/token/not-configured-service"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException())
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessage("No service configuration found for not-configured-service");
    }
}
