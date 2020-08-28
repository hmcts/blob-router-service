package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ReportedZipFile {

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("rescan_for")
    public final String rescanFor;

    @JsonProperty("scannable_item_dcns")
    public final List<String> scannableItemDcns;

    @JsonProperty("payment_dcns")
    public final List<String> paymentDcns;

    public ReportedZipFile(
        String zipFileName,
        String container,
        String rescanFor,
        List<String> scannableItemDcns,
        List<String> paymentDcns
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.rescanFor = rescanFor;
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
    }
}
