package uk.gov.hmcts.reform.blobrouter.data;

import org.postgresql.util.PGobject;

import java.sql.SQLException;

public final class Utils {
    private Utils() {
        // utils class
    }

    /***
     * Converts json string to postgres object.
     */
    public static PGobject toJson(String jsonString) throws SQLException {
        var json = new PGobject();
        json.setType("json");
        json.setValue(jsonString);
        return json;
    }
}
