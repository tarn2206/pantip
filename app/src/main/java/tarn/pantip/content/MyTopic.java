package tarn.pantip.content;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.Pantip;
import tarn.pantip.model.Topic;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.Optional;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

public class MyTopic
{
    public long first_id;
    public long last_id;
    public int page;
    public int max_page;
    public long time;
    public Topic[] items;

    @NonNull
    @Override
    public String toString()
    {
        return String.format(Locale.US, "Page %d / %d : %d items", page, max_page, items == null ? -1 : items.length);
    }

    public static Observable<MyTopic> topic(int mid, int page, long ftid, long ltid)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/profile/me/ajax_my_topic?type=topic&mid=" + mid + "&p=" + page;
            if (page > 1) url += "&ltpage=" + (page - 1) + "&ftid=" + ftid + "&ltid=" + ltid;
            JsonObject json = Http.getAjax(url).executeJson();
            return from(json);
        });
    }

    public static Observable<MyTopic> comment(int mid, int page, long ftid, long ltid)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/profile/me/ajax_my_comment?type=comment&mid=" + mid + "&p=" + page;
            if (page > 1) url += "&ltpage=" + (page - 1) + "&ftid=" + ftid + "&ltid=" + ltid;
            JsonObject json = Http.getAjax(url).executeJson();
            return from(json);
        });
    }

    public static Observable<MyTopic> bookmarks(int mid, int page, long ftid, long ltid)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/profile/me/ajax_my_bookmarks?type=bookmarks&mid=" + mid + "&p=" + page;
            if (page > 1) url += "&ltpage=" + (page - 1) + "&ftid=" + ftid + "&ltid=" + ltid;
            JsonObject json = Http.getAjax(url).executeJson();
            return from(json);
        });
    }

    public static Observable<MyTopic> history(int mid, int page, long ftid, long ltid)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/profile/me/ajax_my_history?type=history&mid=" + mid + "&p=" + page;
            if (page > 1) url += "&ltpage=" + (page - 1) + "&ftid=" + ftid + "&ltid=" + ltid;
            JsonObject json = Http.getAjax(url).executeJson();
            return from(json);
        });
    }

    private static MyTopic from(JsonObject json)
    {
        MyTopic result = new MyTopic();
        if (json == null)
        {
            result.items = new Topic[0];
            return result;
        }

        List<Topic> list = new ArrayList<>();
        JsonArray array = json.get("result").getAsJsonArray();
        for (JsonElement e : array)
        {
            Topic topic = getTopic(e.getAsJsonObject());
            list.add(topic);
        }
        result.items = list.toArray(new Topic[0]);
        if (result.items.length > 0)
        {
            result.max_page = json.get("max_page").getAsInt();
            result.first_id = json.get("first_id").getAsLong();
            result.last_id = json.get("last_id").getAsLong();
        }
        result.page = json.get("page").getAsInt();
        return result;
    }

    private static Topic getTopic(JsonObject o)
    {
        Topic topic = new Topic();
        topic.type = TopicType.parse(o.get("icon_topic").getAsString());
        topic.id = o.get("topic_id").getAsLong();
        topic.title = StringEscapeUtils.unescapeHtml4(o.get("disp_topic").getAsString());
        topic.author = StringEscapeUtils.unescapeHtml4(o.get("author").getAsString());
        topic.setTime(o.get("utime").getAsString());
        topic.comments = o.get("comments").getAsInt();
        topic.votes = o.get("votes").getAsInt();
        if (o.has("topic_status"))
            topic.status = o.get("topic_status").getAsInt();
        if (o.has("topic_delete"))
        {
            String html = o.get("topic_delete").getAsString();
            if (!"false".equals(html))
            {
                topic.deleteMessage = Utils.fromHtml(html);
            }
        }
        return topic;
    }

    public static Observable<Optional<MyTopic>> load(int mid, String type)
    {
        return RxUtils.observe(() -> {
            MyTopic result = null;
            File file = getDataFile(mid, type);
            if (file.exists())
            {
                result = Json.fromFile(file, MyTopic.class);
                if (result != null)
                {
                    result.time = file.lastModified();
                }
            }
            return Optional.of(result);
        });
    }

    private static File getDataFile(int mid, String type) throws IOException
    {
        return new File(Utils.getFileDir(), "/profile/" + mid + "/" + type + ".json");
    }

    public static void save(int mid, String type, MyTopic obj)
    {
        RxUtils.observe(emitter -> {
            File file = getDataFile(mid, type);
            Json.toFile(obj, MyTopic.class, file);
            emitter.onComplete();
        }).subscribe();
    }

    public static void delete()
    {
        RxUtils.observe(emitter -> {
            File fileDir = Utils.getFileDir();
            File[] files = fileDir.listFiles((FileFilter)new WildcardFileFilter(Pantip.currentUser.id + "_*.json"));
            if (files == null) return;
            for (File file : files)
            {
                FileUtils.deleteQuietly(file);
            }
            emitter.onComplete();
        }).subscribe();
    }
}
