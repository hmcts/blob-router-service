package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidRequestParametersException;
import uk.gov.hmcts.reform.blobrouter.model.out.IncompleteEnvelopeInfo;
import uk.gov.hmcts.reform.blobrouter.services.IncompleteEnvelopesService;
import uk.gov.hmcts.reform.blobrouter.util.DateFormatter;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.instant;

@AutoConfigureMockMvc
@SpringBootTest
public class EnvelopeControllerTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private IncompleteEnvelopesService incompleteEnvelopesService;

    @Test
    void should_find_envelope_by_file_name_and_container_if_it_exists() throws Exception {
        final String fileName = "some_file_name.zip";
        final String container = "some_container";

        Instant createdDate = Instant.parse("2020-05-20T10:15:10.000Z");
        Envelope envelopeInDb = envelope(fileName, container, Instant.from(createdDate));
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
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].id").value(envelopeInDb.id.toString()))
            .andExpect(jsonPath("$.data[0].created_at").value("2020-05-20T10:15:10.000Z"))
            .andExpect(jsonPath("$.data[0].file_created_at").value(
                DateFormatter.getSimpleDateTime(envelopeInDb.fileCreatedAt))
            )
            .andExpect(jsonPath("$.data[0].dispatched_at").value(
                DateFormatter.getSimpleDateTime(envelopeInDb.dispatchedAt))
            )
            .andExpect(jsonPath("$.data[0].pending_notification").value(envelopeInDb.pendingNotification))
            .andExpect(jsonPath("$.data[0].events[*].event").value(contains(
                EventType.FILE_PROCESSING_STARTED.name(),
                EventType.DISPATCHED.name()
            )))
            .andExpect(jsonPath("$.data[0].events[*].created_at").value(contains(
                DateFormatter.getSimpleDateTime(eventRecordInDb1.createdAt),
                DateFormatter.getSimpleDateTime(eventRecordInDb2.createdAt)
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
            .andExpect(jsonPath("$.count").value(1))
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
            .andExpect(jsonPath("$.count").value(2))
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
    public void should_return_400_for_missing_file_name_and_date() throws Exception {
        given(envelopeService.getEnvelopes(null, null, null))
            .willThrow(
                new InvalidRequestParametersException("'file_name' or 'date' must not be null or empty")
            );

        mockMvc.perform(
            get("/envelopes")
        )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_400_for_invalid_date() throws Exception {
        mockMvc.perform(
            get("/envelopes").queryParam("date", "2020011") // invalid date
        )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_incomplete_stale_envelopes() throws Exception {

        UUID envelopeId1 = UUID.randomUUID();
        UUID envelopeId2 = UUID.randomUUID();

        given(incompleteEnvelopesService.getIncompleteEnvelopes(2))
            .willReturn(asList(
                new IncompleteEnvelopeInfo("cmc", "file1.zip", envelopeId1, "2021-01-15T10:39:27"),
                new IncompleteEnvelopeInfo("sscs", "file2.zip", envelopeId2, "2021-01-14T11:38:28")
            ));

        mockMvc.perform(get("/envelopes/stale-incomplete-envelopes")
                            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("data[0].container").value("cmc"))
            .andExpect(jsonPath("data[0].file_name").value("file1.zip"))
            .andExpect(jsonPath("data[0].envelope_id").value(envelopeId1.toString()))
            .andExpect(jsonPath("data[0].created_at").value("2021-01-15T10:39:27"))
            .andExpect(jsonPath("data[1].container").value("sscs"))
            .andExpect(jsonPath("data[1].file_name").value("file2.zip"))
            .andExpect(jsonPath("data[1].envelope_id").value(envelopeId2.toString()))
            .andExpect(jsonPath("data[1].created_at").value("2021-01-14T11:38:28"));
    }

    @Test
    void should_return_empty_result_if_envelope_for_given_dncPrefix_and_dates_not_exist() throws Exception {
        final String dcnPrefix = "1234567890";

        var fromDate = LocalDate.of(2021, 8, 4);
        var toDate = LocalDate.of(2021, 8, 21);
        given(envelopeService.getEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate))
            .willReturn(emptyList());

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("dcn_prefix", dcnPrefix)
                    .queryParam("between_dates", "2021-08-04,2021-08-21")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void should_use_earliest_date_as_from_date_when_searching_by_dncPrefix() throws Exception {
        final String dcnPrefix = "123456789§";

        var fromDate = LocalDate.of(2021, 8, 4);
        var toDate = LocalDate.of(2021, 8, 21);
        given(envelopeService.getEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate))
            .willReturn(emptyList());

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("dcn_prefix", dcnPrefix)
                    .queryParam("between_dates", "2021-08-21,2021-08-04")
            )
            .andDo(print())
            .andExpect(status().isOk());
        verify(envelopeService).getEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate);
    }

    @Test
    void should_return_400_when_dncPrefix_shorter_than_10_chars() throws Exception {
        final String dcnPrefix = "123456789";

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("dcn_prefix", dcnPrefix)
                    .queryParam("between_dates", "2021-08-21,2021-08-04")
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_envelopes_if_envelope_for_given_dncPrefix_and_dates_exists() throws Exception {

        Envelope envelope1InDb = envelope("file_567890.zip", "cmc", instant("2021-08-26 10:10:00"));
        Envelope envelope2InDb = envelope("file_567890_2.zip", "sscs", instant("2021-08-27 11:54:00"));

        final String dcnPrefix = "file_567890";

        var fromDate = LocalDate.of(2021, 8, 25);
        var toDate = LocalDate.of(2021, 8, 31);
        given(envelopeService.getEnvelopesByDcnPrefix(anyString(), any(), any()))
            .willReturn(asList(envelope1InDb, envelope2InDb));

        mockMvc
            .perform(
                get("/envelopes")
                    .queryParam("dcn_prefix", dcnPrefix)
                    .queryParam("between_dates", "2021-08-25,2021-08-31")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("data[0].container").value("cmc"))
            .andExpect(jsonPath("data[0].fileName").value("file_567890.zip"))
            .andExpect(jsonPath("data[0].id").value(envelope1InDb.id.toString()))
            .andExpect(jsonPath("data[0].createdAt").value("2021-08-26T09:10:00Z"))
            .andExpect(jsonPath("data[1].container").value("sscs"))
            .andExpect(jsonPath("data[1].fileName").value("file_567890_2.zip"))
            .andExpect(jsonPath("data[1].id").value(envelope2InDb.id.toString()))
            .andExpect(jsonPath("data[1].createdAt").value("2021-08-27T10:54:00Z"));
        verify(envelopeService).getEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate);
    }

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
            false,
            null
        );
    }

    private EnvelopeEvent envelopeEvent(UUID envelopeId, int eventId, EventType eventType) {
        return new EnvelopeEvent(eventId, envelopeId, eventType, null, null, now());
    }
}
