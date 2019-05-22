package net.sothatsit.audiostream.property;

import javax.swing.event.ChangeListener;
import java.util.Objects;

/**
 * An implementation of Property that delegates all calls to another Property.
 *
 * @author Paddy Lamont
 */
public class DelegatedProperty<T> implements Property<T> {

    protected final Property<T> delegate;

    public DelegatedProperty(Property<T> delegate) {
        Objects.requireNonNull(delegate);

        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public void compareAndSet(T expectedValue, T updatedValue) {
        delegate.compareAndSet(expectedValue, updatedValue);
    }

    @Override
    public void set(T value) {
        delegate.set(value);
    }

    @Override
    public T getOrDefault(T defaultValue) {
        return delegate.getOrDefault(defaultValue);
    }

    @Override
    public ReadOnlyProperty<T> readOnly() {
        return delegate.readOnly();
    }

    @Override
    public NonNullProperty<T> nonNull(T nullReplacementValue) {
        return delegate.nonNull(nullReplacementValue);
    }

    @Override
    public void addChangeListener(ChangeListener listener, ChangeListenerProperties properties) {
        delegate.addChangeListener(listener, properties);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        delegate.removeChangeListener(listener);
    }
}
