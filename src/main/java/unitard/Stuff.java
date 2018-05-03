package unitard;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Recursive immutable data structure, which imitates objects in unityped languages.
 * It is intended to bring principled semantics to a situation where a loose,
 * dynamic structure might otherwise be used.
 *
 * Stuff can be either:
 *   - An atomic value (any Java object or null).
 *   - A mapping of unique keys (any Java object or null) to values (Stuff)
 *   - A possibly empty list containing Stuff.
 */
public abstract class Stuff implements Iterable<Entry> {

    public static final Stuff EMPTY_MAP = new MapStuff();
    public static final Stuff EMPTY_LIST = new ListStuff();

    public static Stuff of(Map<?, ?> map) {
        return new MapStuff(getEntriesFromMap(map));
    }

    public static Stuff of(List<?> list) {
        return new ListStuff(copyIntoImmutableList(list.stream()));
    }

    public static Stuff listOf(Object... elements) {
        return new ListStuff(Arrays.asList(elements));
    }

    public static Stuff mapOf(Object key, Object value) {
        return EMPTY_MAP.put(key, value);
    }

    protected abstract Hopefully<Object> getHere(Object key);

    public abstract Stuff remove(Object key);

    public abstract Stuff put(Object key, Object value);

    public abstract Stuff add(Object e);

    public abstract Stuff insert(int index, Object value);

    public abstract int size();


    public Hopefully<Object> get(Object key, Object... keys) {
        return at(key, keys).get();
    }

    public Focus at(Object key, Object... keys) {
        Focus f = new FocusImpl(Optional.empty(), getHere(key), key);
        return matchArray(keys, f::at, () -> f);
    }

    private class FocusImpl implements EditFocus {
        final Optional<Focus> parent;
        final Hopefully<Object> target;
        final Object pathSegment;

        FocusImpl(Optional<Focus> parent, Hopefully<Object> target, Object pathSegment) {
            this.parent = parent;
            this.pathSegment = pathSegment;
            this.target = target;
        }

        public Hopefully<Object> get() {
            return target;
        }

        public EditFocus at(Object key, Object... keys) {
            EditFocus f = new FocusImpl(Optional.of(this), get1(key), key);
            for (Object k : keys) {
                f = f.at(k);
            }
            return f;
        }

        public EditFocus put(Object key, Object value) {
            return refocus(s -> s.put(key, value));
        }

        public EditFocus remove(Object key) {
            return refocus(s -> s.remove(key));
        }

        public EditFocus add(Object e) {
            return refocus(s -> s.add(e));
        }

        public EditFocus insert(int index, Object e) {
            return refocus(s -> s.insert(index, e));
        }

        private EditFocus refocus(Function<Stuff, Stuff> f) {
            return new FocusImpl(
                    parent,
                    target.asStuff().map(f::apply),
                    pathSegment);
        }

        public Path getPath() {
            return parent
                    .map(Focus::getPath)
                    .orElseGet(() -> Path.EMPTY)
                    .dot(pathSegment);
        }

        public Hopefully<Stuff> hopefullyDone() {
            if (parent.isPresent()) {
                return target.flatMap(t -> parent.get().put(pathSegment, t).hopefullyDone());
            } else {
                return target.map(t -> Stuff.this.put(pathSegment, t));
            }
        }

        public Stuff done() {
            return hopefullyDone().getOrElse(() -> Stuff.this);
        }

        public String toString() {
            return "Focus at " + getPath();
        }

        private Hopefully<Object> get1(Object key) {
            return target.asStuff()
                    .flatMap(s -> s.get(key))
                    .withPath(getPath().dot(key));

        }
    }

    public abstract <K, V> Hopefully<Map<K, V>> asMapOf(Class<K> keyType, Class<V> valueType);

    public abstract <E> Hopefully<List<E>> asListOf(Class<E> elementType);

    public abstract boolean isEmpty();

    public abstract Iterator<Object> getKeys();

    private static class MapStuff extends Stuff {

        private final Map<Object, Object> contents;

        MapStuff() {
            this(new HashMap<>());
        }

        private MapStuff(Map<Object,Object> contents) {
            this.contents = contents;
        }

        private MapStuff copy(Consumer<Map<Object,Object>> f) {
            Map<Object,Object> newMap = new HashMap<>(contents);
            f.accept(newMap);
            return new MapStuff(newMap);
        }

        public Iterator<Object> getKeys() {
            return contents.keySet().stream().iterator();
        }

        private MapStuff(Stream<Entry> entries) {
            this.contents = new HashMap<>();

            for (Iterator<Entry> i = entries.iterator(); i.hasNext();) {
                Entry e = i.next();
                contents.put(e.getKey(), e.getValue());
            }
        }

        protected Hopefully<Object> getHere(Object key) {
            return Hopefully.getFromMap(contents, key);
        }

        public Stuff remove(Object key) {
            return copy(newMap -> newMap.remove(key));
        }

        public Stuff put(Object key, Object value) {
            return copy(newMap -> newMap.put(key, value));
        }

        public Stuff add(Object e) {
            return this;
        }

        public Stuff insert(int index, Object value) {
            return this;
        }

        public int size() {
            return contents.size();
        }

        public <K, V> Hopefully<Map<K, V>> asMapOf(Class<K> keyType, Class<V> valueType) {
            return Hopefully.notNull(contents).asMapOf(keyType, valueType);
        }

        public boolean isEmpty() {
            return contents.isEmpty();
        }

