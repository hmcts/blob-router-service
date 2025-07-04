package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;
import uk.gov.hmcts.reform.blobrouter.services.storage.StaleBlobFinder;
import uk.gov.hmcts.reform.blobrouter.util.DateFormatter;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StaleBlobController.class)
public class StaleBlobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaleBlobFinder staleBlobFinder;

    @Test
    void should_return_list_of_stale_blobs_when_there_is_with_request_param() throws Exception {

        Instant createdAt = Instant.now();

        given(staleBlobFinder.findStaleBlobs(60))
            .willReturn(Arrays.asList(
                new BlobInfo("container1", "file_name_1", createdAt),
                new BlobInfo("container2", "file_name_2", createdAt))
            );
        mockMvc
            .perform(
                get("/stale-blobs")
                    .queryParam("stale_time", "60")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2))
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data.[0].container").value("container1"))
            .andExpect(jsonPath("$.data.[0].file_name").value("file_name_1"))
            .andExpect(jsonPath("$.data.[0].created_at").value(DateFormatter.getSimpleDateTime(createdAt)))
            .andExpect(jsonPath("$.data.[1].container").value("container2"))
            .andExpect(jsonPath("$.data.[1].file_name").value("file_name_2"))
            .andExpect(jsonPath("$.data.[1].created_at").value(DateFormatter.getSimpleDateTime(createdAt)));

        verify(staleBlobFinder).findStaleBlobs(60);

    }

    @Test
    void should_return_list_of_stale_blobs_when_there_is_by_default_param_value() throws Exception {

        Instant createdAt = Instant.now();

        given(staleBlobFinder.findStaleBlobs(120))
            .willReturn(Arrays.asList(new BlobInfo("container1", "file_name_1", createdAt)));
        mockMvc
            .perform(get("/stale-blobs"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data.[0].container").value("container1"))
            .andExpect(jsonPath("$.data.[0].file_name").value("file_name_1"))
            .andExpect(jsonPath("$.data.[0].created_at").value(DateFormatter.getSimpleDateTime(createdAt)));

        verify(staleBlobFinder).findStaleBlobs(120);

    }

    @Test
    void should_return_empty_data_when_there_is_no_stale_blob() throws Exception {

        given(staleBlobFinder.findStaleBlobs(120)).willReturn(Collections.emptyList());
        mockMvc
            .perform(get("/stale-blobs"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.data").isEmpty());

        verify(staleBlobFinder).findStaleBlobs(120);

    }

    @Test
    void should_return_400_for_invalid_time() throws Exception {
        mockMvc
            .perform(get("/stale-blobs").queryParam("stale_time", "1x"))
            .andDo(print())
            .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(staleBlobFinder);
    }

}
