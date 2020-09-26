package tarn.pantip.model;

import java.io.Serializable;

/**
 * User: Tarn
 * Date: 9/14/13 1:33 PM
 */
public class LinkSpec implements Serializable
{
    public LinkSpec()
    { }

    public LinkSpec(SpanType type, int start)
    {
        this.type = type;
        this.start = start;
    }

    public LinkSpec(SpanType type, String url, int start)
    {
        this.type = type;
        this.url = url;
        this.start = start;
    }

    public String url;
    public int start;
    public int end;
    public SpanType type;

    public enum SpanType
    {
        Url,
        Bold,
        Italic,
        Underline,
        Emoticon
    }
}
