package tarn.pantip.content;

/**
 * Created by Tarn on 15-Apr-15.
 */
public class LocalObject<T>
{
    public T[] items;
    public long lastModified;
    public String label;

    public LocalObject()
    { }

    public boolean hasItems()
    {
        return items != null && items.length > 0;
    }
}