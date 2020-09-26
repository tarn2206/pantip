package tarn.pantip.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.internal.functions.Functions;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.model.Topic;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.Optional;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

/**
 * User: tarn
 * Date: 1/27/13 11:34 AM
 */
public class Recommend
{
    public List<Topic> items;
    public boolean expired;

    private Recommend(List<Topic> items)
    {
        this.items = items;
    }

    public boolean isEmpty()
    {
        return items == null || items.isEmpty();
    }

    public static Observable<Optional<Recommend>> loadCache(String forum, String tag, int index)
    {
        return RxUtils.observe(() -> {
            Recommend data = null;
            File file = getFile(forum, tag, index);
            if (file.exists())
            {
                List<Topic> items = Json.toList(file, Topic[].class);
                data = new Recommend(items);
                data.expired = Utils.after(file.lastModified(), 10, Calendar.MINUTE);
            }
            return Optional.of(data);
        });
    }

    public static Observable<Recommend> loadFromNetwork(String forum, String tag, int index)
    {
        return RxUtils.observe(() -> {
            String url;
            if (tag == null)
                url = index == 0
                    ? "https://pantip.com/api/forum-service/forum/room_topic_recommend?room=" + forum + "&limit=10"
                    : "https://pantip.com/api/forum-service/forum/room_topic_trend?room=" + forum + "&limit=10";
            else url = "https://pantip.com/api/forum-service/tag/tag_topic_trend?tag_name=" + tag + "&limit=10";
            JsonObject json = Http.getAjax(url).executeJson();
            List<Topic> list = new ArrayList<>();
            JsonArray data = json.getAsJsonArray("data");
            for (JsonElement e : data)
            {
                JsonObject o = e.getAsJsonObject();
                Topic topic = new Topic();
                topic.id = o.get("topic_id").getAsLong();
                topic.type = TopicType.fromValue2(o.get("topic_type").getAsInt());
                topic.title = o.get("title").getAsString();
                topic.author = Json.getAsString(o, "author.name");
                topic.setTime2(o.get("created_time").getAsString());
                topic.comments = o.get("comments_count").getAsInt();
                topic.votes = o.get("votes_count").getAsInt();
                list.add(topic);
            }
            save(forum, tag, index, list.toArray(new Topic[0]))
                    .subscribe(Functions.emptyConsumer(), L::e);
            return new Recommend(list);
        });
    }

    private static File getFile(String forum, String tag, int index) throws IOException
    {
        String path;
        if (tag != null) path = "tag/" + tag + "_best.json";
        else path = "forum/" + forum + (index == 0 ? "_best.json" : "_trend.json");
        return new File(Utils.getFileDir(Pantip.context), path);
    }

    private static Observable<Void> save(String forum, String tag, int index, Topic[] data)
    {
        return RxUtils.observe(emitter -> {
            File file = getFile(forum, tag, index);
            Json.toFile(data, Topic[].class, file);
            emitter.onComplete();
        });
    }
}