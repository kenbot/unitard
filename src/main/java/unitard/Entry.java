package unitard;

import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class Entry {
    private final Object key, value;

    public static Entry fromMapEntry(Map.Entry mapEntry) {
        return new Entry(mapEntry.getKey(), mapEntry.getValue());
    }

    public Entry(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public <A> Hopefully<A> getKeyAs(Class<A> cls) {
        return Hopefully.notNull(key).as(cls);
    }

    public <A> Hopefully<A> getValueAs(Class<A> cls) {
        return Hopefully.notNull(value).as(cls);
    }

    public Entry modifyValue(UnaryOperator<Object> modifyValue) {
        return new Entry(key, modifyValue.apply(value));
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Entry) {
            Entry e = (Entry)o;
            return Objects.equals(e.key, this.key)
                    && Objects.equals(e.value, this.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return key + " -> " + value;
    }
}