        public <E> Hopefully<List<E>> asListOf(Class<E> elementType) {
            return Hopefully.notNull(contents).asListOf(elementType);
        }

        @Override
        public Iterator<Entry> iterator() {
            return new Iterator<Entry>() {
                Iterator<Map.Entry<Object, Object>> it = contents.entrySet().iterator();

                public boolean hasNext() {
                    return it.hasNext();
                }

                public Entry next() {
                    return Entry.fromMapEntry(it.next());
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o != null && o instanceof MapStuff) {
                MapStuff ms = (MapStuff)o;
                return ms.contents.equals(contents);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(contents);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');

            boolean first = true;
            for (Map.Entry<?,?> entry : contents.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
                first = false;
            }

            sb.append('}');
            return sb.toString();
        }
    }

    private static class ListStuff extends Stuff {
        private final List<Object> contents;

        private ListStuff() {
            this(new ArrayList<>());
        }

        private ListStuff(List<Object> contents) {
            this.contents = contents;
        }

        private ListStuff(Stream<Object> elements) {
            this.contents = elements.collect(Collectors.toList());
        }

        private ListStuff copy(Consumer<List<Object>> f) {
            List<Object> newContents = new ArrayList<>(contents);
            f.accept(newContents);
            return new ListStuff(newContents);
        }

        protected Hopefully<Object> getHere(Object key) {
            if (key instanceof Integer) {
                return Hopefully.getFromList(contents, (Integer) key);
            } else {
                return Hopefully.missing().withPath(Path.of(key));
            }
        }

        public Iterator<Object> getKeys() {
            return Stream.iterate((Object)0, n -> ((Integer)n)+1).iterator();
        }

        public Stuff put(Object key, Object value) {
            if (isValidIndex(key)) {
                return copy(newList -> newList.set((int)key, value));
            }
            return this;
        }

        public Stuff remove(Object key) {
            if (isValidIndex(key)) {
                return copy(newList -> newList.remove((int)key));
            }
            return this;
        }

        public Stuff add(Object e) {
            return copy(newList -> newList.add(e));
        }

        public Stuff insert(int index, Object value) {
            if (isValidIndexInclusive(index)) {
                return copy(newList -> newList.add((int)index, value));
            }
            return this;
        }

        public int size() {
            return contents.size();
        }

        public boolean isEmpty() {
            return contents.isEmpty();
        }

        public <K, V> Hopefully<Map<K, V>> asMapOf(Class<K> keyType, Class<V> valueType) {
            return Hopefully.notNull(new ArrayList<>(contents)).asMapOf(keyType, valueType);
        }

        public <E> Hopefully<List<E>> asListOf(Class<E> elementType) {
            return Hopefully.notNull(new ArrayList<>(contents)).asListOf(elementType);
        }

        @Override
        public Iterator<Entry> iterator() {
            return new Iterator<Entry>() {
                int i = 0;
                Iterator<Object> it = contents.iterator();

                public boolean hasNext() {
                    return it.hasNext();
                }

                public Entry next() {
                    return new Entry(i++, it.next());
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o != null && o instanceof ListStuff) {
                ListStuff ms = (ListStuff)o;
                return ms.contents.equals(contents);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return contents.hashCode();
        }

        @Override
        public String toString() {
            return contents.toString();
        }

        private boolean isValidIndex(Object o) {
            if (o instanceof Integer) {
                int i = (Integer)o;
                return i >= 0 && i < contents.size();
            }
            return false;
        }

        private boolean isValidIndexInclusive(Object o) {
            if (o instanceof Integer) {
                int i = (Integer)o;
                return i >= 0 && i <= contents.size();
            }
            return false;
        }
    }

    private static Stream<Entry> getEntriesFromMap(Map<?, ?> map) {
        return map.entrySet().stream().map(Entry::fromMapEntry);
    }

    private static Object copyIfNecessary(Object candidate) {
        Optional<Object> a = describeIfMap(candidate).map(Stuff::copyIntoImmutableMap);
        Optional<Object> b = describeIfList(candidate).map(Stuff::copyIntoImmutableList);
        return a.orElse(b.orElse(candidate));
    }

    private static Map<Object, Object> copyIntoImmutableMap(Stream<Entry> oldEntries) {
        Map<Object,Object> map = new HashMap<>();
        for (Iterator<Entry> i = oldEntries.iterator(); i.hasNext();) {
            Entry e = i.next();
            map.put(e.getKey(), copyIfNecessary(e.getValue()));
        }
        return map;
    }

    private static List<Object> copyIntoImmutableList(Stream<?> oldElements) {
        return new ArrayList<>(oldElements
                .map(Stuff::copyIfNecessary)
                .collect(Collectors.toList()));
    }

    private static Optional<Stream<Object>> describeIfList(Object root) {
        return Hopefully.notNull(root)
                .asListOf(Object.class)
                .map(List::stream)
                .toOptional();
    }

    private static Optional<Stream<Entry>> describeIfMap(Object root) {
        return Hopefully.notNull(root)
                .as(Map.class)
                .map(Stuff::getEntriesFromMap).toOptional();
    }

    private static <X> X matchArray(Object[] array, BiFunction<Object, Object[], X> ifNonEmpty, Supplier<X> ifEmpty) {
        if (array.length == 0) {
            return ifEmpty.get();
        } else {
            Object head = array[0];
            int tailLength = array.length - 1;
            Object[] tail = new Object[tailLength];
            System.arraycopy(array, 1, tail, 0, tailLength);
            return ifNonEmpty.apply(head, tail);
        }
    }


    private Stuff() {}
}
