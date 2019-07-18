package net.sothatsit.audiostream.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.sothatsit.function.Unchecked;

/**
 * Serializes values of the given type to/from serialized types.
 *
 * @author Paddy Lamont
 */
public interface Serializer<V> {

    public static final Serializer<Boolean> BOOL_SERIALIZER = new Serializer<Boolean>() {
        @Override
        public JsonPrimitive serializeToJson(Boolean value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Boolean deserializeFromJson(JsonElement value) {
            return value.getAsBoolean();
        }
    };

    public static final Serializer<Number> NUM_SERIALIZER = new Serializer<Number>() {
        @Override
        public JsonPrimitive serializeToJson(Number value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Number deserializeFromJson(JsonElement value) {
            return value.getAsNumber();
        }
    };

    public static final Serializer<Character> CHAR_SERIALIZER = new Serializer<Character>() {
        @Override
        public JsonPrimitive serializeToJson(Character value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Character deserializeFromJson(JsonElement value) {
            return value.getAsCharacter();
        }
    };

    public static final Serializer<String> STRING_SERIALIZER = new Serializer<String>() {
        @Override
        public JsonPrimitive serializeToJson(String value) {
            return new JsonPrimitive(value);
        }

        @Override
        public String deserializeFromJson(JsonElement value) {
            return value.getAsString();
        }
    };

    /**
     * Serialize {@param value} to Json.
     */
    public default JsonElement serializeToJson(V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserialize {@param value}.
     */
    public default V deserializeFromJson(JsonElement value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return a default Serializer to serialize {@param type}.
     * @throws IllegalArgumentException if no serializer could be found for {@param type}.
     */
    public static <V> Serializer<V> get(Class<V> type) {
        if (type == null)
            throw new IllegalArgumentException("type cannot be null");
        if (Boolean.class.isAssignableFrom(type))
            return Unchecked.cast(BOOL_SERIALIZER);
        if (Number.class.isAssignableFrom(type))
            return Unchecked.cast(NUM_SERIALIZER);
        if (Character.class.isAssignableFrom(type))
            return Unchecked.cast(CHAR_SERIALIZER);
        if (String.class.isAssignableFrom(type))
            return Unchecked.cast(STRING_SERIALIZER);

        throw new IllegalArgumentException("Unable to determine json Serializer for type " + type);
    }
}
