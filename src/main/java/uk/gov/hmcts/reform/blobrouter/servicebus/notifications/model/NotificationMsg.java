package uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;

public class NotificationMsg {

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("error_code")
    public final ErrorCode errorCode;

    @JsonProperty("error_description")
    public final String errorDescription;

    @JsonProperty("service")
    public final String service;

    public NotificationMsg(
        String zipFileName,
        String container,
        String documentControlNumber,
        ErrorCode errorCode,
        String errorDescription,
        String service
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.service = service;
    }

}
