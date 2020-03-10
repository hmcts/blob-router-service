package uk.gov.hmcts.reform.blobrouter.data;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Profile("db-test")
@Component
public class DbHelper {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DbHelper(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM envelope_events", new MapSqlParameterSource());
        jdbcTemplate.update("DELETE FROM envelopes", new MapSqlParameterSource());
    }
}
