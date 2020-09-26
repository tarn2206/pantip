package tarn.pantip.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.util.RxUtils;

public class EmotionApi
{
    private EmotionApi()
    {}

    public static Observable<JsonObject> post(String emo, long topicId, TargetType type, long commentId, int commentNo, long replyId, int replyNo)
    {
        return RxUtils.observe(() -> {
            Http http = Http.postAjax("https://pantip.com/forum/topic/express_emotion")
                    .form("emo", emo)
                    .form("topic_id", topicId);
            if (type == TargetType.Topic) http.form("type", "topic").form("id", topicId);
            else if (type == TargetType.Comment) http.form("type", "comment").form("id", commentId);
            else if (type == TargetType.Reply)
            {
                http.form("type", "reply").form("id", replyId).form("rid", commentId)
                        .form("comment_no", commentNo).form("no", replyNo);
            }

            return http.executeJson();
        });
    }

    public static Observable<JsonObject> get(JsonObject e)
    {
        return RxUtils.observe(() -> {
            Http http = Http.postAjax("https://pantip.com/forum/topic/get_emotion_data")
                    .form("type", e.get("type").getAsString())
                    .form("emo_type", e.get("emo_type").getAsString())
                    .form("check", e.get("check").getAsString());
            JsonElement id = e.get("id");
            if (id.isJsonObject())
            {
                JsonObject id2 = id.getAsJsonObject();
                http.form("id[comment_id]", id2.get("comment_id").getAsString());
                http.form("id[seq]", id2.get("seq").getAsString());
                http.form("id[no]", id2.get("no").getAsString());
            }
            else http.form("id", id.getAsString());

            return http.executeJson();
        });
    }
}
