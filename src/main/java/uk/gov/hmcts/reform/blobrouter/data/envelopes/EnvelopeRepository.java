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

/**
 * The `EnvelopeRepository` class in Java provides methods to interact with a database table storing envelope data,
 * including finding, inserting, updating, and deleting envelope records based on various criteria.
 */
@Repository
public class EnvelopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeMapper mapper;

    public EnvelopeRepository(NamedParameterJdbcTemplate jdbcTemplate, EnvelopeMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    /**
     * The function finds an Envelope object by its UUID using a jdbcTemplate query and returns it wrapped
     * in an Optional, handling EmptyResultDataAccessException by returning an empty Optional.
     *
     * @param id The `id` parameter is a `UUID` representing the unique identifier of the envelope
     *      that you want to find in the database.
     * @return An Optional object containing either the Envelope object with the specified UUID if
     *      found in the database, or an empty Optional if no Envelope with that UUID is found.
     */
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

    /**
     * This Java function retrieves a list of Envelope objects based on specified status, container, and deletion status
     * criteria from a database using JDBC template.
     *
     * @param status Status enum representing the status of the envelope (e.g., PENDING, DELIVERED, RETURNED)
     * @param container The `container` parameter in the `find` method is used to specify the container
     *      value to search for in the database query. It is a String type parameter that represents the
     *      container value to match in the database query.
     * @param isDeleted The `isDeleted` parameter is a boolean value that indicates whether
     *      the envelope has been marked as deleted or not. It is used in the SQL query to filter envelopes
     *      based on their deletion status.
     * @return A List of Envelope objects is being returned from the method.
     */
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

    /**
     * The function `find` retrieves a list of `Envelope` objects from the database based on the provided `status` and
     * `isDeleted` parameters.
     *
     * @param status Status is an enum representing the status of an envelope. It could be one of the following values:
     *      PENDING, SENT, DELIVERED, or FAILED.
     * @param isDeleted The `isDeleted` parameter in the `find` method is a boolean flag that indicates whether the
     *      envelopes being searched for have been marked as deleted or not.
     *      It is used in the SQL query to filter out envelopes based on their deletion status.
     * @return A list of Envelope objects that match the specified status and isDeleted criteria from the database.
     */
    public List<Envelope> find(Status status, boolean isDeleted) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes WHERE status = :status AND is_deleted = :isDeleted",
            new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("isDeleted", isDeleted),
            this.mapper
        );
    }

    /**
     * The `find` function retrieves a list of Envelope objects from a database based on the provided file name and
     * container values.
     *
     * @param fileName The `fileName` parameter is used to specify the name of the file that you want to search
     *      for in the database. It is a string type parameter that helps in filtering the search results
     *      based on the file name.
     * @param container The `container` parameter in the `find` method is used to specify the container
     *      value that will be used in the SQL query to filter the results. It is a criteria based on which the
     *      envelopes will be retrieved from the database.
     * @return A List of Envelope objects is being returned.
     */
    public List<Envelope> find(String fileName, String container) {
        return jdbcTemplate.query(
            "SELECT * FROM envelopes WHERE file_name = :fileName AND container = :container",
            new MapSqlParameterSource()
                .addValue("fileName", fileName)
                .addValue("container", container),
            this.mapper
        );
    }

    /**
     * This Java function finds an envelope in a database table that matches the given file name and container, has a
     * status other than 'CREATED', and returns it wrapped in an Optional.
     *
     * @param fileName The `fileName` parameter is a String representing the name of the file for which
     *      you want to find an envelope that is not in the 'CREATED' status.
     * @param container Container is a parameter that specifies the location or storage unit
     *      where the envelope is stored. It helps identify the specific location of the envelope
     *      within a system or database.
     * @return An Optional object containing an Envelope that is not in the 'CREATED' status, based on the provided file
     *      name and container. If no such Envelope is found, an empty Optional is returned.
     */
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

    /**
     * This Java function finds the last envelope with a specific file name and container in a database table.
     *
     * @param fileName The `fileName` parameter is used to specify the name of the file that you want to
     *      search for in the database when retrieving the last envelope.
     * @param container The `container` parameter in the `findLast` method is used to specify the container
     *      value that will be used in the SQL query to filter the envelopes. It is a String parameter that
     *      represents the container value to search for in the database table.
     * @return An Optional object containing either the Envelope object found in the database based on the provided file
     *      name and container, or an empty Optional if no matching Envelope is found.
     */
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

    /**
     * The `insert` method generates a UUID, inserts data from a `NewEnvelope` object into a database table using JDBC
     * template, and returns the generated UUID.
     *
     * @param envelope The `insert` method you provided is inserting a new envelope into a database table named
     *      `envelopes`. The method generates a new UUID for the envelope, maps the envelope data to the
     *       corresponding columns in the table, and then executes an SQL `INSERT` statement using
     *      Spring's `JdbcTemplate`.
     * @return The method `insert` is returning a `UUID` which is generated using `UUID.randomUUID()`
     *      after inserting the data from the `NewEnvelope` object into the database table `envelopes`.
     */
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

    /**
     * The `updateStatus` function updates the status of an envelope in a database using JDBC template in Java.
     *
     * @param id A unique identifier for the envelope that needs its status updated.
     * @param newStatus The `newStatus` parameter in the `updateStatus` method is of type `Status`, which is an enum
     *      representing the status of an envelope. It is used to update the status of an envelope in the database.
     */
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

    /**
     * The function updates the dispatched_at field of an envelope in a database table using the
     * provided UUID and Instant values.
     *
     * @param id The `id` parameter is of type `UUID`, which stands for Universally Unique Identifier. It is a 128-bit
     *      value typically used as a unique identifier for entities in a system.
     * @param dispatchedAt The `dispatchedAt` parameter is of type `Instant`, which represents a moment
     *      on the timeline in UTC with a resolution of nanoseconds. In the provided code snippet, the
     *      `dispatchedAt` parameter is converted to a `Timestamp`
     *      object using `Timestamp.from(dispatchedAt)`.
     */
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

    /**
     * The function updates the "is_deleted" field in the "envelopes" table to True for a specific ID using a UUID
     * parameter.
     *
     * @param id UUID id
     * @return The method `markAsDeleted` is returning an integer value, which represents the number of rows affected by
     *      the SQL update operation.
     */
    public int markAsDeleted(UUID id) {
        return jdbcTemplate.update(
            "UPDATE envelopes "
                + "SET is_deleted = True "
                + "WHERE id = :id",
            new MapSqlParameterSource("id", id)
        );
    }

    /**
     * The function updates the pending_notification field in the envelopes table for a specific
     * id using a boolean value.
     *
     * @param id A unique identifier for the envelope.
     * @param notificationPending The `notificationPending` parameter is a boolean value that indicates whether a
     *      notification is pending for a specific envelope. It is used in the `updatePendingNotification` method
     *      to update the `pending_notification` field in the `envelopes` table for a given `id`.
     * @return The method `updatePendingNotification` returns an integer value, which represents the number of rows
     *      affected by the update operation in the database table "envelopes".
     */
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

    /**
     * The function retrieves the count of envelopes within a specified time range and belonging to a set of containers
     * using JDBC template in Java.
     *
     * @param containers A set of container names that you want to query for envelopes within a specific time range.
     * @param fromDateTime fromDateTime is the starting date and time from which you want to filter the envelopes.
     * @param toDateTime toDateTime is the timestamp representing the end date and time for the query.
     *      It is used to filter the envelopes based on their file creation timestamp, ensuring that only envelopes
     *      created before this specific date and time are included in the count.
     * @return The method is returning the count of envelopes that meet the specified criteria.
     *      It queries the database to count the number of envelopes where the container is in the provided
     *      set of containers and the file creation timestamp is between the specified fromDateTime and toDateTime.
     */
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
