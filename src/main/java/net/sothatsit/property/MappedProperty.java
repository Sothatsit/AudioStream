package net.sothatsit.property;

import javax.swing.event.ChangeListener;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A property whose value is mapped from the value of other Properties.
 *
 * @author Paddy Lamont
 */
public class MappedProperty<V> extends AbstractProperty<V> {

    private final Supplier<V> valueGenerator;
    private final ChangeListener updateListener;
    private V value;

    public MappedProperty(String name, Supplier<V> valueGenerator, ChangeListenable... updateTriggers) {
        super(name);

        this.valueGenerator = valueGenerator;

        this.updateListener = event -> updateValue(true);
        for (ChangeListenable updateTrigger : updateTriggers) {
            if (updateTrigger == null)
                throw new IllegalArgumentException("None of updateTriggers can be null");

            updateTrigger.addWeakChangeListener(updateListener);
        }

        updateValue(false);
    }

    private void updateValue(boolean triggerUpdate) {
        this.value = valueGenerator.get();

        if (triggerUpdate) {
            fireChangeEvent();
        }
    }

    @Override
    public V get() {
        return value;
    }

    public static <A, V> MappedProperty<V> map(String name,
                                               Property<A> property,
                                               Function<A, V> function) {

        return new MappedProperty<>(name, () -> {
            A argument = property.get();
            return function.apply(argument);
        }, property);
    }

    public static <A, B, V> MappedProperty<V> map(String name,
                                                  Property<A> property1,
                                                  Property<B> property2,
                                                  BiFunction<A, B, V> function) {

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            return function.apply(argument1, argument2);
        }, property1, property2);
    }

    public static <A, B, C, V> MappedProperty<V> map(String name,
                                                     Property<A> property1,
                                                     Property<B> property2,
                                                     Property<C> property3,
                                                     TriFunction<A, B, C, V> function) {

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            return function.apply(argument1, argument2, argument3);
        }, property1, property2, property3);
    }

    public static <A, B, C, D, V> MappedProperty<V> map(String name,
                                                        Property<A> property1,
                                                        Property<B> property2,
                                                        Property<C> property3,
                                                        Property<D> property4,
                                                        QuadFunction<A, B, C, D, V> function) {

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            D argument4 = property4.get();
            return function.apply(argument1, argument2, argument3, argument4);
        }, property1, property2);
    }

    public static <V> MappedProperty<V> mapMany(String name, Supplier<V> valueGenerator, ChangeListenable... sources) {
        return new MappedProperty<>(name, valueGenerator, sources);
    }
}
