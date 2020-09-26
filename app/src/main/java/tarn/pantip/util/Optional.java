package tarn.pantip.util;

import java.util.NoSuchElementException;
import java.util.Objects;

public final class Optional<T>
{
    private static final Optional<?> EMPTY = new Optional<>();
    private final T value;

    private Optional()
    {
        this.value = null;
    }

    private Optional(T value)
    {
        this.value = Objects.requireNonNull(value);
    }

    public static <T> Optional<T> of(T value)
    {
        return value == null ? empty() : new Optional<>(value);
    }

    public static<T> Optional<T> empty()
    {
        @SuppressWarnings("unchecked")
        Optional<T> t = (Optional<T>) EMPTY;
        return t;
    }

    public T get()
    {
        if (value == null)
        {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent()
    {
        return value != null;
    }

    public T orElse(T other)
    {
        return value != null ? value : other;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof Optional))
        {
            return false;
        }

        Optional<?> other = (Optional<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(value);
    }

    @Override
    public String toString()
    {
        return value != null ? String.format("Optional[%s]", value) : "Optional.empty";
    }
}
