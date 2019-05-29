package net.sothatsit.property;

/**
 * A Property whose value is constant.
 *
 * @author Paddy Lamont
 */
public class ConstantProperty<T> extends AbstractProperty<T> {

    private final T value;

    public ConstantProperty(String name, T value) {
        super(name);

        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }
}
