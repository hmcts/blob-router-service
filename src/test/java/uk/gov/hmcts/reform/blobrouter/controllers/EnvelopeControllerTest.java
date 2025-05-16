package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.blobrouter.model.out.IncompleteEnvelopeInfo;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.IncompleteEnvelopesService;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnvelopeController.class)
class EnvelopeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncompleteEnvelopesService mockIncompleteEnvelopeService;

    @MockitoBean
    private EnvelopeService envelopeService;

    private static final int DEFAULT_STALE_TIME = 168;

    private static final String TEST_UUID_ONE = "b73bd419-7a45-4376-b75a-ec8e595676e9";
    private static final String TEST_UUID_TWO = "ede55b1a-bba7-4b7f-ba4f-b7108b6a27c0";
    private static final String TEST_UUID_THREE = "0397fdbf-fcc3-4d9d-91f7-cf4d4966ee11";
    private final List<IncompleteEnvelopeInfo> incompleteEnvelopeInfos = List.of(
        new IncompleteEnvelopeInfo("container", "file name",
                         UUID.fromString(TEST_UUID_ONE), "2024-01-10T09:22:55Z"
        ),
        new IncompleteEnvelopeInfo("container two", "file name two",
                         UUID.fromString(TEST_UUID_TWO), "2024-01-15T11:14:23Z"
        ),
        new IncompleteEnvelopeInfo("container three", "file name three",
                         UUID.fromString(TEST_UUID_THREE), "2024-01-16T13:44:11Z"
        )
    );

    @Test
    void should_successfully_remove_stale_envelopes() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(DEFAULT_STALE_TIME, List.of(
                      TEST_UUID_ONE, TEST_UUID_TWO, TEST_UUID_THREE
                  ))).willReturn(1);
        given(mockIncompleteEnvelopeService.getIncompleteEnvelopes(anyInt()))
            .willReturn(incompleteEnvelopeInfos);
        performDeleteOneStaleEnvelopes(DEFAULT_STALE_TIME)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(3))
            .andExpect(jsonPath("$.data", hasItem(TEST_UUID_ONE)))
            .andExpect(jsonPath("$.data", hasItem(TEST_UUID_TWO)))
            .andExpect(jsonPath("$.data", hasItem(TEST_UUID_THREE)));
    }

    @Test
    void should_fail_to_remove_all_stale_envelopes_when_invalid_stale_time() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(DEFAULT_STALE_TIME, List.of(TEST_UUID_ONE))).willReturn(1);
        performDeleteOneStaleEnvelopes(44)
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("stale_time must be at least 48 hours")));
    }

    @Test
    void should_successfully_remove_stale_envelope() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(DEFAULT_STALE_TIME, List.of(TEST_UUID_ONE))).willReturn(1);
        performDeleteOneStaleEnvelope(DEFAULT_STALE_TIME, TEST_UUID_ONE)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.data").value(TEST_UUID_ONE));
    }

    @Test
    void should_return_not_found_exception_for_not_found_envelope() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(DEFAULT_STALE_TIME, List.of(TEST_UUID_ONE))).willReturn(0);
        performDeleteOneStaleEnvelope(DEFAULT_STALE_TIME, TEST_UUID_ONE)
            .andExpect(status().isNotFound());
    }

    @Test
    void should_fail_to_remove_stale_envelope_when_invalid_stale_time() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(DEFAULT_STALE_TIME, List.of(TEST_UUID_TWO))).willReturn(1);
        performDeleteOneStaleEnvelope(44, TEST_UUID_TWO)
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("stale_time must be at least 168 hours (a week)")));
    }

    private ResultActions performDeleteOneStaleEnvelope(int staleTime, String envelopeId) throws Exception {
        return mockMvc.perform(delete("/envelopes/stale/{envelopeId}", envelopeId)
                                   .param("stale_time", String.valueOf(staleTime)));
    }

    private ResultActions performDeleteOneStaleEnvelopes(int staleTime) throws Exception {
        return mockMvc.perform(delete("/envelopes/stale/all")
                                   .param("stale_time", String.valueOf(staleTime)));
    }
}
