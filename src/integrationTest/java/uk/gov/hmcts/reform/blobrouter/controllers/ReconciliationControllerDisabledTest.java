package uk.gov.hmcts.reform.blobrouter.controllers;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {"reconciliation.enabled=false"})
public class ReconciliationControllerDisabledTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_not_found_when_reconciliation_endpoint_is_disabled() throws Exception {
        // given
        String requestBody = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-report.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post("/reform-scan/reconciliation-report/10082020")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isNotFound());
    }
}
