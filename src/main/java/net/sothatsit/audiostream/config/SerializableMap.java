package net.sothatsit.audiostream.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.sothatsit.audiostream.property.ChangeEventSource;
import net.sothatsit.audiostream.property.ChangePropagatingMapProperty;
import net.sothatsit.audiostream.property.NonNullProperty;
import net.sothatsit.audiostream.property.Property;
import net.sothatsit.audiostream.util.Unchecked;

import java.util.Map;

/**
 * A Serializable ChangePropagatingMapProperty to be used to
 * construct auto-saving configs, and to be used to easily reload configs.
 *
 * @author Paddy Lamont
 */
public class SerializableMap extends ChangeEventSource implements Serializable {

    private final ChangePropagatingMapProperty<String, Serializable> entries;

    public SerializableMap() {
        this.entries = new ChangePropagatingMapProperty<>("entries");
        this.entries.addChangeListener(this::fireChangeEvent);
    }

    /**
     * Put {@param value} into this ConfigMap at {@param key}.
     */
    public void put(String key, Serializable value) {
        if (entries.containsKey(key))
            throw new IllegalArgumentException("There already exists a value with the given key");

        Serializable previousValue = entries.put(key, value);
        if (previousValue != null) {
            entries.put(key, previousValue);
            throw new IllegalArgumentException("There already exists a value with the given key");
        }
    }

    /**
     * Get the Serializable value associated with {@param key}.
     */
    public Serializable get(String key) {
        return entries.get(key);
    }

    /**
     * Create a non-null Property in this ConfigMap, with the Serialization
     * values automatically determined from the type of {@param defaultValue}.
     */
    public <T> NonNullProperty<T> createNonNullProperty(String name, T defaultValue) {
        if (defaultValue == null)
            throw new IllegalArgumentException("defaultValue cannot be null for a non-null property");

        return createProperty(name, defaultValue).nonNull(defaultValue);
    }

    /**
     * Create a non-null Property in this ConfigMap, with the Serialization
     * of its values automatically determined from {@param type}.
     */
    public <T> NonNullProperty<T> createNonNullProperty(String name, T defaultValue, Class<T> type) {
        if (defaultValue == null)
            throw new IllegalArgumentException("defaultValue cannot be null for a non-null property");

        return createProperty(name, defaultValue, type).nonNull(defaultValue);
    }

    /**
     * Create a non-null Property in this ConfigMap.
     */
    public <T> NonNullProperty<T> createNonNullProperty(String name, T defaultValue, Serializer<T> serializer) {
        if (defaultValue == null)
            throw new IllegalArgumentException("defaultValue cannot be null for a non-null property");

        return createProperty(name, defaultValue, serializer).nonNull(defaultValue);
    }

    /**
     * Create a Property in this ConfigMap, with the Serialization values
     * automatically determined from the type of {@param defaultValue}.
     */
    public <T> Property<T> createProperty(String name, T defaultValue) {
        if (defaultValue == null)
            throw new IllegalArgumentException("The type of defaultValue cannot be inferred as it is null");

        return createProperty(name, defaultValue, Unchecked.<Class<T>> cast(defaultValue.getClass()));
    }

    /**
     * Create a Property in this ConfigMap, with the Serialization
     * of its values automatically determined from {@param type}.
     */
    public <T> Property<T> createProperty(String name, T defaultValue, Class<T> type) {
        return createProperty(name, defaultValue, Serializer.get(type));
    }

    /**
     * Create a Property in this ConfigMap.
     */
    public <T> Property<T> createProperty(String name, T defaultValue, Serializer<T> serializer) {

        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (serializer == null)
            throw new IllegalArgumentException("serializer cannot be null");

        SerializableProperty<T> property = new SerializableProperty<>(
                name, defaultValue, serializer
        );

        put(name, property);
        return property;
    }

    @Override
    public JsonObject serializeToJson() {
        JsonObject object = new JsonObject();

        for (Map.Entry<String, Serializable> entry : entries.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue().serializeToJson();

            object.add(key, value);
        }

        return object;
    }

    @Override
    public void deserializeFromJson(JsonElement element) {
        if (!element.isJsonObject())
            throw new IllegalArgumentException("value is not a JsonObject");

        JsonObject object = element.getAsJsonObject();

        RuntimeException deserializationException = null;

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            Serializable valueHolder = get(key);
            try {
                if (valueHolder == null)
                    throw new IllegalStateException("Could not find key " + key + " to deserialize");

                valueHolder.deserializeFromJson(value);
            } catch (RuntimeException exception) {
                if (valueHolder != null) {
                    exception = new RuntimeException("Exception while deserializing key " + key, exception);
                }

                if (deserializationException == null) {
                    deserializationException = exception;
                } else {
                    deserializationException.addSuppressed(exception);
                }
            }
        }

        // Only throw at the end, as we want to try restore as much as we can
        if (deserializationException != null)
            throw deserializationException;
    }
}
