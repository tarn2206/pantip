package tarn.pantip.content;

import android.util.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.internal.functions.Functions;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.model.Tag;
import tarn.pantip.util.Optional;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

public class Tags
{
    public List<Tag> tags;
    public boolean expired;

    private Tags(List<Tag> tags)
    {
        this.tags = tags;
    }

    public boolean isEmpty()
    {
        return tags == null || tags.isEmpty();
    }

    public static Observable<Optional<Tags>> loadCache(String forum, String tag)
    {
        return RxUtils.observe(() -> {
            File file = getFile(forum, tag);
            if (!file.exists()) return Optional.empty();

            List<Tag> tags = Json.toList(file, Tag[].class);
            Tags data = new Tags(tags);
            data.expired = Utils.after(file.lastModified(), 1, Calendar.DATE);

            return Optional.of(data);
        });
    }

    public static Observable<Tags> loadFromNetwork(String forum, String tag)
    {
        return RxUtils.observe(() -> {
            String url = tag == null
                        ? "https://pantip.com/api/forum-service/forum/room_main_tag?room=" + URLEncoder.encode(forum, "UTF-8")
                        : "https://pantip.com/api/forum-service/tag/get_tag_hit?tag_name=" + URLEncoder.encode(tag, "UTF-8") + "&fields=name,slug&limit=20";
            JsonObject json = Http.getAjax(url).executeJson();
            List<Tag> list = new ArrayList<>();
            JsonArray data = json.getAsJsonArray("data");
            if (data != null)
            {
                for (JsonElement e : data)
                {
                    JsonObject o = e.getAsJsonObject();
                    Tag item = new Tag();
                    item.label = o.get("name").getAsString();
                    item.url = o.get("slug").getAsString();
                    list.add(item);
                }
                save(forum, tag, list.toArray(new Tag[0])).subscribe(Functions.emptyConsumer(), L::e);
            }
            return new Tags(list);
        });
    }

    private static File getFile(String forum, String tag) throws IOException
    {
        String path = tag == null ? "forum/" + forum + "_tags.json" : "tag/" + tag + "_hits.json";
        return new File(Utils.getFileDir(Pantip.context), path);
    }

    private static Observable<Void> save(String forum, String tag, Tag[] data)
    {
        return RxUtils.observe(emitter -> {
            File file = getFile(forum, tag);
            Json.toFile(data, Tag[].class, file);
            emitter.onComplete();
        });
    }

    public static Observable<List<Tag>> search(String q)
    {
        return RxUtils.observe(() -> {
            List<Tag> list = new ArrayList<>();
            String uri = "https://pantip.com/tags/search_tag?str=" + new String(Base64.encode(q.getBytes(), Base64.DEFAULT));
            JsonObject json = Http.getAjax(uri).executeJson();
            if (json.has("tags"))
            {
                JsonArray tags = json.getAsJsonArray("tags");
                for (JsonElement e : tags)
                {
                    JsonObject o = e.getAsJsonObject();
                    list.add(new Tag(o.get("name").getAsString(), o.get("name").getAsString(), o.get("topic_count").getAsInt()));
                }
            }
            return list;
        });
    }
}
