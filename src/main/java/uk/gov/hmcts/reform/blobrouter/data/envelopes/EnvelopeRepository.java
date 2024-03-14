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
import java.time.LocalDateTime;
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
            "INSERT INTO envelopes (id, container, file_name, file_created_at, status, dispatched_at, "
                + "created_at, file_size) "
                + "VALUES (:id, :container, :fileName, :fileCreatedAt, :status, :dispatchedAt, CURRENT_TIMESTAMP, "
                + ":fileSize)",
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
                .addValue("fileSize", envelope.fileSize)
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

    /**
     * This Java function retrieves envelopes based on specified criteria such as file name, container, and date.
     *
     * @param fileName The `fileName` parameter is used to filter the envelopes based on the file name. If a
     *      `fileName` is provided, the SQL query will include a condition to match the `file_name` column with
     *      the provided `fileName` value.
     * @param container The `container` parameter in the `findEnvelopes` method is used to filter the envelopes based on
     *      the container they belong to. If a `container` value is provided, the SQL query will include a condition
     *      to filter envelopes where the `container` column matches the provided value.
     * @param date The `date` parameter in the `findEnvelopes` method is used to filter envelopes based on the creation
     *      date. If a date is provided, the method will include a condition in the SQL query to only retrieve
     *      envelopes that were created on that specific date.
     * @return A List of Envelope objects that match the specified criteria of file name, container, and creation date.
     *      The query is executed on the "envelopes" table, filtering the results based on the provided parameters
     *      and ordering the results by the creation date in descending order.
     */
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

    /**
     * This Java function retrieves a list of incomplete envelopes created before a specified datetime.
     *
     * @param dateTime The `dateTime` parameter is a LocalDateTime object that represents a specific date and time.
     *      In the provided method `getIncompleteEnvelopesBefore`, this parameter is used to retrieve a list of
     *      incomplete envelopes that were created before the specified date and time.
     * @return A list of incomplete envelopes that were created before the specified datetime and have a status of
     *      'CREATED', ordered by their creation date in descending order.
     */
    public List<Envelope> getIncompleteEnvelopesBefore(@Param("datetime") LocalDateTime dateTime) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes"
                + " WHERE file_created_at < :datetime AND status = 'CREATED'"
                + " ORDER BY created_at DESC",
            new MapSqlParameterSource()
                .addValue("datetime", dateTime),
            mapper
        );
    }

    /**
     * The function `findEnvelopesByDcnPrefix` retrieves a list of envelopes based on a given
     * prefix, creation date range, and orders them by creation date in descending order.
     *
     * @param dcnPrefix The `dcnPrefix` parameter is used to search for envelopes based on a
     *      prefix of the file name. The query will look for file names that start with the specified `dcnPrefix`.
     * @param fromDate The `fromDate` parameter represents the starting date for the search criteria in the
     *      `findEnvelopesByDcnPrefix` method. It is used to filter envelopes based on their creation date,
     *      ensuring that only envelopes created on or after this date are included in the result set.
     * @param toDate The `toDate` parameter in the `findEnvelopesByDcnPrefix` method is used to specify the end date for
     *      filtering envelopes based on their creation date. Envelopes with a creation date up to and including
     *      this `toDate` will be included in the result set.
     * @return A List of Envelope objects that match the criteria specified in the SQL query. The envelopes are filtered
     *      based on the `dcnPrefix`, `fromDate`, and `toDate` parameters, and are ordered by `file_created_at`
     *      in descending order.
     */
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

    /**
     * The function deletes envelopes with file creation date before a specified datetime and with status not equal to
     * 'DISPATCHED' based on a list of envelope IDs.
     *
     * @param dateTime The `dateTime` parameter represents a specific point in time as a `LocalDateTime` object. In the
     *      `deleteEnvelopesBefore` method, it is used to specify a cutoff time before which envelopes
     *      should be deleted.
     * @param envelopeIds The `envelopeIds` parameter is a list of UUIDs representing the unique
     *      identifiers of envelopes that you want to delete. In the `deleteEnvelopesBefore` method, these envelope
     *      IDs are used to specify which envelopes should be deleted from the database based on the given criteria.
     * @return The method `deleteEnvelopesBefore` returns an integer value representing the number of rows
     *      affected by the deletion operation in the database table `envelopes`.
     */
    public int deleteEnvelopesBefore(LocalDateTime dateTime, List<UUID> envelopeIds) {
        return jdbcTemplate.update(
            "DELETE FROM envelopes e WHERE e.file_created_at < :dateTime AND e.status != 'DISPATCHED' "
                + "AND e.id IN (:envelopeIds)",
            new MapSqlParameterSource()
                .addValue("dateTime", dateTime)
                .addValue("envelopeIds", envelopeIds)
        );
    }
}
