package tarn.pantip.model;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import tarn.pantip.L;

/**
 * User: Tarn
 * Date: 9/14/13 2:48 PM
 */
public class SearchResultItem
{
    public String url;
    public SpanText title;
    public SpanText content;
    public List<SearchDetail> details;

    public SearchResultItem()
    { }

    public SearchResultItem(String url)
    {
        try
        {
            this.url = URLDecoder.decode(url, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            this.url = url;
        }
        details = new ArrayList<>();
    }

    public long getId()
    {
        String key = "pantip.com/topic/";
        int i = StringUtils.indexOfIgnoreCase(url, key);
        if (i == -1) return -1;
        i += key.length();

        int x = i;
        while (x < url.length() && Character.isDigit(url.charAt(x))) x++;
        try
        {
            return Long.parseLong(url.substring(i, x));
        }
        catch (Exception e)
        {
            L.e(url);
            return 0;
        }
    }
}