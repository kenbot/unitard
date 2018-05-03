package unitard;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;

public final class Path implements Iterable<Object> {

    public static final Path EMPTY = new Path(TreePVector.empty());


    private final PVector<Object> elements;

    public static Path of(Object... elements) {
        return new Path(Arrays.asList(elements));
    }

    public Path(List<?> elements) {
        this.elements = TreePVector.from(elements);
    }

    public Path(PVector<Object> elements) {
        this.elements = elements;
    }

    public List<Object> getElements() {
        return elements;
    }

    public Hopefully<Object> getHead() {
        return Hopefully.getFromList(elements, 0);
    }

    public Path getTail() {
        return new Path(elements.isEmpty() ? elements : elements.subList(1, elements.size()));
    }

    public Path getAllButLast() {
        return elements.isEmpty() ? this : new Path(elements.subList(0, elements.size() - 1));
    }

    public Hopefully<Object> getLast() {
        return Hopefully.getFromList(elements, elements.size() - 1);
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public Path dot(Object el) {
        return new Path(elements.plus(el));

    }

    public Path key(Object nextKey) {
        return dot(nextKey);
    }

    public Path index(int i) {
        return dot(i);
    }

    public int length() {
        return elements.size();
    }

    public Path up() {
        if (elements.isEmpty()) {
            return this;
        } else {
            return new Path(elements.minus(elements.size() - 1));
        }
    }

    public Path join(Path path) {
        return new Path(elements.plusAll(path.elements));
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Path) {
            Path p = (Path)o;
            return elements.equals(p.elements);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        boolean first = true;
        for (Object e : elements) {
            if (e instanceof Integer) {
                str.append("[").append(e).append("]");
            } else {
                if (!first) {
                    str.append(".");
                }
                str.append(Objects.toString(e));
            }
            first = false;
        }
        return str.toString();
    }

    @Override
    public Iterator<Object> iterator() {
        return elements.iterator();
    }
}
