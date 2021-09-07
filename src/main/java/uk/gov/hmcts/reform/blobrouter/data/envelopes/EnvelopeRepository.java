package uk.gov.hmcts.reform.blobrouter.data.envelopes;

import feign.Param;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

@Repository
public class EnvelopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeMapper mapper;

    public EnvelopeRepository(NamedParameterJdbcTemplate jdbcTemplate, EnvelopeMapper mapper) {
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

    public List<Envelope> find(Status status, String container, boolean isDeleted) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes WHERE status = :status AND container = :container AND is_deleted = :isDeleted",
            new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("container", container)
                .addValue("isDeleted", isDeleted),
            this.mapper
        );
    }

    public List<Envelope> find(Status status, boolean isDeleted) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes WHERE status = :status AND is_deleted = :isDeleted",
            new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("isDeleted", isDeleted),
            this.mapper
        );
    }

    public List<Envelope> find(String fileName, String container) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes WHERE file_name = :fileName AND container = :container",
            new MapSqlParameterSource()
                .addValue("fileName", fileName)
                .addValue("container", container),
            this.mapper
        );
    }

    public Optional<Envelope> findEnvelopeNotInCreatedStatus(String fileName, String container) {
        try {
            Envelope envelope = jdbcTemplate.queryForObject(
                "SELECT * FROM envelopes "
                    + "WHERE file_name = :fileName "
                    + "AND container = :container "
                    + "AND status != 'CREATED' "
                    + "ORDER BY created_at DESC "
                    + "LIMIT 1",
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

    public Optional<Envelope> findLast(String fileName, String container) {
        try {
            Envelope envelope = jdbcTemplate.queryForObject(
                "SELECT * FROM envelopes"
                    + " WHERE file_name = :fileName"
                    + " AND container = :container"
                    + " ORDER BY created_at DESC"
                    + " LIMIT 1",
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
                .addValue(
                    "dispatchedAt",
                    envelope.dispatchedAt == null ? null : Timestamp.from(envelope.dispatchedAt)
                )
        );
        return id;
    }

    public void updateStatus(UUID id, Status newStatus) {
        jdbcTemplate.update(
            "UPDATE envelopes "
                + "SET status = :newStatus "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("newStatus", newStatus.name())
        );
    }

    public void updateDispatchDateTime(UUID id, Instant dispatchedAt) {
        jdbcTemplate.update(
            "UPDATE envelopes "
                + "SET dispatched_at = :dispatchedAt "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("dispatchedAt", Timestamp.from(dispatchedAt))
        );
    }

    public int markAsDeleted(UUID id) {
        return jdbcTemplate.update(
            "UPDATE envelopes "
                + "SET is_deleted = True "
                + "WHERE id = :id",
            new MapSqlParameterSource("id", id)
        );
    }

    public int updatePendingNotification(UUID id, Boolean notificationPending) {
        return jdbcTemplate.update(
            "UPDATE envelopes "
                + "SET pending_notification = :notificationPending "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("notificationPending", notificationPending)
        );
    }

    public Integer getEnvelopesCount(Set<String> containers, Instant fromDateTime, Instant toDateTime) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM envelopes "
                + " WHERE container in (:containers)"
                + " AND file_created_at BETWEEN :fromDateTime AND :toDateTime",
            new MapSqlParameterSource()
                .addValue("containers", containers)
                .addValue("fromDateTime", Timestamp.from(fromDateTime))
                .addValue("toDateTime", Timestamp.from(toDateTime)),
            Integer.class
        );
    }

    public List<Envelope> findEnvelopes(String fileName, String container, LocalDate date) {
        StringJoiner whereClause = new StringJoiner(" AND ", " WHERE ", "");
        whereClause.setEmptyValue(""); // default value when all query params are null/empty

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        if (StringUtils.isNotEmpty(fileName)) {
            whereClause.add("file_name = :fileName");
            parameterSource.addValue("fileName", fileName);
        }

        if (StringUtils.isNotEmpty(container)) {
            whereClause.add("container = :container");
            parameterSource.addValue("container", container);
        }

        if (date != null) {
            whereClause.add("DATE(created_at) = :date");
            parameterSource.addValue("date", date);
        }

        return jdbcTemplate.query(
            "SELECT * FROM envelopes"
                + whereClause.toString()
                + " ORDER BY created_at DESC",
            parameterSource,
            this.mapper
        );
    }

    public List<Envelope> getIncompleteEnvelopesBefore(@Param("datetime") Instant dateTime) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes"
                + " WHERE file_created_at < :datetime AND status = 'CREATED'"
                + " ORDER BY created_at DESC",
            new MapSqlParameterSource()
                .addValue("datetime", Timestamp.from(dateTime)),
            mapper
        );
    }

    public List<Envelope> findEnvelopesByDcnPrefix(String dcnPrefix, LocalDate fromDate, LocalDate toDate) {

        return jdbcTemplate.query(
            "SELECT * FROM envelopes"
                + " WHERE file_name like :dcnPrefix"
                + " AND DATE(file_created_at) BETWEEN :fromDate AND :toDate"
                + " ORDER BY file_created_at DESC",
            new MapSqlParameterSource()
                .addValue("dcnPrefix", dcnPrefix + "%")
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate),
            this.mapper
        );
    }
}
