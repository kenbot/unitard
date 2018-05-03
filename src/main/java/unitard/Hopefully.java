package unitard;

import java.util.*;
import java.util.function.*;

/**
 * Possibly a real value of the expected type, but otherwise describes the various 
 * failure modes one can encounter rummaging around in Stuff.
 *
 * Whether representing success or failure, it carries information about the path
 * withPath which the result was obtained.
 */
@SuppressWarnings("unchecked")
public abstract class Hopefully<A> {

    public static <A> Hopefully<A> nullValue()  {
        return notNull(null);
    }

    public static <A> Hopefully<A> notNull(A a) {
        return a == null ? new Null(Path.EMPTY) : new ActualValue(Path.EMPTY, a);
    }

    public static <A> Hopefully<A> missing() {
        return new Missing(Path.EMPTY);
    }

    public static <K,V> Hopefully<V> getFromMap(Map<K,V> map, K key) {
        Path path = Path.of(key);
        if (map.containsKey(key)) {
            return notNull(map.get(key)).withPath(path);
        } else {
            return Hopefully.<V>missing().withPath(path);
        }
    }

    public static <E> Hopefully<E> getFromList(List<E> list, int index) {
        Path path = Path.of(index);
        if (index >= 0 && index < list.size()) {
            return notNull(list.get(index)).withPath(path);
        } else {
            return Hopefully.<E>missing().withPath(path);
        }
    }


    public abstract <B> B fold(
            Function<Path, Function<A,B>> onActualValue,
            Function<Path,B> onNull,
            Function<Path,B> onMissing,
            Function<Path, Function<Class<?>, Function<Class<?>, B>>> onWrongType);

    public final void foldVoid(
            Function<Path, Consumer<A>> onActualValue,
            Consumer<Path> onNull,
            Consumer<Path> onMissing,
            Function<Path, Function<Class<?>, Consumer<Class<?>>>> onWrongType) {

        this.<Void>fold(
             path -> a -> { onActualValue.apply(path).accept(a); return null; },
             path -> { onNull.accept(path); return null; },
             path -> { onMissing.accept(path); return null; },
             path -> found -> expected -> { onWrongType.apply(path).apply(found).accept(expected); return null; }
        );
    }

    public final <B> B mapOrElse(Function<A,B> onActualValue, Supplier<B> onFail) {
        return fold(
                path -> onActualValue::apply,
                path -> onFail.get(),
                path -> onFail.get(),
                path -> c1 -> c2 -> onFail.get());
    }

    public final <B> Hopefully<B> map(Function<A,B> f) {
        return flatMap(a -> notNull(f.apply(a)));
    }

    public final <B> Hopefully<B> flatMap(Function<A, Hopefully<B>> f) {
        return fold(
                path -> a -> f.apply(a).withPath(path),
                Null::new,
                Missing::new,
                path -> c1 -> c2 -> new WrongType(path, c1, c2));
    }

    public final A getOrElse(Supplier<A> elseValue) {
        return mapOrElse(a -> a, elseValue);
    }

    public final Hopefully<A> orElse(Supplier<Hopefully<A>> next) {
        return mapOrElse(a -> this, next);
    }

    public Path getPath() {
        return fold(
                p -> a -> p,
                p -> p,
                p -> p,
                p -> c1 -> c2 -> p);

    }

    public final <B> Hopefully<B> as(Class<B> expected) {
        return flatMap(a -> {
            try {
                return new ActualValue(getPath(), expected.cast(a));
            } catch (ClassCastException e) {
                return new WrongType(getPath(), a.getClass(), expected);
            }
        });
    }

    public Hopefully<A> withPath(Path newPath) {
        return fold(
                p -> a -> new ActualValue(newPath, a),
                p -> new Null(newPath),
                p -> new Missing(newPath),
                p -> c1 -> c2 -> new WrongType(newPath, c1, c2));
    }

    public Hopefully<Stuff> asStuff() {
        return as(Stuff.class)
                .orElse(() -> as(Map.class).map(Stuff::of))
                .orElse(() -> as(List.class).map(Stuff::of));
    }

    public boolean isTrue() {
        return as(Boolean.class).getOrElse(() -> false);
    }

    public Optional<A> toOptional() {
        return (Optional<A>) mapOrElse(Optional::of, Optional::empty);
    }

    public final void ifActualValue(Consumer<A> f) {
        foldVoid(
                p -> f::accept,
                p -> {},
                p -> {},
                p -> c1 -> c2 -> {});
    }

    public final boolean isActualValue() {
        return mapOrElse(a -> true, () -> false);
    }

    public final boolean isMissing() {
        return fold(
                p -> a -> false,
                p -> false,
                p -> true,
                p -> c1 -> c2 -> false);
    }

    public final boolean isWrongType() {
        return fold(
                p -> a -> false,
                p -> false,
                p -> false,
                p -> c1 -> c2 -> true);
    }

