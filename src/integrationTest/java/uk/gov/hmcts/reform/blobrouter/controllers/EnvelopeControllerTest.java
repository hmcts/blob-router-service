package uk.gov.hmcts.reform.blobrouter.controllers;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.instant;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.toLocalTimeZone;

@AutoConfigureMockMvc
@SpringBootTest
public class EnvelopeControllerTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_find_envelope_by_file_name_and_container_if_it_exists() throws Exception {
        final String fileName = "some_file_name.zip";
        final String container = "some_container";

        Envelope envelopeInDb = envelope(fileName, container);
        var eventRecordInDb1 = envelopeEvent(envelopeInDb.id, 1, EventType.FILE_PROCESSING_STARTED);
        var eventRecordInDb2 = envelopeEvent(envelopeInDb.id, 2, EventType.DISPATCHED);

        given(envelopeService.getEnvelopes(fileName, container, null))
            .willReturn(singletonList(Tuples.of(
                envelopeInDb,
                asList(
                    eventRecordInDb1,
                    eventRecordInDb2
                )
            )));

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("file_name", fileName)
                    .queryParam("container", container)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].id").value(envelopeInDb.id.toString()))
            .andExpect(jsonPath("$.data[0].created_at").value(toLocalTimeZone(envelopeInDb.createdAt).toString()))
            .andExpect(jsonPath("$.data[0].file_created_at").value(
                toLocalTimeZone(envelopeInDb.fileCreatedAt).toString())
            )
            .andExpect(jsonPath("$.data[0].dispatched_at").value(
                toLocalTimeZone(envelopeInDb.dispatchedAt).toString())
            )
            .andExpect(jsonPath("$.data[0].pending_notification").value(envelopeInDb.pendingNotification))
            .andExpect(jsonPath("$.data[0].events[*].event").value(contains(
                EventType.FILE_PROCESSING_STARTED.name(),
                EventType.DISPATCHED.name()
            )))
            .andExpect(jsonPath("$.data[0].events[*].created_at").value(contains(
                toLocalTimeZone(eventRecordInDb1.createdAt).toString(),
                toLocalTimeZone(eventRecordInDb2.createdAt).toString()
            )));
    }

    @Test
    void should_return_empty_result_if_envelope_for_given_file_name_and_container_does_not_exist() throws Exception {
        final String fileName = "hello.zip";
        final String container = "foo";

        given(envelopeService.getEnvelopes(fileName, container, null))
            .willReturn(emptyList());

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("file_name", fileName)
                    .queryParam("container", container)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void should_return_envelopes_by_file_name_and_container_and_created_date() throws Exception {
        String container = "container1";

        Envelope envelope1InDb = envelope("file1.zip", container, instant("2020-05-10 12:10:00"));
        envelopeEvent(envelope1InDb.id, 1, EventType.FILE_PROCESSING_STARTED);
        envelopeEvent(envelope1InDb.id, 2, EventType.DISPATCHED);

        Envelope envelope2InDb = envelope("file2.zip", container, instant("2020-05-11 08:10:00"));
        var envelope2Event1InDb = envelopeEvent(envelope2InDb.id, 3, EventType.FILE_PROCESSING_STARTED);
        var envelope2Event2InDb = envelopeEvent(envelope2InDb.id, 4, EventType.REJECTED);

        given(envelopeService.getEnvelopes("file2.zip", container, LocalDate.of(2020, 5, 11)))
            .willReturn(singletonList(Tuples.of(
                envelope2InDb,
                asList(
                    envelope2Event1InDb,
                    envelope2Event2InDb
                )
            )));

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("file_name", "file2.zip")
                    .queryParam("container", container)
                    .queryParam("date", "2020-05-11")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].id").value(envelope2InDb.id.toString()))
            .andExpect(jsonPath("$.data[0].container").value(envelope2InDb.container))
            .andExpect(jsonPath("$.data[0].events[*].event").value(contains(
                EventType.FILE_PROCESSING_STARTED.name(),
                EventType.REJECTED.name()
            )));
    }

    @Test
    void should_return_envelopes_for_the_requested_date() throws Exception {
        Envelope envelope1InDb = envelope("file1.zip", "container1", instant("2020-05-10 12:10:00"));
        envelopeEvent(envelope1InDb.id, 1, EventType.FILE_PROCESSING_STARTED);
        envelopeEvent(envelope1InDb.id, 2, EventType.DISPATCHED);

        Envelope envelope2InDb = envelope("file2.zip", "container2", instant("2020-05-11 08:10:00"));
        var envelope2Event1InDb = envelopeEvent(envelope2InDb.id, 3, EventType.FILE_PROCESSING_STARTED);
        var envelope2Event2InDb = envelopeEvent(envelope2InDb.id, 4, EventType.REJECTED);

        Envelope envelope3InDb = envelope("file3.zip", "container3", instant("2020-05-11 10:10:00"));

        given(envelopeService.getEnvelopes(null, null, LocalDate.of(2020, 5, 11)))
            .willReturn(asList(
                Tuples.of(
                    envelope2InDb,
                    asList(
                        envelope2Event1InDb,
                        envelope2Event2InDb
                    )
                ),
                Tuples.of(envelope3InDb, emptyList())
            ));

        mockMvc.perform(
            get("/envelopes").queryParam("date", "2020-05-11")
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data[0].id").value(envelope2InDb.id.toString()))
            .andExpect(jsonPath("$.data[0].container").value(envelope2InDb.container))
            .andExpect(jsonPath("$.data[0].events[*].event").value(contains(
                EventType.FILE_PROCESSING_STARTED.name(),
                EventType.REJECTED.name()
            )))
            .andExpect(jsonPath("$.data[1].id").value(envelope3InDb.id.toString()))
            .andExpect(jsonPath("$.data[1].container").value(envelope3InDb.container))
            .andExpect(jsonPath("$.data[1].events", empty()));
    }

    @Test
    void should_return_all_envelopes_when_no_request_params_provided() throws Exception {
        Envelope envelope1InDb = envelope("file1.zip", "container1", instant("2020-05-10 12:10:00"));
        var envelope1Event1InDb = envelopeEvent(envelope1InDb.id, 1, EventType.FILE_PROCESSING_STARTED);
        var envelope1Event2InDb = envelopeEvent(envelope1InDb.id, 2, EventType.DISPATCHED);

        Envelope envelope2InDb = envelope("file2.zip", "container2", instant("2020-05-11 10:10:00"));

        given(envelopeService.getEnvelopes(null, null, null))
            .willReturn(asList(
                Tuples.of(
                    envelope1InDb,
                    asList(
                        envelope1Event1InDb,
                        envelope1Event2InDb
                    )
                ),
                Tuples.of(envelope2InDb, emptyList())
            ));

        mockMvc.perform(get("/envelopes")) // no query params
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data[0].id").value(envelope1InDb.id.toString()))
            .andExpect(jsonPath("$.data[0].container").value(envelope1InDb.container))
            .andExpect(jsonPath("$.data[0].events[*].event").value(contains(
                EventType.FILE_PROCESSING_STARTED.name(),
                EventType.DISPATCHED.name()
            )))
            .andExpect(jsonPath("$.data[1].id").value(envelope2InDb.id.toString()))
            .andExpect(jsonPath("$.data[1].container").value(envelope2InDb.container))
            .andExpect(jsonPath("$.data[1].events", empty()));
    }

    @Test
    public void should_return_400_for_invalid_date() throws Exception {
        mockMvc.perform(
            get("/envelopes").queryParam("date", "2020011") // invalid date
        )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @NotNull
    private Envelope envelope(String fileName, String container, Instant createdDate) {
        return new Envelope(
            UUID.randomUUID(),
            container,
            fileName,
            createdDate,
            now(),
            now(),
            Status.DISPATCHED,
            false,
            false
        );
    }

    @NotNull
    private Envelope envelope(String fileName, String container) {
        return envelope(fileName, container, now());
    }

    @NotNull
    private EnvelopeEvent envelopeEvent(UUID envelopeId, int eventId, EventType eventType) {
        return new EnvelopeEvent(eventId, envelopeId, eventType, null, null, now());
    }
}
