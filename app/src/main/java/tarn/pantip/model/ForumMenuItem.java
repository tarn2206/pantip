package tarn.pantip.model;

/**
 * User: Tarn
 * Date: 5/4/13 5:05 PM
 */
public class ForumMenuItem
{
    public final MenuItemType type;
    public final String label;
    public String url;
    public int hitCount;
    public int iconId;
    public String description;

    public ForumMenuItem(MenuItemType type, String label)
    {
        this.type = type;
        this.label = label;
    }
}