package net.sothatsit.audiostream.config;

import com.google.gson.JsonElement;

/**
 * Allows objects to mark that their contents can be serialized.
 *
 * @author Paddy Lamont
 */
public interface Serializable {

    public default JsonElement serializeToJson() {
        throw new UnsupportedOperationException();
    }

    public default void deserializeFromJson(JsonElement value) {
        throw new UnsupportedOperationException();
    }
}
