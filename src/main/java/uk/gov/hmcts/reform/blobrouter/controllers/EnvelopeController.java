package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeEventResponse;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeInfo;
import uk.gov.hmcts.reform.blobrouter.model.out.IncompleteEnvelopeInfo;
import uk.gov.hmcts.reform.blobrouter.model.out.SearchResult;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.IncompleteEnvelopesService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Validated
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

    @DeleteMapping(path = "/stale/{envelopeId}")
    @Operation(
        summary = "Remove one stale envelope",
        description = "If an envelope is older than a certain period of time then it will be removed."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = SearchResult.class))
    )
    @ApiResponse(responseCode = "404", description = "envelope not found")
    public SearchResult deleteOneStaleEnvelope(
        @RequestParam(name = "stale_time", required = false, defaultValue = "168")
        @Min(value = 168, message = "stale_time must be at least 168 hours (a week)")
        int staleTime,
        @PathVariable UUID envelopeId
    ) {
        if (incompleteEnvelopesService.deleteIncompleteEnvelopes(staleTime, List.of(envelopeId.toString())) == 0) {
            throw new EnvelopeNotFoundException("Envelope not removed, as it is not found/not stale");
        }
        return new SearchResult(List.of(envelopeId));
    }

    @DeleteMapping(path = "/stale/all")
    @Operation(
        summary = "Remove all stale envelopes",
        description = "If an envelope is older than a certain period of time then it will be removed."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = SearchResult.class))
    )
    public SearchResult deleteAllStaleEnvelopes(
        @RequestParam(name = "stale_time", required = false, defaultValue = "168")
        @Min(value = 48, message = "stale_time must be at least 48 hours")
        int staleTime
    ) {
        List<IncompleteEnvelopeInfo> incompleteEnvelopesInfo =
            incompleteEnvelopesService.getIncompleteEnvelopes(staleTime);
        List<String> envelopeIds = incompleteEnvelopesInfo.stream().map(s -> s.envelopeId.toString()).toList();
        incompleteEnvelopesService.deleteIncompleteEnvelopes(staleTime, envelopeIds);

        return new SearchResult(envelopeIds);
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
            dbEnvelope.fileSize,
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
