package net.sothatsit.audiostream.property;

/**
 * A wrapper around Property that stops itself being modified.
 *
 * @author Paddy Lamont
 */
public class ReadOnlyProperty<T> extends DelegatedProperty<T> {

    public ReadOnlyProperty(Property<T> delegate) {
        super(delegate);
    }

    @Override
    public void compareAndSet(T expectedValue, T updatedValue) {
        throw new UnsupportedOperationException("This Property is read-only.");
    }

    @Override
    public void set(T value) {
        throw new UnsupportedOperationException("This Property is read-only.");
    }
}
