package uk.gov.hmcts.reform.blobrouter.controllers;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
public class ReconciliationControllerTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_success_when_supplier_statement_report_is_valid() throws Exception {
        // given
        String requestBody = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-report.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post("/reform-scan/reconciliation-report/2020-08-10")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty());
    }

    @Test
    void should_return_bad_request_when_supplier_statement_report_is_invalid() throws Exception {
        // given
        String requestBody = Resources.toString(
            getResource("reconciliation/invalid-supplier-statement-report.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post("/reform-scan/reconciliation-report/2020-08-10")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_when_date_is_invalid() throws Exception {
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
            .andExpect(status().isBadRequest());
    }
}
