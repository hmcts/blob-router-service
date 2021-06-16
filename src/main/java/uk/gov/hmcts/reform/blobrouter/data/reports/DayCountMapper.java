package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class DayCountMapper implements RowMapper<DayCount> {

    @Override
    public DayCount mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DayCount(
            rs.getTimestamp("interval").toLocalDateTime().toLocalDate(),
            rs.getInt("cnt")
        );
    }
}
