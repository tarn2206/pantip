package tarn.pantip.content;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.util.RxUtils;

/**
 * Created by Tarn on 21/1/2561.
 */

public class Notify
{
    public int type;
    public String text;
    public String url;
    public String avatar;
    public boolean isNew;

    @NonNull
    public static Observable<Notify[]> load()
    {
        return RxUtils.observe(() -> {
            String content = Http.get("https://pantip.com/notifications").execute();
            Document doc = Jsoup.parse(content);

            Elements colSm12 = doc.select(".col-sm-12");
            if (colSm12 == null || colSm12.size() == 0) return new Notify[0];

            List<Notify> list = new ArrayList<>();
            for (Element col : colSm12)
            {
                if (!StringUtils.equalsIgnoreCase(col.tagName(), "div")) continue;

                Elements a = col.getElementsByTag("a");
                if (a == null || a.size() == 0) continue;

                Notify topic = getTopic(a.get(0));
                list.add(topic);
                for (int i = 1; i < a.size(); i++)
                {
                    Notify reply = getReply(a.get(i), topic.isNew);
                    list.add(reply);
                }
            }
            return list.toArray(new Notify[0]);
        });
    }

    private static Notify getTopic(Element e)
    {
        Notify t = new Notify();
        t.type = 0;
        t.text = e.text();
        t.url = e.attr("href");
        t.isNew = !e.parent().hasClass("primary-txt");
        return t;
    }

    private static Notify getReply(Element e, boolean isNew)
    {
        Notify r = new Notify();
        r.type = 1;
        r.text = e.text();
        r.url = e.attr("href");
        r.isNew = isNew;
        Elements imgElements = e.getElementsByTag("img");
        if (imgElements != null && imgElements.size() > 0)
        {
            String src = imgElements.get(0).attr("src");
            if (!src.endsWith("/unknown-avatar-38x38.png"))
            {
                r.avatar = src.replace("_m.jpg", "_l.jpg");
            }
        }
        return r;
    }

    public long getTopicId()
    {
        try
        {
            int i = url.indexOf("/topic/") + 7;
            int j = url.indexOf("/", i);
            if (j == -1) j = url.length();
            return Long.parseLong(url.substring(i, j));
        }
        catch (Exception e)
        {
            L.e(e, url);
            return 0;
        }
    }
}
