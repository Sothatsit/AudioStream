package net.sothatsit.property;

import javax.swing.event.ChangeListener;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A holder that has a value, and notifies listeners whenever this value is changed.
 *
 * @author Paddy Lamont
 */
public interface Property<V> extends ChangeListenable {

    public static final ConstantProperty<Boolean> TRUE = constant("TRUE", true);
    public static final ConstantProperty<Boolean> FALSE = constant("FALSE", false);

    /**
     * @return The name of this Property.
     */
    public String getName();

    /**
     * Set the value of this Property to {@param updatedValue} iff the current value is {@param expectedValue}.
     */
    public default void compareAndSet(V expectedValue, V updatedValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the value of this Property to {@param value}.
     */
    public default void set(V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return The value of this Property.
     */
    public V get();

    /**
     * @return The value of this Property, or {@param defaultValue} if the value of this Property is null.
     */
    public default V getOrDefault(V defaultValue) {
        V value = get();
        return (value == null ? defaultValue : value);
    }

    /**
     * Add a change listener to be invoked using the given properties.
     */
    public default ChangeListener addValueListener(Consumer<V> listener, ChangeListenerProperties properties) {
        ChangeListener changeListener = event -> listener.accept(get());
        addChangeListener(changeListener, properties);
        return changeListener;
    }

    /**
     * Add a change listener.
     */
    public default ChangeListener addValueListener(Consumer<V> listener) {
        return addValueListener(listener, ChangeListenerProperties.create());
    }

    /**
     * Add a change listener that is held using a weak reference.
     */
    public default ChangeListener addWeakValueListener(Consumer<V> listener) {
        return addValueListener(listener, ChangeListenerProperties.createWeak());
    }

    /**
     * Add a change listener that should be invoked on the EDT.
     */
    public default ChangeListener addEDTValueListener(Consumer<V> listener) {
        return addValueListener(listener, ChangeListenerProperties.createOnEDT());
    }

    /**
     * Add a change listener that should be invoked on the EDT.
     */
    public default ChangeListener addWeakEDTValueListener(Consumer<V> listener) {
        return addValueListener(listener, ChangeListenerProperties.createWeakOnEDT());
    }

    /**
     * @return A wrapper around this Property that blocks any modification operations.
     */
    public default ReadOnlyProperty<V> readOnly() {
        return new ReadOnlyProperty<>(this);
    }

    /**
     * @return A Property whose change events are all propagated on the EDT.
     */
    public default RedirectedEventsProperty<V> onEDT() {
        return new RedirectedEventsProperty<>(this, ChangeListenerProperties.createOnEDT());
    }

    /**
     * @return A Property whose change events are all propagated off the EDT.
     */
    public default RedirectedEventsProperty<V> offEDT() {
        return new RedirectedEventsProperty<>(this, ChangeListenerProperties.createOffEDT());
    }

    /**
     * @return A wrapper around this Property that always returns a non-null version of the value
     *         of this Property, and that does not allow itself to be set to a null value.
     */
    public default NonNullProperty<V> nonNull(V nullReplacementValue) {
        if (nullReplacementValue == null)
            throw new IllegalArgumentException("The null replacement value cannot be null");

        return new NonNullProperty<>(this, nullReplacementValue);
    }

    /**
     * @return A Property whose value is mapped from this property by {@param function}.
     */
    public default <T> MappedProperty<T> map(String name, Function<V, T> function) {
        return map(name, this, function);
    }

    /**
     * @return A Property whose value is false if {@param property} is null, and true if it is not,
     *         and whose name is generated from the name of {@param property}.
     */
    public default <T> MappedProperty<Boolean> isNull() {
        return isNull(this);
    }

    /**
     * @return A Property whose value is false if {@param property} is null, and true if it is not.
     */
    public default <T> MappedProperty<Boolean> isNull(String name) {
        return isNull(name, this);
    }

    /**
     * @return A Property whose value is false if {@param property} is null, and true if it is not,
     *         and whose name is generated from the name of {@param property}.
     */
    public default <T> MappedProperty<Boolean> isNotNull() {
        return isNotNull(this);
    }

    /**
     * @return A Property whose value is false if {@param property} is null, and true if it is not.
     */
    public default <T> MappedProperty<Boolean> isNotNull(String name) {
        return isNotNull(name, this);
    }

    /**
     * @return A new modifiable Property.
     */
    public static <V> ModifiableProperty<V> create(String name) {
        return new ModifiableProperty<>(name);
    }

    /**
     * @return A new modifiable Property with the initial value {@param initialValue}.
     */
    public static <V> ModifiableProperty<V> create(String name, V initialValue) {
        return new ModifiableProperty<>(name, initialValue);
    }

    /**
     * @return A new modifiable Property with the initial value {@param initialValue}.
     */
    public static <V> NonNullProperty<V> createNonNull(String name, V initialValue) {
        if (initialValue == null)
            throw new IllegalArgumentException("initialValue cannot be null when creating a non-null property");

        return create(name, initialValue).nonNull(initialValue);
    }

    /**
     * @return A Property whose value is mapped from {@param property} by {@param function}.
     */
    public static <A, V> MappedProperty<V> map(String name,
                                               Property<A> property,
                                               Function<A, V> function) {

        return MappedProperty.map(name, property, function);
    }

    /**
     * @return A Property whose value is mapped from {@param property1} and {@param property2} by {@param function}.
     */
    public static <A, B, V> MappedProperty<V> map(String name,
                                                  Property<A> property1,
                                                  Property<B> property2,
                                                  BiFunction<A, B, V> function) {

        return MappedProperty.map(name, property1, property2, function);
    }

    /**
     * @return A Property whose value is mapped from {@param property1}, {@param property2},
     *         and {@param property3} by {@param function}.
     */
    public static <A, B, C, V> MappedProperty<V> map(String name,
                                                     Property<A> property1,
                                                     Property<B> property2,
                                                     Property<C> property3,
                                                     TriFunction<A, B, C, V> function) {

        return MappedProperty.map(name, property1, property2, property3, function);
    }

    /**
     * @return A Property whose value is mapped from {@param property1}, {@param property2},
     *         {@param property3}, and {@param property4} by {@param function}.
     */
    public static <A, B, C, D, V> MappedProperty<V> map(String name,
                                                        Property<A> property1,
                                                        Property<B> property2,
                                                        Property<C> property3,
                                                        Property<D> property4,
                                                        QuadFunction<A, B, C, D, V> function) {

        return MappedProperty.map(name, property1, property2, property3, property4, function);
    }

    /**
     * @return A Property whose value is the value of the innermost property.
     */
    public static <T> FlatMapProperty<T> flatMap(String name, Property<Property<T>> property) {
        return new FlatMapProperty<>(name, property);
    }

    /**
     * @return A Property whose value is the value of the Property mapped from the value of {@param property}.
     */
    public static <H, R> FlatMapProperty<R> flatMap(String name,
                                                    Property<H> property,
                                                    Function<H, Property<R>> toProperty) {

        String intermediaryName = "flatMap_intermediate_value_to_property(" + name + ")";

        return new FlatMapProperty<>(name, property.map(intermediaryName, toProperty));
    }

    /**
     * @return A Property whose value is always {@param value}.
     */
    public static <V> ConstantProperty<V> constant(String name, V value) {
        return new ConstantProperty<>(name, value);
    }

    /**
     * @return A ListProperty whose value is always {@param value}.
     */
    public static <E> ConstantListProperty<E> constantList(String name, E[] values) {
        return new ConstantListProperty<>(name, values);
    }

    /**
     * @return A ListProperty whose value is always {@param value}.
     */
    public static <E> ConstantListProperty<E> constantList(String name, List<? extends E> values) {
        return new ConstantListProperty<>(name, values);
    }

    /**
     * @return A Property whose value is {@param trueValue} if
     *         {@param condition} is true, else it is {@param falseValue}.
     */
    public static <V> MappedProperty<V> ternary(String name, Property<Boolean> condition, V trueValue, V falseValue) {
        return condition.map(name, condValue -> (condValue != null && condValue ? trueValue : falseValue));
    }

    /**
     * @return A Property whose value is true iff the values of all {@param conditions} are true,
     *         and whose name is generated from the names of the passed {@param conditions}.
     */
    @SafeVarargs
    public static MappedProperty<Boolean> and(Property<Boolean>... conditions) {
        StringBuilder name = new StringBuilder("and(");

        for (int index = 0; index < conditions.length; ++index) {
            if (index > 0) {
                name.append(", ");
            }
            name.append(conditions[index].getName());
        }

        name.append(")");
        return and(name.toString(), conditions);
    }

    /**
     * @return A Property whose value is true iff the values of all {@param conditions} are true.
     */
    @SafeVarargs
    public static MappedProperty<Boolean> and(String name, Property<Boolean>... conditions) {
        if (conditions.length == 0)
            throw new IllegalArgumentException("conditions cannot be empty");

        return MappedProperty.mapMany(name, () -> {
            for (Property<Boolean> property : conditions) {
                Boolean value = property.get();
                if (value == null || !value)
                    return false;
            }
            return true;
        }, (ChangeListenable[]) conditions);
    }

    /**
     * @return A Property whose value is true iff one of {@param conditions} is true,
     *         and whose name is generated from the names of the passed {@param conditions}.
     */
    @SafeVarargs
    public static MappedProperty<Boolean> or(Property<Boolean>... conditions) {
        StringBuilder name = new StringBuilder("or(");

        for (int index = 0; index < conditions.length; ++index) {
            if (index > 0) {
                name.append(", ");
            }
            name.append(conditions[index].getName());
        }

        name.append(")");
        return or(name.toString(), conditions);
    }

    /**
     * @return A Property whose value is true iff one of {@param conditions} is true.
     */
    @SafeVarargs
    public static MappedProperty<Boolean> or(String name, Property<Boolean>... conditions) {
        if (conditions.length == 0)
            throw new IllegalArgumentException("conditions cannot be empty");

        return MappedProperty.mapMany(name, () -> {
            for (Property<Boolean> property : conditions) {
                Boolean value = property.get();
                if (value != null && value)
                    return true;
            }
            return false;
        }, (ChangeListenable[]) conditions);
    }

    /**
     * @return A Property whose value is false if {@param property} is true, or true otherwise,
     *         and whose name is generated from the name of {@param property}.
     */
    public static MappedProperty<Boolean> not(Property<Boolean> property) {
        return not("not(" + property.getName() + ")", property);
    }

    /**
     * @return A Property whose value is false if {@param property} is true, or true otherwise.
     */
    public static MappedProperty<Boolean> not(String name, Property<Boolean> property) {
        return property.map(name, value -> (value == null || !value));
    }

    /**
     * @return A Property whose value is false if {@param property} is null, and true if it is not,
     *         and whose name is generated from the name of {@param property}.
     */
    public static MappedProperty<Boolean> isNotNull(Property<?> property) {
        return isNotNull("notNull(" + property.getName() + ")", property);
    }

    /**
     * @return A Property whose value is false if {@param property} is null, and true if it is not.
     */
    public static MappedProperty<Boolean> isNotNull(String name, Property<?> property) {
        return property.map(name, value -> value != null);
    }

    /**
     * @return A Property whose value is true if {@param property} is null, and false if it is not,
     *         and whose name is generated from the name of {@param property}.
     */
    public static MappedProperty<Boolean> isNull(Property<?> property) {
        return isNull("isNull(" + property.getName() + ")", property);
    }

    /**
     * @return A Property whose value is true if {@param property} is null, and false if it is not.
     */
    public static MappedProperty<Boolean> isNull(String name, Property<?> property) {
        return property.map(name, value -> value == null);
    }
}
