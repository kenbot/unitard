package unitard;

public interface EditFocus extends Focus {
    Stuff done();
    Hopefully<Stuff> hopefullyDone();
    EditFocus at(Object key, Object... keys);
}
