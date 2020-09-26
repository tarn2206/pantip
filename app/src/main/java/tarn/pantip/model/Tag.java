package tarn.pantip.model;

/**
 * Created by Tarn on 04 September 2016
 */
public class Tag
{
    public String label;
    public String url;
    public Integer count;

    public Tag()
    { }

    public Tag(String label, String url)
    {
        this.label = label;
        this.url = url;
    }

    public Tag(String label, String url, int count)
    {
        this(label, url);
        this.count = count;
    }
}