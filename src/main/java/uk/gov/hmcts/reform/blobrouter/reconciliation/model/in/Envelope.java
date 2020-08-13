package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Envelope {

    public final String zipFileName;
    public final String rescanFor;
    public final String container;
    public final String jurisdiction;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;

    public Envelope(
        @JsonProperty(value = "zip_file_name", required = true) String zipFileName,
        @JsonProperty(value = "rescan_for") String rescanFor,
        @JsonProperty(value = "container", required = true) String container,
        @JsonProperty(value = "jurisdiction", required = true) String jurisdiction,
        @JsonProperty(value = "scannable_item_dcns", required = true) List<String> scannableItemDcns,
        @JsonProperty(value = "payment_dcns", required = true) List<String> paymentDcns
    ) {
        this.zipFileName = zipFileName;
        this.rescanFor = rescanFor;
        this.container = container;
        this.jurisdiction = jurisdiction;
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
    }
}
