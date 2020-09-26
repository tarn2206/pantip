package tarn.pantip.content;

import com.google.gson.JsonObject;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.util.RxUtils;

/**
 * Created by Tarn on 09 September 2016
 */
public class Vote
{
    public boolean success;
    public String message;

    public static Observable<Vote> post(TargetType type, boolean value, long topicId, long commentId, int commentNo, long replyId, int replyNo)
    {
        return RxUtils.observe(() -> {
            Vote vote = new Vote();
            StringBuilder data = new StringBuilder();
            data.append("vote_type=").append(type.getValue()).append("&vote_status=").append(value ? -1 : 1);
            data.append("&topic_id=").append(topicId);
            if (type == TargetType.Comment)
            {
                data.append("&comment_id=").append(commentId).append("&comment_no=").append(commentNo);
            }
            else if (type == TargetType.Reply)
            {
                data.append("&cid=").append(commentId).append("&comment_no=").append(commentNo);
                data.append("&rp_id=").append(replyId).append("&rp_no=").append(replyNo);
            }

            JsonObject json = Http.postAjax("https://pantip.com/vote1/cal_like", data.toString()).executeJson();
            if (json.has("error_message")) vote.message = json.get("error_message").getAsString();
            else if (json.has("vote_success")) vote.success = json.get("vote_success").getAsBoolean();
            if (vote.success)
            {
                String html = json.get("vote_message").getAsString();
                vote.message = Jsoup.clean(html, Whitelist.none());
                vote.message = vote.message.replace("00:00 นาที", "1 ชั่วโมง");
            }
            return vote;
        });
    }
}