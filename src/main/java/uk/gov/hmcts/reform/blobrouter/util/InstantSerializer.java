package uk.gov.hmcts.reform.blobrouter.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;

/**
 * The `InstantSerializer` class in Java serializes Instant objects to a simple date-time format using a custom
 * DateFormatter.
 */
public final class InstantSerializer extends StdSerializer<Instant> {

    private InstantSerializer() {
        super(Instant.class);
    }

    /**
     * The function serializes an Instant value to a simple date-time format using a custom DateFormatter class.
     *
     * @param value The `value` parameter in the `serialize` method represents the Instant object
     *              that needs to be serialized into a JSON format.
     * @param gen The `gen` parameter in the `serialize` method is an instance of `JsonGenerator`.
     *            It is used to write JSON content, such as strings, numbers, objects, and arrays, to
     *            the output stream.
     * @param provider The `provider` parameter in the `serialize` method is an
     *                 instance of `SerializerProvider` class. It provides contextual information
     *                 and helper methods for serialization, such as accessing configuration
     *                 settings, handling type information, and resolving serializers for specific types.
     */
    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(DateFormatter.getSimpleDateTime(value));
    }
}
