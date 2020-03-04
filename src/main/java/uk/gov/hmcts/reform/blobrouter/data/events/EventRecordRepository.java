package uk.gov.hmcts.reform.blobrouter.data.events;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventRecordRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EventRecordMapper mapper;

    public EventRecordRepository(NamedParameterJdbcTemplate jdbcTemplate, EventRecordMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public List<EventRecord> find(String container, String fileName) {
        return jdbcTemplate.query(
            "SELECT * FROM events WHERE container = :container AND file_name = :fileName",
            new MapSqlParameterSource()
                .addValue("container", container)
                .addValue("fileName", fileName),
            this.mapper
        );
    }

    public long insert(NewEventRecord eventRecord) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
            "INSERT INTO events (container, file_name, created_at, event, notes) "
                + "VALUES (:container, :fileName, CURRENT_TIMESTAMP, :event, :notes)",
            new MapSqlParameterSource()
                .addValue("container", eventRecord.container)
                .addValue("fileName", eventRecord.fileName)
                .addValue("event", eventRecord.event.name())
                .addValue("notes", eventRecord.notes),
            keyHolder,
            new String[]{ "id" }
        );

        return (long) keyHolder.getKey();
    }
}
