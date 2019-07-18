package net.sothatsit.property;

import net.sothatsit.function.FourFunction;
import net.sothatsit.function.ThreeFunction;
import net.sothatsit.property.event.ChangeListenable;

import javax.swing.event.ChangeListener;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A property whose value is mapped from the value of other Properties as well as its previous value.
 *
 * @author Paddy Lamont
 */
public class SelfMappedProperty<V> extends AbstractProperty<V> {

    // TODO : If this proves to be useful, would be good to
    //        add convenience functions to the Property class
    // i.e. Property.selfMap(...)

    private final Function<V, V> valueGenerator;
    private final ChangeListenable[] updateTriggers;
    private final ChangeListener updateListener;

    private V value;

    public SelfMappedProperty(String name, Function<V, V> valueGenerator, ChangeListenable... updateTriggers) {
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

    private synchronized void updateValue(boolean triggerUpdate) {
        this.value = valueGenerator.apply(value);

        if (triggerUpdate) {
            fireChangeEvent();
        }
    }

    @Override
    public V get() {
        return value;
    }

    public static <A, V> SelfMappedProperty<V> map(String name,
                                                   Property<A> property,
                                                   BiFunction<V, A, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property == null)
            throw new IllegalArgumentException("property cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new SelfMappedProperty<>(name, previousValue -> {
            A argument = property.get();
            return function.apply(previousValue, argument);
        }, property);
    }

    public static <A, B, V> SelfMappedProperty<V> map(String name,
                                                      Property<A> property1,
                                                      Property<B> property2,
                                                      ThreeFunction<V, A, B, V> function) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (property1 == null)
            throw new IllegalArgumentException("property1 cannot be null");
        if (property2 == null)
            throw new IllegalArgumentException("property2 cannot be null");
        if (function == null)
            throw new IllegalArgumentException("function cannot be null");

        return new SelfMappedProperty<>(name, previousValue -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            return function.apply(previousValue, argument1, argument2);
        }, property1, property2);
    }

    public static <A, B, C, V> SelfMappedProperty<V> map(String name,
                                                         Property<A> property1,
                                                         Property<B> property2,
                                                         Property<C> property3,
                                                         FourFunction<V, A, B, C, V> function) {
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

        return new SelfMappedProperty<>(name, previousValue -> {
            A argument1 = property1.get();
            B argument2 = property2.get();
            C argument3 = property3.get();
            return function.apply(previousValue, argument1, argument2, argument3);
        }, property1, property2, property3);
    }

    public static <V> SelfMappedProperty<V> mapMany(String name, Function<V, V> valueGenerator, ChangeListenable... sources) {
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

        return new SelfMappedProperty<>(name, valueGenerator, sources);
    }
}
