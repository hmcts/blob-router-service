package uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationMsg {

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("jurisdiction")
    public final String jurisdiction;

    @JsonProperty("po_box")
    public final String poBox;

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("error_code")
    public final String errorCode;

    @JsonProperty("error_description")
    public final String errorDescription;

    @JsonProperty("service")
    public final String service;

    public NotificationMsg(
        String zipFileName,
        String jurisdiction,
        String poBox,
        String documentControlNumber,
        String errorCode,
        String errorDescription,
        String service
    ) {
        this.zipFileName = zipFileName;
        this.jurisdiction = jurisdiction;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.service = service;
    }

}
