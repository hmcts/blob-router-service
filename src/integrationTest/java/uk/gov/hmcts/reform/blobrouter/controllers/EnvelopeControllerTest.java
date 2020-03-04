package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.EventRecord;
import uk.gov.hmcts.reform.blobrouter.data.model.EventType;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;
import static org.hamcrest.Matchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
public class EnvelopeControllerTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_find_envelope_by_file_name_and_container_if_it_exists() throws Exception {
        final String fileName = "some_file_name.zip";
        final String container = "some_container";

        Envelope envelopeInDb = new Envelope(
            UUID.randomUUID(),
            container,
            fileName,
            now(),
            now(),
            now(),
            Status.DISPATCHED,
            false
        );
        EventRecord eventRecordInDb1 = new EventRecord(
            1,
            container,
            fileName,
            now(),
            EventType.FILE_PROCESSING_STARTED,
            null
        );
        EventRecord eventRecordInDb2 = new EventRecord(
            2,
            container,
            fileName,
            now(),
            EventType.DISPATCHED,
            null
        );

        given(envelopeRepo.find(fileName, container))
            .willReturn(Optional.of(envelopeInDb));
        given(eventRecordRepo.find(container, fileName))
            .willReturn(Arrays.asList(
                eventRecordInDb1,
                eventRecordInDb2
            ));

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("file_name", fileName)
                    .queryParam("container", container)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(envelopeInDb.id.toString()))
            .andExpect(jsonPath("$.events[*].event").value(contains(
                EventType.FILE_PROCESSING_STARTED.name(),
                EventType.DISPATCHED.name()
            )));
    }

    @Test
    void should_return_404_if_envelope_for_given_file_name_and_container_does_not_exist() throws Exception {
        final String fileName = "hello.zip";
        final String container = "foo";

        given(envelopeRepo.find(fileName, container))
            .willReturn(Optional.empty());

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("file_name", fileName)
                    .queryParam("container", container)
            )
            .andDo(print())
            .andExpect(status().isNotFound());

        verifyNoInteractions(eventRecordRepo);
    }
}
