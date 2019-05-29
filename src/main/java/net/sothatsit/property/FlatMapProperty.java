package net.sothatsit.property;

import javax.swing.event.ChangeListener;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A Property whose value is taken from a nested property.
 * i.e. Property<Property<T>>.
 *
 * @author Paddy Lamont
 */
public class FlatMapProperty<T> extends AbstractProperty<T> {

    private final ChangeListener valueChangePropagator = this::fireChangeEvent;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Property<Property<T>> property;
    private Property<T> currentNestedProperty;

    public FlatMapProperty(String name, Property<Property<T>> property) {
        super(name);

        this.property = property;

        property.addChangeListener(event -> updateNestedProperty(true));
        updateNestedProperty(false);
    }

    private void updateNestedProperty(boolean triggerUpdate) {
        Property<T> newNestedProperty = property.get();
        if (newNestedProperty == currentNestedProperty)
            return;

        lock.writeLock().lock();
        try {
            if (newNestedProperty != null) {
                newNestedProperty.addChangeListener(valueChangePropagator);
            }

            if (currentNestedProperty != null) {
                currentNestedProperty.removeChangeListener(valueChangePropagator);
            }

            currentNestedProperty = newNestedProperty;
        } finally {
            lock.writeLock().unlock();
        }

        if (triggerUpdate) {
            fireChangeEvent();
        }
    }

    @Override
    public T get() {
        lock.readLock().lock();
        try {
            if (currentNestedProperty == null)
                return null;

            return currentNestedProperty.get();
        } finally {
            lock.readLock().unlock();
        }
    }
}
