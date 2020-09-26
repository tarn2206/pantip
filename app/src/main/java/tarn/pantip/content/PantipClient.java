package tarn.pantip.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.model.DetailException;
import tarn.pantip.model.Tag;
import tarn.pantip.model.Topic;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.RxUtils;

/**
 * User: Tarn
 * Date: 5/17/13 10:36 PM
 */
public class PantipClient
{
    private PantipClient()
    {}

    /*public static void pSearch(String query, Callback<SearchResult> callback)
    {
        String q;
        try
        {
            q = URLEncoder.encode(query, "UTF-8");
        }
        catch (Exception e)
        {
            q = query;
        }
        String s = Pantip.getSharedPreferences().getString("search", "a");
        String url = "http://search.pantip.com/ss?q=" + q + "&s=" + s;
        sendAsync(GET, url, null, new PSearchResultParser(query, callback));
    }

    public static void pSearchNext(String query, String url, Callback<SearchResult> callback)
    {
        sendAsync(GET, url, null, new PSearchResultParser(query, callback));
    }

    private static class PSearchResultParser implements Callback<String>
    {
        private String query;
        private Callback<SearchResult> callback;

        public PSearchResultParser(String query, Callback<SearchResult> callback)
        {
            this.query = query;
            this.callback = callback;
        }

        @Override
        public void complete(String html)
        {
            callback.complete(parse(html));
        }

        private SearchResult parse(String html)
        {
            SearchResult result = new SearchResult();
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("td[style=border-top:solid 1px #dedede]");
            if (elements == null || elements.size() == 0)
            {
                return result;
            }
            Element td = elements.get(0);
            if (td.childNodeSize() == 0)
            {
                return result;
            }

            String q = query.toLowerCase();
            Element p = td.child(0);
            result.items = new ArrayList<>();
            Iterator<Node> nodes = p.childNodes().iterator();
            while (nodes.hasNext())
            {
                Node node = nodes.next();
                if (node instanceof TextNode)
                {
                    String s = node.outerHtml();
                    if (s.contains(":&nbsp;"))
                    {
                        Node temp = nodes.next();
                        Element a = (Element)temp; // a
                        SearchResultItem item = new SearchResultItem("http://search.pantip.com" + a.attr("href"));
                        item.title = highlight(a.text(), q);
                        try
                        {
                            while (true)
                            {
                                String no = null;
                                String text = null;
                                Element e;
                                temp = nodes.next(); // br
                                temp = nodes.next(); // \n
                                if ((temp instanceof TextNode) && ((TextNode)temp).getWholeText().equals("\n"))
                                {
                                    temp = nodes.next();
                                    e = (Element)temp;
                                    if (e.tagName().equals("br")) break;

                                    if (e.tagName().equals("a"))
                                    {
                                        no = e.text();
                                        temp = nodes.next(); // \n
                                        temp = nodes.next();
                                        e = (Element)temp; // font
                                    }
                                    text = e.text();
                                    temp = nodes.next(); // <<<
                                }
                                temp = nodes.next();
                                if (text != null)
                                {
                                    e = (Element)temp; // font
                                    String author = e.text();
                                    item.details.add(highlight(no, text, author, q));
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            L.e(new DetailException(ex, "[" + query + "] " + temp));
                        }
                        result.items.add(item);
                    }
                }
            }
            elements = doc.select("div[class=gp]");
            if (elements != null && elements.size() > 0)
            {
                Element e = elements.get(elements.size() - 1);
                if (e.tagName().equals("div") && e.children().size() > 0)
                {
                    e = e.child(0);
                    if (e.tagName().equals("a")) result.nextUrl = "http://search.pantip.com" + e.attr("href");
                }
            }
            return result;
        }

        private SpanText highlight(String text, String query)
        {
            text = text.replace("\u00a0", "");
            SpanText result = new SpanText(text);
            text = text.toLowerCase();
            int start = 0;
            while ((start = text.indexOf(query, start)) != -1)
            {
                int end = start + query.length();
                result.spans.add(new SpanInfo(start, end, Pantip.colorAccent));
                start = end;
            }
            return result;
        }

        private SearchDetail highlight(String no, String text, String author, String query)
        {
            SpanText result = new SpanText("");
            int offset = 0;
            if (no != null)
            {
                offset = no.length();
                result.text = no + " ";
                result.spans.add(new SpanInfo(0, offset++, Pantip.colorAccent));
            }
            text = text.replace("\u00a0", "");
            result.text += text;
            text = text.toLowerCase();
            int start = 0;
            while ((start = text.indexOf(query, start)) != -1)
            {
                int end = start + query.length();
                result.spans.add(new SpanInfo(start + offset, end + offset, Pantip.colorAccent));
                start = end;
            }

            SearchDetail detail = new SearchDetail();
            detail.text = result;
            detail.author = author;
            return detail;
        }

        @Override
        public void error(Throwable tr)
        {
            callback.error(tr);
        }
    }*/

    public static Observable<String> openTopic(long id)
    {
        return RxUtils.observe(() -> Http.get("https://pantip.com/topic/" + id).execute());
    }

    public static String getTagLabel(String tag) throws IOException
    {
        Document doc = Http.getDocument("https://pantip.com/tag/" + URLEncoder.encode(tag, "utf-8"));
        String title = doc.title();
        int i = title.indexOf('-');
        return i == -1 ? title : title.substring(0, i - 1);
    }

    public static Observable<List<Topic>> loadTopics(boolean loadForum, String text, int type, int page, long lastId)
    {
        return RxUtils.observe(() -> {
            String url;
            if (loadForum)
            {
                url = "https://pantip.com/api/forum-service/forum/room_topic?room=" + URLEncoder.encode(text, "utf-8") + "&limit=50";
            }
            else
            {
                url = "https://pantip.com/api/forum-service/forum/tag_topic?tag_name=" + URLEncoder.encode(text, "utf-8") + "&limit=50";
            }
            if (type > 0) url += "&topic_type=" + type;
            if (page > 1) url += "&next_id=" + lastId;

            JsonObject json = Http.getAjax(url).executeJson();
            List<Topic> list = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray("data"))
            {
                JsonObject o = element.getAsJsonObject();
                if (!o.has("topic_type") || !o.has("title")) continue;

                try
                {
                    Topic topic = new Topic();
                    topic.id = o.get("topic_id").getAsLong();
                    topic.type = TopicType.fromValue(o.get("topic_type").getAsInt());
                    topic.title = StringEscapeUtils.unescapeHtml4(o.get("title").getAsString());
                    topic.author = StringEscapeUtils.unescapeHtml4(o.get("author").getAsJsonObject().get("name").getAsString());
                    topic.setTime2(o.get("created_time").getAsString());
                    topic.comments = o.get("comments_count").getAsInt();
                    topic.votes = o.get("votes_count").getAsInt();
                    if (o.has("tags"))
                    {
                        JsonArray tags = o.get("tags").getAsJsonArray();
                        topic.tags = new ArrayList<>(tags.size());
                        for (JsonElement tag : tags)
                        {
                            JsonObject o1 = tag.getAsJsonObject();
                            topic.tags.add(new Tag(o1.get("name").getAsString(), o1.get("slug").getAsString()));
                        }
                    }
                    list.add(topic);
                }
                catch (Exception ex)
                {
                    L.e(new DetailException(ex, element.toString()));
                }
            }
            return list;
        });
    }

    public static JsonObject getRoomTags() throws IOException
    {
        return Http.getAjax("https://pantip.com/forum/new_topic/get_rooms_tags").executeJson();
    }
}