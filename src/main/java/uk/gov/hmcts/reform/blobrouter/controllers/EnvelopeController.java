package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeEventResponse;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeInfo;
import uk.gov.hmcts.reform.blobrouter.model.out.IncompleteEnvelopeInfo;
import uk.gov.hmcts.reform.blobrouter.model.out.SearchResult;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.IncompleteEnvelopesService;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static uk.gov.hmcts.reform.blobrouter.util.TimeUtils.toLocalTimeZone;

@RestController
@RequestMapping(path = "/envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeService envelopeService;
    private final IncompleteEnvelopesService incompleteEnvelopesService;

    private static final String DEFAULT_STALE_TIME_HOURS = "2";

    public EnvelopeController(
        EnvelopeService envelopeService,
        IncompleteEnvelopesService incompleteEnvelopesService
    ) {
        this.envelopeService = envelopeService;
        this.incompleteEnvelopesService = incompleteEnvelopesService;
    }

    @GetMapping()
    public SearchResult findEnvelopes(
        @RequestParam(name = "file_name", required = false) String fileName,
        @RequestParam(name = "container", required = false) String container,
        @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        return new SearchResult(
            envelopeService
                .getEnvelopes(fileName, container, date)
                .stream()
                .map(tuple -> toResponse(tuple.getT1(), tuple.getT2()))
                .collect(toList())
        );

    }

    @GetMapping(path = "/stale-incomplete-blobs")
    @ApiOperation(
        value = "Retrieves incomplete stale envelopes",
        notes = "Returns an empty list when no incomplete stale envelopes were found"
    )
    @ApiResponse(code = 200, message = "Success")
    public SearchResult getIncomplete(
        @RequestParam(name = "stale_time", required = false, defaultValue = DEFAULT_STALE_TIME_HOURS)
            int staleTime
    ) {
        List<IncompleteEnvelopeInfo> envelopes = incompleteEnvelopesService.getIncompleteEnvelopes(2);

        return new SearchResult(envelopes);
    }

    private EnvelopeInfo toResponse(Envelope dbEnvelope, List<EnvelopeEvent> dbEventRecords) {
        return new EnvelopeInfo(
            dbEnvelope.getId(),
            dbEnvelope.getContainer(),
            dbEnvelope.getFileName(),
            toLocalTimeZone(dbEnvelope.getCreatedAt()),
            toLocalTimeZone(dbEnvelope.getFileCreatedAt()),
            toLocalTimeZone(dbEnvelope.getDispatchedAt()),
            dbEnvelope.getStatus(),
            dbEnvelope.getIsDeleted(),
            dbEnvelope.getPendingNotification(),
            dbEventRecords
                .stream()
                .map(
                    e -> new EnvelopeEventResponse(
                        e.id, toLocalTimeZone(e.createdAt), e.type.name(), e.notes
                    )
                )
                .collect(toList())
        );
    }
}
