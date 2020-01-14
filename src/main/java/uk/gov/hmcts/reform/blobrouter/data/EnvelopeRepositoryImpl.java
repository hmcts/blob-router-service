package uk.gov.hmcts.reform.blobrouter.data;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EnvelopeRepositoryImpl implements EnvelopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeMapper mapper;

    public EnvelopeRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate, EnvelopeMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public Optional<Envelope> find(UUID id) {
        try {
            Envelope envelope = jdbcTemplate.queryForObject(
                "SELECT * FROM envelopes WHERE id = :id",
                new MapSqlParameterSource("id", id),
                this.mapper
            );
            return Optional.of(envelope);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<Envelope> find(String fileName, String container) {
        try {
            Envelope envelope = jdbcTemplate.queryForObject(
                "SELECT * FROM envelopes"
                    + " WHERE file_name = :fileName"
                    + " AND container = :container",
                new MapSqlParameterSource()
                    .addValue("fileName", fileName)
                    .addValue("container", container),
                this.mapper
            );
            return Optional.of(envelope);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Envelope> find(Status status, boolean isDeleted) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes WHERE status = :status AND is_deleted = :isDeleted",
            new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("isDeleted", isDeleted),
            this.mapper
        );
    }

    @Override
    public UUID insert(NewEnvelope envelope) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO envelopes (id, container, file_name, file_created_at, status, dispatched_at, created_at) "
                + "VALUES (:id, :container, :fileName, :fileCreatedAt, :status, :dispatchedAt, CURRENT_TIMESTAMP)",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("container", envelope.container)
                .addValue("fileName", envelope.fileName)
                .addValue("fileCreatedAt", Timestamp.from(envelope.fileCreatedAt))
                .addValue("status", envelope.status.name())
                .addValue("dispatchedAt", Timestamp.from(envelope.dispatchedAt))
        );
        return id;
    }

    @Override
    public int markAsDeleted(UUID id) {
        return jdbcTemplate.update(
            "UPDATE envelopes "
                + "SET is_deleted = True "
                + "WHERE id = :id",
            new MapSqlParameterSource("id", id)
        );
    }
}
