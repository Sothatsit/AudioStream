package net.sothatsit.audiostream.config;

import com.google.gson.JsonElement;
import net.sothatsit.audiostream.property.ModifiableProperty;
import net.sothatsit.audiostream.property.Property;

/**
 * A Property that can be serialized.
 *
 * @author Paddy Lamont
 */
public class SerializableProperty<V> extends ModifiableProperty<V> implements Serializable {

    private final Serializer<V> valueSerializer;

    public SerializableProperty(String name, Serializer<V> valueSerializer) {
        super(name);

        if (valueSerializer == null)
            throw new IllegalArgumentException("valueSerializer cannot be null");

        this.valueSerializer = valueSerializer;
    }

    public SerializableProperty(String name, V value, Serializer<V> valueSerializer) {
        super(name, value);

        if (valueSerializer == null)
            throw new IllegalArgumentException("valueSerializer cannot be null");

        this.valueSerializer = valueSerializer;
    }

    @Override
    public JsonElement serializeToJson() {
        return valueSerializer.serializeToJson(get());
    }

    @Override
    public void deserializeFromJson(JsonElement value) {
        set(valueSerializer.deserializeFromJson(value));
    }
}
