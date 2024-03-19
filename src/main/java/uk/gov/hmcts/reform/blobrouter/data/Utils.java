package uk.gov.hmcts.reform.blobrouter.data;

import org.postgresql.util.PGobject;

import java.sql.SQLException;

/**
 * The `Utils` class provides a static method `toJson` that converts a JSON string into a PGobject with type "json".
 */
public final class Utils {
    private Utils() {
        // utils class
    }

    /**
     * The function `toJson` converts a JSON string into a PGobject with type "json".
     *
     * @param jsonString The `jsonString` parameter in the `toJson` method is a String that represents a JSON object.
     *                   This method creates a `PGobject` instance, sets its type to "json", assigns the JSON string
     *                   to it, and returns the `PGobject`.
     * @return A PGobject containing the JSON string is being returned.
     */
    public static PGobject toJson(String jsonString) throws SQLException {
        var json = new PGobject();
        json.setType("json");
        json.setValue(jsonString);
        return json;
    }
}
