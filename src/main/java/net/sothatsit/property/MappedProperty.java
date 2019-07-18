package net.sothatsit.property;

import net.sothatsit.function.*;
import net.sothatsit.property.event.ChangeListenable;

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
    private final ChangeListenable[] updateTriggers;
    private final ChangeListener updateListener;

    private final Object lock = new Object();
    private V value;
    private long lastUpdateTime = -1;

    public MappedProperty(String name, Supplier<V> valueGenerator, ChangeListenable... updateTriggers) {
        super(name);

        this.valueGenerator = valueGenerator;
        this.updateTriggers = updateTriggers;

        this.updateListener = event -> updateValue(true);
        for (ChangeListenable updateTrigger : updateTriggers) {
            if (updateTrigger == null)
                throw new IllegalArgumentException("None of updateTriggers can be null");

            updateTrigger.addWeakChangeListener(updateListener);
        }

        updateValue(false);
    }

    private void updateValue(boolean triggerUpdate) {
        V newValue = valueGenerator.get();
        long time = System.nanoTime();

        synchronized (lock) {
            // Check if the new value is stale
            if (time < this.lastUpdateTime)
                return;

            // Update the value
            this.lastUpdateTime = time;
            this.value = newValue;
        }

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
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property == null)
            throw new IllegalArgumentException("property cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new MappedProperty<>(name, () -> {
            A argument = property.get();
            return function.apply(argument);
        }, property);
    }

    public static <A, B, V> MappedProperty<V> map(String name,
                                                  Property<A> property1,
                                                  Property<B> property2,
                                                  BiFunction<A, B, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

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
                                                     ThreeFunction<A, B, C, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (property3 == null)
            throw new IllegalArgumentException("property3 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

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
                                                        FourFunction<A, B, C, D, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (property3 == null)
            throw new IllegalArgumentException("property3 cannot be null");
        if (property4 == null)
            throw new IllegalArgumentException("property4 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            D argument4 = property4.get();
            return function.apply(argument1, argument2, argument3, argument4);
        }, property1, property2, property3, property4);
    }

    public static <A, B, C, D, E, V> MappedProperty<V> map(String name,
                                                        Property<A> property1,
                                                        Property<B> property2,
                                                        Property<C> property3,
                                                        Property<D> property4,
                                                        Property<E> property5,
                                                        FiveFunction<A, B, C, D, E, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (property3 == null)
            throw new IllegalArgumentException("property3 cannot be null");
        if (property4 == null)
            throw new IllegalArgumentException("property4 cannot be null");
        if (property5 == null)
            throw new IllegalArgumentException("property5 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            D argument4 = property4.get();
            E argument5 = property5.get();
            return function.apply(argument1, argument2, argument3, argument4, argument5);
        }, property1, property2, property3, property4, property5);
    }

    public static <A, B, C, D, E, F, V> MappedProperty<V> map(String name,
                                                              Property<A> property1,
                                                              Property<B> property2,
                                                              Property<C> property3,
                                                              Property<D> property4,
                                                              Property<E> property5,
                                                              Property<F> property6,
                                                              SixFunction<A, B, C, D, E, F, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (property3 == null)
            throw new IllegalArgumentException("property3 cannot be null");
        if (property4 == null)
            throw new IllegalArgumentException("property4 cannot be null");
        if (property5 == null)
            throw new IllegalArgumentException("property5 cannot be null");
        if (property6 == null)
            throw new IllegalArgumentException("property6 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            D argument4 = property4.get();
            E argument5 = property5.get();
            F argument6 = property6.get();
            return function.apply(argument1, argument2, argument3, argument4, argument5, argument6);
        }, property1, property2, property3, property4, property5, property6);
    }

    public static <A, B, C, D, E, F, G, V> MappedProperty<V> map(String name,
                                                              Property<A> property1,
                                                              Property<B> property2,
                                                              Property<C> property3,
                                                              Property<D> property4,
                                                              Property<E> property5,
                                                              Property<F> property6,
                                                              Property<G> property7,
                                                              SevenFunction<A, B, C, D, E, F, G, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (property3 == null)
            throw new IllegalArgumentException("property3 cannot be null");
        if (property4 == null)
            throw new IllegalArgumentException("property4 cannot be null");
        if (property5 == null)
            throw new IllegalArgumentException("property5 cannot be null");
        if (property6 == null)
            throw new IllegalArgumentException("property6 cannot be null");
        if (property7 == null)
            throw new IllegalArgumentException("property7 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            D argument4 = property4.get();
            E argument5 = property5.get();
            F argument6 = property6.get();
            G argument7 = property7.get();
            return function.apply(argument1, argument2, argument3, argument4, argument5, argument6, argument7);
        }, property1, property2, property3, property4, property5, property6, property7);
    }

    public static <A, B, C, D, E, F, G, H, V> MappedProperty<V> map(String name,
                                                                    Property<A> property1,
                                                                    Property<B> property2,
                                                                    Property<C> property3,
                                                                    Property<D> property4,
                                                                    Property<E> property5,
                                                                    Property<F> property6,
                                                                    Property<G> property7,
                                                                    Property<H> property8,
                                                                    EightFunction<A, B, C, D, E, F, G, H, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (property3 == null)
            throw new IllegalArgumentException("property3 cannot be null");
        if (property4 == null)
            throw new IllegalArgumentException("property4 cannot be null");
        if (property5 == null)
            throw new IllegalArgumentException("property5 cannot be null");
        if (property6 == null)
            throw new IllegalArgumentException("property6 cannot be null");
        if (property7 == null)
            throw new IllegalArgumentException("property7 cannot be null");
        if (property8 == null)
            throw new IllegalArgumentException("property8 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            D argument4 = property4.get();
            E argument5 = property5.get();
            F argument6 = property6.get();
            G argument7 = property7.get();
            H argument8 = property8.get();
            return function.apply(
                    argument1, argument2, argument3, argument4, argument5, argument6, argument7, argument8
            );
        }, property1, property2, property3, property4, property5, property6, property7, property8);
    }

    public static <A, B, C, D, E, F, G, H, I, V> MappedProperty<V> map(String name,
                                                                       Property<A> property1,
                                                                       Property<B> property2,
                                                                       Property<C> property3,
                                                                       Property<D> property4,
                                                                       Property<E> property5,
                                                                       Property<F> property6,
                                                                       Property<G> property7,
                                                                       Property<H> property8,
                                                                       Property<I> property9,
                                                                       NineFunction<A, B, C, D, E, F, G, H, I, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (property3 == null)
            throw new IllegalArgumentException("property3 cannot be null");
        if (property4 == null)
            throw new IllegalArgumentException("property4 cannot be null");
        if (property5 == null)
            throw new IllegalArgumentException("property5 cannot be null");
        if (property6 == null)
            throw new IllegalArgumentException("property6 cannot be null");
        if (property7 == null)
            throw new IllegalArgumentException("property7 cannot be null");
        if (property8 == null)
            throw new IllegalArgumentException("property8 cannot be null");
        if (property9 == null)
            throw new IllegalArgumentException("property9 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            D argument4 = property4.get();
            E argument5 = property5.get();
            F argument6 = property6.get();
            G argument7 = property7.get();
            H argument8 = property8.get();
            I argument9 = property9.get();
            return function.apply(
                    argument1, argument2, argument3, argument4, argument5, argument6, argument7, argument8, argument9
            );
        }, property1, property2, property3, property4, property5, property6, property7, property8, property9);
    }

    public static <A, B, C, D, E, F, G, H, I, J, V> MappedProperty<V> map(String name,
                                                                          Property<A> property1,
                                                                          Property<B> property2,
                                                                          Property<C> property3,
                                                                          Property<D> property4,
                                                                          Property<E> property5,
                                                                          Property<F> property6,
                                                                          Property<G> property7,
                                                                          Property<H> property8,
                                                                          Property<I> property9,
                                                                          Property<J> property10,
                                                                          TenFunction<A, B, C, D, E, F, G, H, I, J, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (property3 == null)
            throw new IllegalArgumentException("property3 cannot be null");
        if (property4 == null)
            throw new IllegalArgumentException("property4 cannot be null");
        if (property5 == null)
            throw new IllegalArgumentException("property5 cannot be null");
        if (property6 == null)
            throw new IllegalArgumentException("property6 cannot be null");
        if (property7 == null)
            throw new IllegalArgumentException("property7 cannot be null");
        if (property8 == null)
            throw new IllegalArgumentException("property8 cannot be null");
        if (property9 == null)
            throw new IllegalArgumentException("property9 cannot be null");
        if (property10 == null)
            throw new IllegalArgumentException("property10 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new MappedProperty<>(name, () -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            D argument4 = property4.get();
            E argument5 = property5.get();
            F argument6 = property6.get();
            G argument7 = property7.get();
            H argument8 = property8.get();
            I argument9 = property9.get();
            J argument10 = property10.get();
            return function.apply(
                    argument1, argument2, argument3, argument4, argument5,
                    argument6, argument7, argument8, argument9, argument10
            );
        }, property1, property2, property3, property4, property5, property6, property7, property8, property9, property10);
    }

    public static <V> MappedProperty<V> mapMany(String name, Supplier<V> valueGenerator, ChangeListenable... sources) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (valueGenerator == null)
            throw new IllegalArgumentException("valueGenerator cannot be null");
        if (sources == null)
            throw new IllegalArgumentException("sources cannot be null");

        for (ChangeListenable source : sources) {
            if (source == null)
                throw new IllegalArgumentException("None of sources can be null");
        }

        return new MappedProperty<>(name, valueGenerator, sources);
    }
}
