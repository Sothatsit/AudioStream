package net.sothatsit.property;

import net.sothatsit.property.event.ChangeListenerProperties;

import javax.swing.event.ChangeListener;
import java.util.function.Consumer;

/**
 * An attribute that can be set to the value of another Property.
 *
 * @author Paddy Lamont
 */
public class Attribute<V> implements Property<V> {

    private final String name;
    private final Property<Property<V>> valueProperty;
    private final Property<V> value;

    public Attribute(String name, V defaultValue, boolean nullable) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (!nullable && defaultValue == null)
            throw new IllegalArgumentException("defaultValue cannot be null if attribute is not nullable");

        this.name = name;
        this.valueProperty = Property.create(name + "Property");

        if (nullable) {
            Property<V> defaultConstant = Property.constant(name + "Default", defaultValue);

            this.value = Property.flatMap(name, valueProperty.nonNull(defaultConstant));
        } else {
            this.value = Property.flatMap(name, valueProperty).nonNull(defaultValue);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void set(V value) {
        set(Property.constant(name, value));
    }

    public void set(Property<V> valueProperty) {
        this.valueProperty.set(valueProperty);
    }

    public V get() {
        return value.get();
    }

    @Override
    public void addChangeListener(ChangeListener listener, ChangeListenerProperties properties) {
        value.addChangeListener(listener, properties);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        value.removeChangeListener(listener);
    }

    public static <V> Attribute<V> createNullable(String name) {
        return createNullable(name, null);
    }

    public static <V> Attribute<V> createNullable(String name, V defaultValue) {
        return new Attribute<>(name, defaultValue, true);
    }

    public static <V> Attribute<V> createNullable(String name, V defaultValue, Consumer<V> applyListener) {
        Attribute<V> attribute = createNullable(name, defaultValue);
        attribute.addEDTValueListener(applyListener);
        return attribute;
    }

    public static <V> Attribute<V> createNonNull(String name, V defaultValue) {
        return new Attribute<>(name, defaultValue, false);
    }

    public static <V> Attribute<V> createNonNull(String name, V defaultValue, Consumer<V> applyListener) {
        Attribute<V> attribute = createNonNull(name, defaultValue);
        attribute.addEDTValueListener(applyListener);
        return attribute;
    }
}
