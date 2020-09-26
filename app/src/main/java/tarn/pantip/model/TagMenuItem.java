package tarn.pantip.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * User: Tarn
 * Date: 7/15/13 10:53 AM
 */
public class TagMenuItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    public MenuItemType type;
    public String forum;
    public String label;
    public String url;
    public boolean isSubTag;
    public int hitCount;
    public int no;

    public TagMenuItem()
    {
        type = MenuItemType.Item;
    }

    public TagMenuItem(String forum, String label, String url)
    {
        type = MenuItemType.Item;
        this.forum = forum;
        this.label = label;
        this.url = url;
    }

    public TagMenuItem(String header)
    {
        type = MenuItemType.Header;
        label = header;
    }

    @NonNull
    @Override
    public String toString()
    {
        return label;
    }
}