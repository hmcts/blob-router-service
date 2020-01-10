package uk.gov.hmcts.reform.blobrouter.data;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.util.List;
import java.util.UUID;

@Repository
public class EnvelopeRepositoryImpl implements EnvelopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EnvelopeRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Envelope> find(Status status, boolean isDeleted) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes WHERE status = :status AND is_deleted = :isDeleted",
            new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("isDeleted", isDeleted),
            new EnvelopeMapper()
        );
    }

    @Override
    public void insert(Envelope envelope) {
        jdbcTemplate.update(
            "INSERT INTO envelopes (id, container, file_name, file_created_at, dispatched_at, status, is_deleted) "
            + "VALUES (:id, :container, :fileName, :fileCreatedAt, :dispatchedAt, :status, :isDeleted",
            new MapSqlParameterSource()
            .addValue("id", envelope.id)
            .addValue("container", envelope.container)
            .addValue("fileName", envelope.fileName)
            .addValue("fileCreatedAt", envelope.fileCreatedAt)
        )
    }

    @Override
    public int markAsDeleted(UUID envelopeId) {
        throw new NotImplementedException("Not yet implemented");
    }
}
