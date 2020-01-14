package uk.gov.hmcts.reform.blobrouter.data;

import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvelopeRepository {

    List<Envelope> find(Status status, boolean isDeleted);

    Optional<Envelope> find(String fileName, String container);

    /**
     * Creates new envelope in DB. 
     * @return ID of newly created envelope.
     */
    UUID insert(NewEnvelope envelope);

    int markAsDeleted(UUID envelopeId);
}
