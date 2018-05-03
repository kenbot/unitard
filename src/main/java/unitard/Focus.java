package unitard;

public interface Focus {
    Focus at(Object key, Object... keys);

    Path getPath();

    Hopefully<Object> get();

    EditFocus put(Object key, Object value);
    EditFocus remove(Object key);
    EditFocus add(Object key);
    EditFocus insert(int index, Object key);
}
