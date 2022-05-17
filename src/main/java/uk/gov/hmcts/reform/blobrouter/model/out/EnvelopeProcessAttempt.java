package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class EnvelopeProcessAttempt {

    @JsonProperty("attemptId")
    public final UUID attemptId;
    @JsonProperty("envelopeId")
    public final String envelopeId;
    @JsonProperty("teamId")
    public final String teamId;
    @JsonProperty("timestamp")
    public final String timestamp;
    @JsonProperty("description")
    public final String description;
    @JsonProperty("warnings")
    public final List<String> warnings;
    @JsonProperty("errors")
    public final List<String> errors;
    @JsonProperty("status")
    public final String status;

    public EnvelopeProcessAttempt(UUID attemptId,
                                  String envelopeId,
                                  String teamId,
                                  String timestamp,
                                  String description,
                                  List<String> warnings,
                                  List<String> errors,
                                  String status) {
        this.attemptId = attemptId;
        this.envelopeId = envelopeId;
        this.teamId = teamId;
        this.timestamp = timestamp;
        this.description = description;
        this.warnings = warnings;
        this.errors = errors;
        this.status = status;
    }

}
