package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class StatisticsRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DayCountMapper mapper;

    public StatisticsRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        DayCountMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public List<DayCount> getEnvelopesDayStatistics(LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                "SELECT series.day as interval, coalesce(cnt.cnt,0) as cnt FROM\n"
                        + "    (\n"
                        + "    SELECT COUNT(*) cnt, \n"
                        + "      to_timestamp(floor((extract('epoch' from file_created_at) / 86400 )) * 86400) \n"
                        + "      AT TIME ZONE 'UTC' as interval_alias\n"
                        + "      FROM envelopes\n"
                        + "      WHERE file_created_at BETWEEN :from AND :to\n"
                        + "      GROUP BY interval_alias\n"
                        + "    ) cnt\n"
                        + "RIGHT JOIN\n"
                        + "    (\n"
                        + "    SELECT generate_series(MIN(date_trunc('day', file_created_at)),\n"
                        + "        max(date_trunc('day', file_created_at)), '1d') as day \n"
                        + "    FROM envelopes\n"
                        + "    WHERE file_created_at BETWEEN :from AND :to\n"
                        + "    ) series\n"
                        + "ON series.day = cnt.interval_alias\n"
                        + "ORDER BY interval\n",
                new MapSqlParameterSource()
                        .addValue("from", from)
                        .addValue("to", to),
                mapper
        );
    }
}
