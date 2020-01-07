package uk.gov.hmcts.reform.blobrouter.clients;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorNotificationRequest {

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("po_box")
    public final String poBox;

    @JsonProperty("error_code")
    public final String errorCode;

    @JsonProperty("error_description")
    public final String errorDescription;

    @JsonProperty("reference_id")
    public final String referenceId;

    /**
     * Full request body definition.
     *
     * @param zipFileName Name of the zip file
     * @param poBox POBox to which envelope was received
     * @param errorCode Error code
     * @param errorDescription Error message describing cause of notification
     * @param referenceId TBD
     */
    public ErrorNotificationRequest(
        String zipFileName,
        String poBox,
        String errorCode,
        String errorDescription,
        String referenceId
    ) {
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.referenceId = referenceId;
    }

    /**
     * Constructor with mandatory fields.
     *
     * @param zipFileName Name of the zip file
     * @param poBox POBox to which envelope was received
     * @param errorCode Error code
     * @param errorDescription Error message describing cause of notification
     */
    public ErrorNotificationRequest(
        String zipFileName,
        String poBox,
        String errorCode,
        String errorDescription
    ) {
        this(
            zipFileName,
            poBox,
            errorCode,
            errorDescription,
            null
        );
    }
}
