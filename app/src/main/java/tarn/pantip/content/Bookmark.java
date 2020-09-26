package tarn.pantip.content;

import com.google.gson.JsonObject;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.Pantip;
import tarn.pantip.util.RxUtils;

public class Bookmark
{
    private Bookmark()
    {}

    public static Observable<Boolean> add(long tid)
    {
        return RxUtils.observe(() -> {
            JsonObject json = Http.postAjax("https://pantip.com/forum/topic/bookmarks")
                    .form("tid", tid)
                    .form("ac", "push")
                    .executeJson();
            return success(json);
        });
    }

    public static Observable<Boolean> remove(long tid)
    {
        return RxUtils.observe(() -> {
            JsonObject json = Http.postAjax("https://pantip.com/forum/topic/bookmarks")
                    .form("tid", tid)
                    .form("ac", "pop")
                    .executeJson();
            return success(json);
        });
    }

    public static Observable<Boolean> removeMy(long tid)
    {
        return RxUtils.observe(() -> {
            JsonObject json = Http.postAjax("https://pantip.com/profile/me/remove_my_bookmarks")
                    .form("topic_id", tid)
                    .form("mid", Pantip.currentUser.id)
                    .executeJson();
            return success(json);
        });
    }

    private static boolean success(JsonObject json)
    {
        return json != null && json.has("status") && "success".equals(json.get("status").getAsString());
    }

    public static Observable<String> follow(long topicId)
    {
        return RxUtils.observe(() -> Http.postAjax("https://pantip.com/forum/topic/set_follow")
                .form("topic_id", topicId)
                .form("follow_type", 3)
                .form("c_no", 0)
                .execute());
    }

    public static Observable<String> unfollow(long topicId)
    {
        return RxUtils.observe(() -> Http.postAjax("https://pantip.com/notifications/notifications/unfollow_topic")
                .form("topic_id", topicId)
                .execute());
    }
}
