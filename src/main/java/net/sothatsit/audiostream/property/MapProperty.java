package net.sothatsit.audiostream.property;

import net.sothatsit.audiostream.util.Unchecked;

import javax.swing.event.ChangeEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Map implementation that fires change events whenever changes are made to itself.
 *
 * This class provides no guarantees for the atomicity of its operations.
 *
 * @author Paddy Lamont
 */
public class MapProperty<K, V> extends ModifiableProperty<Map<K, V>> implements Map<K, V> {

    public MapProperty(String name) {
        this(name, Collections.emptyMap());
    }

    public MapProperty(String name, Map<K, V> defaultValue) {
        super(name, new ConcurrentHashMap<>(defaultValue));
    }

    @Override
    public Map<K, V> get() {
        return Collections.unmodifiableMap(super.get());
    }

    @Override
    public void set(Map<K, V> value) {
        List<MapRemoveChangeEvent<K, V>> removals = clearImpl();
        List<MapPutChangeEvent<K, V>> additions = putAllImpl(value);

        if (removals.size() > 0 || additions.size() > 0) {
            fireChangeEvent(new MapManyChangeEvent<>(this, removals, additions));
        }
    }

    @Override
    public int size() {
        return this.value.size();
    }

    @Override
    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.value.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.value.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return this.value.get(key);
    }

    @Override
    public V put(K key, V value) {
        MapPutChangeEvent<K, V> change = putImpl(key, value);

        if (change.isActualChange()) {
            fireChangeEvent(change);
        }

        return change.previousValue;
    }

    private MapPutChangeEvent<K, V> putImpl(K key, V value) {
        if (value == null)
            throw new IllegalArgumentException("MapProperty does not support null values");

        V previousValue = this.value.put(key, value);
        return new MapPutChangeEvent<>(this, key, value, previousValue);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> entryMap) {
        List<MapPutChangeEvent<K, V>> additions = putAllImpl(entryMap);

        if (additions.size() > 0) {
            fireChangeEvent(new MapManyChangeEvent<>(this, Collections.emptyList(), additions));
        }
    }

    private List<MapPutChangeEvent<K, V>> putAllImpl(Map<? extends K, ? extends V> entryMap) {
        if (entryMap == null)
            throw new IllegalArgumentException("entryMap cannot be null");

        List<MapPutChangeEvent<K, V>> additions = new ArrayList<>();

        for (Map.Entry<? extends K, ? extends V> entry : entryMap.entrySet()) {
            MapPutChangeEvent<K, V> change = putImpl(entry.getKey(), entry.getValue());

            if (!change.isActualChange())
                continue;

            additions.add(change);
        }

        return additions;
    }

    @Override
    public V remove(Object key) {
        MapRemoveChangeEvent<K, V> change = removeImpl(Unchecked.cast(key));

        if (change.isActualChange()) {
            fireChangeEvent(change);
        }

        return change.value;
    }

    private MapRemoveChangeEvent<K, V> removeImpl(K key) {
        V value = this.value.remove(key);
        return new MapRemoveChangeEvent<>(this, key, value);
    }

    @Override
    public void clear() {
        List<MapRemoveChangeEvent<K, V>> removals = clearImpl();

        if (removals.size() > 0) {
            fireChangeEvent(new MapManyChangeEvent<>(this, removals, Collections.emptyList()));
        }
    }

    public List<MapRemoveChangeEvent<K, V>> clearImpl() {
        List<MapRemoveChangeEvent<K, V>> removals = new ArrayList<>();

        for (K key : keySet()) {
            MapRemoveChangeEvent<K, V> change = removeImpl(key);

            if (!change.isActualChange())
                continue;

            removals.add(change);
        }

        return removals;
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(this.value.keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(this.value.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(this.value.entrySet());
    }

    public static class MapPutChangeEvent<K, V> extends ChangeEvent {

        private final K key;
        private final V value;
        private final V previousValue;

        public MapPutChangeEvent(Object source, K key, V value, V previousValue) {
            super(source);

            this.key = key;
            this.value = value;
            this.previousValue = previousValue;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public boolean hasPreviousValue() {
            return previousValue != null;
        }

        public V getPreviousValue() {
            return previousValue;
        }

        private boolean isActualChange() {
            return value != previousValue;
        }
    }

    public static class MapRemoveChangeEvent<K, V> extends ChangeEvent {

        private final K key;
        private final V value;

        public MapRemoveChangeEvent(Object source, K key, V value) {
            super(source);

            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        private boolean isActualChange() {
            return value != null;
        }
    }

    public static class MapManyChangeEvent<K, V> extends ChangeEvent {

        private final List<MapRemoveChangeEvent<K, V>> removals;
        private final List<MapPutChangeEvent<K, V>> additions;

        public MapManyChangeEvent(Object source,
                                  List<MapRemoveChangeEvent<K, V>> removals,
                                  List<MapPutChangeEvent<K, V>> additions) {

            super(source);

            if (removals.stream().anyMatch(event -> !event.isActualChange()))
                throw new IllegalArgumentException("At least one RemovalChangeEvent is not an actual change");
            if (additions.stream().anyMatch(event -> !event.isActualChange()))
                throw new IllegalArgumentException("At least one MapPutChangeEvent is not an actual change");

            this.removals = removals;
            this.additions = additions;
        }

        public List<MapRemoveChangeEvent<K, V>> getRemovals() {
            return removals;
        }

        public List<MapPutChangeEvent<K, V>> getAdditions() {
            return additions;
        }
    }
}
