package uk.gov.hmcts.reform.blobrouter.controllers;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.reform.blobrouter.config.TestClockProvider;
import uk.gov.hmcts.reform.blobrouter.util.TimeZones;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Import(TestClockProvider.class)
public class ReconciliationControllerTest extends ControllerTestBase {

    public static final String RECONCILIATION_URL = "/reconciliation-report/"
        + LocalDate.now().minusDays(1);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_success_when_supplier_statement_report_is_valid() throws Exception {

        // given
        Instant fiveAM = ZonedDateTime.now(TimeZones.EUROPE_LONDON_ZONE_ID).withHour(5).toInstant();
        givenTheRequestWasMadeAt(fiveAM);
        String requestBody = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-report.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post(RECONCILIATION_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-api-key")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("warning").doesNotHaveJsonPath());
    }


    @Test
    void should_return_warning_message_in_body_when_the_upload_time_makes_the_statement_irrelevant() throws Exception {

        // given
        Instant sevenAM = ZonedDateTime.now(TimeZones.EUROPE_LONDON_ZONE_ID).withHour(7).toInstant();
        givenTheRequestWasMadeAt(sevenAM);
        String requestBody = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-report.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post(RECONCILIATION_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-api-key")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("warning").exists());
    }

    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing() throws Exception {
        // given
        String requestBody = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-report.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post(RECONCILIATION_URL)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("API Key is missing"));
    }

    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_bearer_prefix() throws Exception {
        // given
        String requestBody = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-report.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post(RECONCILIATION_URL)
                    .header(HttpHeaders.AUTHORIZATION, "valid-api-key")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Test
    void should_return_unauthorized_when_authorisation_header_is_invalid() throws Exception {
        // given
        String requestBody = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-report.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post(RECONCILIATION_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-api-key")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
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
                post(RECONCILIATION_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-api-key")
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
                post("/reconciliation-report/10082020")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-api-key")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_when_supplier_statement_has_invalid_containers() throws Exception {
        // given
        String requestBody = Resources.toString(
            getResource("reconciliation/invalid-supplier-statement-with-invalid-containers.json"),
            UTF_8
        );

        // when
        mockMvc
            .perform(
                post(RECONCILIATION_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-api-key")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid statement. Unrecognized Containers : [c1, c2]"));
    }

    private void givenTheRequestWasMadeAt(Instant time) {
        TestClockProvider.stoppedInstant = time;
    }
}
