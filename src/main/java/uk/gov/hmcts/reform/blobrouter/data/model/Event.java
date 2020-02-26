package uk.gov.hmcts.reform.blobrouter.data.model;

public enum Event {
    FILE_PROCESSING_STARTED,
    DISPATCHED,
    REJECTED,
    DELETED,
    DELETED_FROM_REJECTED,
    DUPLICATE_REJECTED,
    ERROR,
}
