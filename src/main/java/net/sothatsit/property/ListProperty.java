package net.sothatsit.property;

import javax.swing.event.ChangeEvent;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// TODO : There is still a big question of identity vs. equality
//
// I believe the best way to deal with this is to only fire change events if the list has
// changed in terms of equality, but always change the list regardless to maintain identity.

/**
 * A List implementation that fires change events whenever changes are made to itself.
 *
 * @author Paddy Lamont
 */
public class ListProperty<E> extends ModifiableProperty<List<E>> implements List<E> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ListProperty(String name) {
        this(name, Collections.emptyList());
    }

    public ListProperty(String name, E[] defaultValue) {
        this(name, Arrays.asList(defaultValue));
    }

    public ListProperty(String name, List<? extends E> defaultValue) {
        super(name, new ArrayList<>(defaultValue));
    }

    /**
     * @return A snapshot of this ListProperty at the current point in time.
     */
    @Override
    public List<E> get() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(this));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return this.value.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return this.value.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(Object key) {
        lock.readLock().lock();
        try {
            return this.value.contains(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        lock.readLock().lock();
        try {
            return this.value.containsAll(c);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Object[] toArray() {
        lock.readLock().lock();
        try {
            return this.value.toArray();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] array) {
        lock.readLock().lock();
        try {
            return this.value.toArray(array);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E get(int index) {
        lock.readLock().lock();
        try {
            return this.value.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E set(int index, E value) {
        ListSetChangeEvent<E> change = setImpl(index, value);

        if (change == null)
            return value;

        fireChangeEvent(change);
        return change.previousValue;
    }

    private ListSetChangeEvent<E> setImpl(int index, E value) {
        E previousValue;

        lock.writeLock().lock();
        try {
            previousValue = this.value.set(index, value);
        } finally {
            lock.writeLock().unlock();
        }

        if (previousValue == value)
            return null;

        return new ListSetChangeEvent<>(this, index, previousValue, value);
    }

    @Override
    public void compareAndSet(List<E> expectedValue, List<E> updatedValue) {
        lock.writeLock().lock();
        try {
            if (!equals(expectedValue))
                return;

            set(updatedValue);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void set(List<E> value) {
        // Take a copy of the list so we can protect against concurrent modification
        value = new ArrayList<>(value);
        List<ChangeEvent> changes = new ArrayList<>();

        lock.writeLock().lock();
        try {
            { // 1. Removes all values not in the given list
                List<ListRemoveChangeEvent<E>> removals = retainAllCountDuplicatesImpl(value);
                changes.addAll(removals);
            }

            { // 2. Re-order the remaining values
                List<E> valueNoNewElements;
                { // A. Find the desired list of given values containing no new elements
                    valueNoNewElements = new ArrayList<>(value);
                    retainAllCountDuplicates(valueNoNewElements, this);
                }

                List<Integer> indices;
                { // B. Find the index of all of this Property's elements in valueNoNewElements
                    indices = new ArrayList<>(valueNoNewElements.size());

                    for (E element : this) {
                        int index = 0;
                        for (; index < valueNoNewElements.size(); ++index) {
                            if (Objects.equals(element, valueNoNewElements.get(index)) && !indices.contains(index))
                                break;
                        }

                        // We should be able to find an index for every element
                        if (index == valueNoNewElements.size())
                            throw new IllegalStateException();

                        indices.add(index);
                    }
                }

                { // C. Change the order of elements to match that in the given list
                    ListPermutationChangeEvent<E> permutationEvent = permuteImpl(indices);
                    if (permutationEvent != null) {
                        changes.add(permutationEvent);
                    }
                }
            }

            { // 3. Add in all newly added values
                for (int index = 0; index < value.size(); ++index) {
                    E element = value.get(index);

                    if (index < size() && Objects.equals(element, get(index)))
                        continue;

                    ListAddChangeEvent<E> addEvent;
                    if (index < size()) {
                        addEvent = addImpl(index, element);
                    } else {
                        addEvent = addImpl(element);
                    }

                    if (addEvent == null)
                        throw new IllegalStateException();

                    changes.add(addEvent);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (changes.isEmpty())
            return;

        fireChangeEvent(new ListManyChangeEvent<>(this, changes));
    }

    @Override
    public boolean add(E value) {
        ListAddChangeEvent<E> addition = addImpl(value);

        if (addition != null) {
            fireChangeEvent(addition);
        }

        return addition != null;
    }

    private ListAddChangeEvent<E> addImpl(E value) {
        lock.writeLock().lock();
        try {
            int index = this.value.size();
            this.value.add(value);

            return new ListAddChangeEvent<>(this, index, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void add(int index, E value) {
        ListAddChangeEvent<E> addition = addImpl(value);

        if (addition != null) {
            fireChangeEvent(addition);
        }
    }

    private ListAddChangeEvent<E> addImpl(int index, E value) {
        lock.writeLock().lock();
        try {
            this.value.add(index, value);

            return new ListAddChangeEvent<>(this, index, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Object value) {
        ListRemoveChangeEvent<E> removal = removeImpl(value);

        if (removal != null) {
            fireChangeEvent(removal);
        }

        return removal != null;
    }

    public ListRemoveChangeEvent<E> removeImpl(Object value) {
        lock.writeLock().lock();
        try {
            int index = this.value.indexOf(value);
            if (index < 0)
                return null;

            E removed = this.value.remove(index);
            return new ListRemoveChangeEvent<>(this, index, removed);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E remove(int index) {
        ListRemoveChangeEvent<E> removal = removeImpl(value);

        fireChangeEvent(removal);

        return removal.getValue();
    }

    public ListRemoveChangeEvent<E> removeImpl(int index) {
        lock.writeLock().lock();
        try {
            E removed = this.value.remove(index);
            return new ListRemoveChangeEvent<>(this, index, removed);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        ListManyChangeEvent<E> changes = addAllImpl(collection);

        if (changes != null) {
            fireChangeEvent(changes);
        }

        return changes != null;
    }

    private ListManyChangeEvent<E> addAllImpl(Collection<? extends E> collection) {
        if (collection == null)
            throw new IllegalArgumentException("collection cannot be null");

        List<ChangeEvent> additions = new ArrayList<>();

        lock.writeLock().lock();
        try {
            for (E value : collection) {
                ListAddChangeEvent<E> addition = addImpl(value);
                if (addition == null)
                    continue;

                additions.add(addition);
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (additions.isEmpty())
            return null;

        return new ListManyChangeEvent<>(this, additions);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        ListManyChangeEvent<E> changes = addAllImpl(index, collection);

        if (changes != null) {
            fireChangeEvent(changes);
        }

        return changes != null;
    }

    private ListManyChangeEvent<E> addAllImpl(int index, Collection<? extends E> collection) {
        if (collection == null)
            throw new IllegalArgumentException("collection cannot be null");

        List<ChangeEvent> additions = new ArrayList<>();

        lock.writeLock().lock();
        try {
            for (E value : reversed(collection)) {
                ListAddChangeEvent<E> addition = addImpl(index, value);
                if (addition == null)
                    continue;

                additions.add(addition);
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (additions.isEmpty())
            return null;

        return new ListManyChangeEvent<>(this, additions);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        ListManyChangeEvent<E> changes = removeAllImpl(collection);

        if (changes != null) {
            fireChangeEvent(changes);
        }

        return changes != null;
    }

    private ListManyChangeEvent<E> removeAllImpl(Collection<?> collection) {
        if (collection == null)
            throw new IllegalArgumentException("collection cannot be null");

        List<ChangeEvent> removals = new ArrayList<>();

        lock.writeLock().lock();
        try {
            for (Object value : reversed(collection)) {
                ListRemoveChangeEvent<E> removal = removeImpl(value);
                if (removal == null)
                    continue;

                removals.add(removal);
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (removals.isEmpty())
            return null;

        return new ListManyChangeEvent<>(this, removals);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        List<ListRemoveChangeEvent<E>> changes = retainAllImpl(collection);

        if (!changes.isEmpty()) {
            fireChangeEvent(new ListManyChangeEvent<>(this, changes));
        }

        return !changes.isEmpty();
    }

    private List<ListRemoveChangeEvent<E>> retainAllImpl(Collection<?> collection) {
        if (collection == null)
            throw new IllegalArgumentException("collection cannot be null");

        lock.writeLock().lock();
        try {
            List<ListRemoveChangeEvent<E>> removals = new ArrayList<>();
            int index = 0;
            while (index < size()) {
                E element = get(index);

                if (!collection.contains(element)) {
                    ListRemoveChangeEvent<E> removal = removeImpl(index);
                    if (removal == null)
                        throw new IllegalStateException();

                    removals.add(removal);
                    continue;
                }

                index += 1;
            }

            return removals;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean retainAllCountDuplicates(Collection<?> collection) {
        List<ListRemoveChangeEvent<E>> changes = retainAllCountDuplicatesImpl(collection);

        if (!changes.isEmpty()) {
            fireChangeEvent(new ListManyChangeEvent<>(this, changes));
        }

        return !changes.isEmpty();
    }

    private List<ListRemoveChangeEvent<E>> retainAllCountDuplicatesImpl(Collection<?> collection) {
        if (collection == null)
            throw new IllegalArgumentException("collection cannot be null");

        // Copy the collection so we can modify it
        List<?> valuesRemainingToRetain = new ArrayList<>(collection);

        List<ListRemoveChangeEvent<E>> removals = new ArrayList<>();

        lock.writeLock().lock();
        try {
            int index = 0;
            while (index < size()) {
                E element = get(index);

                // Identity removal of element
                boolean removed = false;
                for (int removeIndex = 0; removeIndex < valuesRemainingToRetain.size(); ++removeIndex) {
                    if (element != valuesRemainingToRetain.get(removeIndex))
                        continue;

                    valuesRemainingToRetain.remove(removeIndex);
                    removed = true;
                    break;
                }

                if (!removed) {
                    ListRemoveChangeEvent<E> removal = removeImpl(index);
                    if (removal == null)
                        throw new IllegalStateException();

                    removals.add(removal);
                    continue;
                }

                index += 1;
            }
        } finally {
            lock.writeLock().unlock();
        }

        return removals;
    }

    public void permute(List<Integer> indices) {
        permute(indices.stream().mapToInt(i -> i).toArray());
    }

    public void permute(int[] indices) {
        ListPermutationChangeEvent<E> event = permuteImpl(indices);

        if (event != null) {
            fireChangeEvent(event);
        }
    }

    private ListPermutationChangeEvent<E> permuteImpl(List<Integer> indices) {
        return permuteImpl(indices.stream().mapToInt(i -> i).toArray());
    }

    private ListPermutationChangeEvent<E> permuteImpl(int[] indices) {
        lock.writeLock().lock();
        try {
            if (indices.length != size())
                throw new IllegalArgumentException("indices must be an array containing all indices of this list");

            int[] indicesSorted = Arrays.copyOf(indices, indices.length);
            Arrays.sort(indicesSorted);

            boolean changesOrder = false;

            for (int index = 0; index < indices.length; ++index) {
                if(indicesSorted[index] != index)
                    throw new IllegalArgumentException("indices must be an array containing all indices of this list");

                if (indices[index] != index) {
                    changesOrder = true;
                }
            }

            if (!changesOrder)
                return null;

            List<E> valuesCopy = new ArrayList<>(this.value);
            for (int index = 0; index < indices.length; ++index) {
                E newValue = valuesCopy.get(indices[index]);

                this.value.set(index, newValue);
            }

            return new ListPermutationChangeEvent<>(this, indices);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        ListManyChangeEvent<E> cleared = clearImpl();

        if (cleared != null) {
            fireChangeEvent(cleared);
        }
    }

    private ListManyChangeEvent<E> clearImpl() {
        lock.writeLock().lock();
        try {
            List<ChangeEvent> removals = new ArrayList<>();

            for (int index = size() - 1; index >= 0; --index) {
                ListRemoveChangeEvent<E> removal = removeImpl(index);
                if (removal == null)
                    throw new IllegalStateException();

                removals.add(removal);
            }

            return new ListManyChangeEvent<>(this, removals);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int indexOf(Object value) {
        lock.readLock().lock();
        try {
            return this.value.indexOf(value);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int lastIndexOf(Object value) {
        lock.readLock().lock();
        try {
            return this.value.lastIndexOf(value);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return get().iterator();
    }

    @Override
    public ListIterator<E> listIterator() {
        return get().listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return get().listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return get().subList(fromIndex, toIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        return get().equals(obj);
    }

    public static class ListAddChangeEvent<E> extends ChangeEvent {

        private final int index;
        private final E value;

        public ListAddChangeEvent(Object source, int index, E value) {
            super(source);

            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public E getValue() {
            return value;
        }
    }

    public static class ListRemoveChangeEvent<E> extends ChangeEvent {

        private final int index;
        private final E value;

        public ListRemoveChangeEvent(Object source, int index, E value) {
            super(source);

            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public E getValue() {
            return value;
        }
    }

    public static class ListPermutationChangeEvent<E> extends ChangeEvent {

        private final int[] newIndices;

        public ListPermutationChangeEvent(Object source, int[] newIndices) {
            super(source);

            this.newIndices = newIndices;
        }

        public int[] getNewIndices() {
            return newIndices;
        }
    }

    public static class ListSetChangeEvent<E> extends ListManyChangeEvent<E> {

        private final int index;
        private final E value;
        private final E previousValue;

        public ListSetChangeEvent(Object source, int index, E previousValue, E value) {
            super(source, Arrays.asList(
                    new ListRemoveChangeEvent<>(source, index, previousValue),
                    new ListAddChangeEvent<>(source, index, value)
            ));

            this.index = index;
            this.value = value;
            this.previousValue = previousValue;
        }

        public int getIndex() {
            return index;
        }

        public E getValue() {
            return value;
        }

        public E getPreviousValue() {
            return previousValue;
        }
    }

    public static class ListManyChangeEvent<E> extends ChangeEvent {

        private final List<? extends ChangeEvent> changes;

        public ListManyChangeEvent(Object source, List<? extends ChangeEvent> changes) {
            super(source);

            this.changes = changes;
        }


        public List<? extends ChangeEvent> getChanges() {
            return changes;
        }

        @SafeVarargs
        private static <E> ListManyChangeEvent<E> combine(ListManyChangeEvent<E>... events) {
            Object source = null;
            List<ChangeEvent> changes = new ArrayList<>();

            for (ListManyChangeEvent<E> event : events) {
                if (event == null)
                    continue;

                changes.addAll(event.getChanges());

                if (source == null) {
                    source = event.getSource();
                } else if (source != event.getSource()) {
                    throw new IllegalArgumentException("events have different sources");
                }
            }

            if (source == null || changes.isEmpty())
                return null;

            return new ListManyChangeEvent<>(source, changes);
        }
    }

    private static <E> List<E> reversed(Iterable<E> iterable) {
        List<E> reversed = new ArrayList<>();
        for (E value : iterable) {
            reversed.add(0, value);
        }
        return reversed;
    }

    private static <E> void retainAllCountDuplicates(List<E> list, Collection<E> collection) {
        // Copy the collection so we can modify it
        collection = new ArrayList<>(collection);

        int index = 0;
        while (index < list.size()) {
            E element = list.get(index);

            if (!collection.remove(element)) {
                list.remove(index);
                continue;
            }

            index += 1;
        }
    }

    public static <E> ConstantListProperty<E> constant(String name, E[] values) {
        return new ConstantListProperty<>(name, values);
    }

    public static <E> ConstantListProperty<E> constant(String name, List<E> values) {
        return new ConstantListProperty<>(name, values);
    }
}
