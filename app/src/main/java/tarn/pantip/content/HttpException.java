package tarn.pantip.content;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by Tarn on 12 March 2017
 */

public class HttpException extends IOException
{
    private final String content;

    public HttpException(String method, String url, int status, String statusText, Map<String, List<String>> headers, String content)
    {
        super(log(method, url, status, statusText, headers, content));
        this.content = content;
    }

    public String text()
    {
        if (StringUtils.isBlank(content)) return getMessage();
        try
        {
            Document doc = Jsoup.parse(content);
            return doc.body().text();
        }
        catch (Exception e)
        {
            return content;
        }
    }

    private static String log(String method, String url, int status, String statusText, Map<String, List<String>> headers, String content)
    {
        StringBuilder s = new StringBuilder();
        s.append("\n").append(method).append(" ").append(url).append("\n");
        if (headers != null)
        {
            for (String key : headers.keySet())
            {
                if (key != null) s.append(key).append(": ");
                List<String> values = headers.get(key);
                if (values != null)
                {
                    for (int i = 0; i < values.size(); i++)
                    {
                        if (i > 0) s.append(", ");
                        s.append(values.get(i));
                    }
                }
                s.append('\n');
            }
        }
        s.append('\n').append(content);
        return s.toString();
    }
}