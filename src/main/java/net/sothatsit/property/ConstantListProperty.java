package net.sothatsit.property;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A Property whose value is constant.
 *
 * @author Paddy Lamont
 */
public class ConstantListProperty<E> extends ListProperty<E> {

    public ConstantListProperty(String name) {
        this(name, Collections.emptyList());
    }

    public ConstantListProperty(String name, E[] values) {
        super(name, values);
    }

    public ConstantListProperty(String name, List<? extends E> values) {
        super(name, values);
    }

    @Override
    public void compareAndSet(List<E> expectedValue, List<E> updatedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(List<E> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E set(int index, E value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(E value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, E value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAllCountDuplicates(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void permute(List<Integer> indices) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void permute(int[] indices) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
