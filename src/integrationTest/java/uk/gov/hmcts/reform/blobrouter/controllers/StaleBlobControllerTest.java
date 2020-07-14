package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobLister;

import java.util.Arrays;

import static java.time.Instant.now;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.blobrouter.util.TimeUtils.toLocalTimeZone;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StaleBlobController.class)
public class StaleBlobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlobLister blobLister;

    @Test
    void should_return_list_of_stale_blobs_when_there_is_with_request_param() throws Exception {

        String createdAt = toLocalTimeZone(now());

        given(blobLister.listBlobs(1))
            .willReturn(Arrays.asList(
                new BlobInfo("container1", "file_name_1", createdAt),
                new BlobInfo("container2", "file_name_2", createdAt))
            );
        mockMvc
            .perform(
                get("/stale-blobs")
                    .queryParam("stale_time", "1")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[0].container").value("container1"))
            .andExpect(jsonPath("$.[0].file_name").value("file_name_1"))
            .andExpect(jsonPath("$.[0].created_at").value(createdAt))
            .andExpect(jsonPath("$.[1].container").value("container2"))
            .andExpect(jsonPath("$.[1].file_name").value("file_name_2"))
            .andExpect(jsonPath("$.[1].created_at").value(createdAt));

        verify(blobLister).listBlobs(1);

    }

    @Test
    void should_return_list_of_stale_blobs_when_there_is_by_default_param_value() throws Exception {

        String createdAt = toLocalTimeZone(now());

        given(blobLister.listBlobs(2))
            .willReturn(Arrays.asList(new BlobInfo("container1", "file_name_1", createdAt)));
        mockMvc
            .perform(get("/stale-blobs"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$.[0].container").value("container1"))
            .andExpect(jsonPath("$.[0].file_name").value("file_name_1"))
            .andExpect(jsonPath("$.[0].created_at").value(createdAt));

        verify(blobLister).listBlobs(2);

    }

    @Test
    void should_return_400_for_invalid_time() throws Exception {
        mockMvc
            .perform(get("/stale-blobs").queryParam("stale_time", "1x"))
            .andDo(print())
            .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(blobLister);
    }

}
