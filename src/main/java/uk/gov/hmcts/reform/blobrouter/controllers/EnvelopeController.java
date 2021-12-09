package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping(params = {"dcn_prefix", "between_dates"})
    public ResponseEntity findEnvelopesByDcnPrefixAndDate(
        @RequestParam(name = "dcn_prefix") String dcnPrefix,
        @RequestParam(name = "between_dates") @DateTimeFormat(iso = DATE) List<LocalDate> dates
    ) {

        if (dcnPrefix.length() < 10) {
            return ResponseEntity.badRequest().body("`dcn_prefix` should contain at least 10 characters");
        }
        if (dates.size() != 2) {
            return ResponseEntity.badRequest().body("`between_dates` should contain 2 valid dates");
        }
        LocalDate fromDate = dates.get(0);
        LocalDate toDate = dates.get(1);
        if (toDate.isBefore(fromDate)) {
            toDate = dates.get(0);
            fromDate = dates.get(1);
        }

        return ResponseEntity.ok(new SearchResult(
            envelopeService
                .getEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate))
        );

    }

    @GetMapping(path = "/stale-incomplete-envelopes")
    @Operation(
        summary = "Retrieves incomplete stale envelopes",
        description = "Returns an empty list when no incomplete stale envelopes were found"
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public SearchResult getIncomplete(
        @RequestParam(name = "stale_time", required = false, defaultValue = DEFAULT_STALE_TIME_HOURS)
            int staleTime
    ) {
        List<IncompleteEnvelopeInfo> envelopes = incompleteEnvelopesService.getIncompleteEnvelopes(2);

        return new SearchResult(envelopes);
    }

    private EnvelopeInfo toResponse(Envelope dbEnvelope, List<EnvelopeEvent> dbEventRecords) {
        return new EnvelopeInfo(
            dbEnvelope.id,
            dbEnvelope.container,
            dbEnvelope.fileName,
            dbEnvelope.createdAt,
            dbEnvelope.fileCreatedAt,
            dbEnvelope.dispatchedAt,
            dbEnvelope.status,
            dbEnvelope.isDeleted,
            dbEnvelope.pendingNotification,
            dbEventRecords
                .stream()
                .map(
                    e -> new EnvelopeEventResponse(
                        e.id, e.createdAt, e.type.name(), e.notes
                    )
                )
                .collect(toList())
        );
    }
}