    public final boolean isNull() {
        return fold(
                p -> a -> false,
                p -> true,
                p -> false,
                p -> c1 -> c2 -> false);
    }

    public A unsafeGet() throws IllegalStateException {
        return fold(
                p -> a -> a,
                p -> null,
                p -> {
                    String msg = "No result found" + atPath();
                    throw new NoSuchElementException(msg);
                },
                p -> c1 -> c2 -> {
                    String msg = "Expected: " + c2 + " Found: " + c1 + atPath();
                    throw new NoSuchElementException(msg);
                });
    }

    protected String atPath() {
        return (getPath().isEmpty() ? "" : " at " + getPath());
    }

    public static <A> Hopefully<List<A>> sequence(List<Hopefully<A>> hopefullies) {
        List<A> result = new ArrayList<>();
        for (Hopefully<A> ha : hopefullies) {
            if (!ha.isActualValue()) {
                return (Hopefully<List<A>>)ha;
            } else {
                ha.ifActualValue(result::add);
            }
        }
        return notNull(result);
    }

    public final <B> Hopefully<List<B>> asListOf(Class<B> elementType) {
        return as(List.class).flatMap(list -> {
            for (Object o : list) {
                if (o != null && !elementType.isAssignableFrom(o.getClass())) {
                    return new WrongType(Path.EMPTY, o.getClass(), elementType);
                }
            }

            return notNull((List<B>) list);
        });
    }

    public final <K,V> Hopefully<Map<K,V>> asMapOf(Class<K> keyType, Class<V> valueType) {
        return as(Map.class).flatMap(map -> {
            for (Map.Entry<?,?> e : ((Map<?,?>)map).entrySet()) {
                Class<?> foundKeyType = e.getKey().getClass();
                if (!keyType.isAssignableFrom(foundKeyType)) {
                    return new WrongType(Path.EMPTY, foundKeyType, keyType);
                }

                Class foundValueType = e.getKey().getClass();
                if (!valueType.isAssignableFrom(foundValueType)) {
                    return new WrongType(Path.EMPTY, foundValueType, valueType);
                }
            }

            return notNull((Map<K,V>)map);
        });
    }

    private final static class ActualValue<A> extends Hopefully<A> {
        private final A value;
        private final Path path;

        public ActualValue(Path path, A value) {
            this.value = value;
            this.path = path;
        }

        public <B> B fold(
                Function<Path, Function<A,B>> onActualValue,
                Function<Path, B> onNull,
                Function<Path, B> onMissing,
                Function<Path, Function<Class<?>, Function<Class<?>, B>>> onWrongType) {

            return onActualValue.apply(path).apply(value);
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof ActualValue) {
                ActualValue av = (ActualValue)o;
                return av.value.equals(value) &&
                       av.path.equals(path);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, path);
        }

        @Override
        public String toString() {
            return "ActualValue(" + value + ")" + atPath();
        }
    }

    private final static class Null<A> extends Hopefully<A> {
        private final Path path;

        private Null(Path path) {
            this.path = path;
        }

        @Override
        public <B> B fold(
                Function<Path, Function<A,B>> onActualValue,
                Function<Path, B> onNull,
                Function<Path, B> onMissing,
                Function<Path, Function<Class<?>, Function<Class<?>, B>>> onWrongType) {

            return onNull.apply(path);
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof Null) {
                Null n = (Null)o;
                return n.path.equals(path);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public String toString() {
            return "Null" + atPath();
        }
    }

    private final static class Missing<A> extends Hopefully<A> {
        private final Path path;

        public Missing(Path path) {
            this.path = path;
        }

        public <B> B fold(
                Function<Path, Function<A,B>> onActualValue,
                Function<Path, B> onNull,
                Function<Path, B> onMissing,
                Function<Path, Function<Class<?>, Function<Class<?>, B>>> onWrongType) {

            return onMissing.apply(path);
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof Missing) {
                Missing m = (Missing)o;
                return m.path.equals(path);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public String toString() {
            return "Missing" + atPath();
        }
    }

    private final static class WrongType<A> extends Hopefully<A> {
        private final Class<?> found, expected;
        private final Path path;

        private WrongType(Path path, Class<?> found, Class<?> expected) {
            this.found = found;
            this.expected = expected;
            this.path = path;
        }

        public <B> B fold(
                Function<Path, Function<A,B>> onActualValue,
                Function<Path, B> onNull,
                Function<Path, B> onMissing,
                Function<Path, Function<Class<?>, Function<Class<?>, B>>> onWrongType) {

            return onWrongType.apply(path).apply(found).apply(expected);
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof WrongType) {
                WrongType wt = (WrongType)o;
                return wt.path.equals(path)
                        && wt.found.equals(found)
                        && wt.expected.equals(expected);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, found, expected);
        }

        @Override
        public String toString() {
            return "WrongType(found=" + found.getName()
                    + ", expected=" + expected.getName() + ")"  + atPath();
        }
    }

    private Hopefully() {}
}
