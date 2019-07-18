package net.sothatsit.property;

import net.sothatsit.function.Unchecked;
import net.sothatsit.property.event.ChangeListenable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Collections;
import java.util.Map;

/**
 * A MapProperty that listens and propogates change events from any ChangeListenable values.
 *
 * @author Paddy Lamont
 */
public class ChangePropagatingMapProperty<K, V> extends MapProperty<K, V> {

    private final ChangeListener propagatingListener;

    public ChangePropagatingMapProperty(String name) {
        this(name, Collections.emptyMap());
    }

    public ChangePropagatingMapProperty(String name, Map<K, V> defaultValue) {
        super(name, defaultValue);

        ChangeListener onMapChangeListener = this::updatePropertyListeners;

        // We don't want propagated events to invoke onMapChangeListener
        propagatingListener = event -> {
            fireChangeEventIgnoreListeners(event, Collections.singleton(onMapChangeListener));
        };

        addChangeListener(onMapChangeListener);
    }

    private void updatePropertyListeners(ChangeEvent event) {
        if (event == null)
            throw new IllegalArgumentException("event shouldn't be null");

        if (event instanceof MapPutChangeEvent) {
            handlePutEvent(Unchecked.cast(event));
            return;
        }

        if (event instanceof MapRemoveChangeEvent) {
            handleRemoveEvent(Unchecked.cast(event));
            return;
        }

        if (event instanceof MapManyChangeEvent) {
            MapManyChangeEvent<K, V> many = Unchecked.cast(event);

            many.getRemovals().forEach(this::handleRemoveEvent);
            many.getAdditions().forEach(this::handlePutEvent);
            return;
        }

        throw new IllegalArgumentException("Unknown ChangeEvent type " + event.getClass());
    }

    private void handlePutEvent(MapPutChangeEvent<K, V> event) {
        V value = event.getValue();
        if (value instanceof ChangeListenable) {
            ((ChangeListenable) value).addWeakChangeListener(propagatingListener);
        }

        V previousValue = event.getPreviousValue();
        if (previousValue instanceof ChangeListenable) {
            ((ChangeListenable) previousValue).removeChangeListener(propagatingListener);
        }
    }

    private void handleRemoveEvent(MapRemoveChangeEvent<K, V> event) {
        V value = event.getValue();
        if (value instanceof ChangeListenable) {
            ((ChangeListenable) value).removeChangeListener(propagatingListener);
        }
    }
}
