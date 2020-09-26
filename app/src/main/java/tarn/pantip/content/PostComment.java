package tarn.pantip.content;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.RxUtils;

public class PostComment
{
    private PostComment()
    {}

    public static Observable<Long> post(int roomId, String[] tags, TopicType type, String title, String message,
                                        String product, float rating, boolean cr, boolean sr)
    {
        return RxUtils.observe(() -> {
            StringBuilder data = new StringBuilder();
            data.append("topic_type=").append(type.getValue());
            data.append("&value[room_id]=").append(roomId);
            data.append("&value[topic][raw]=").append(title);
            data.append("&value[topic][disp]=").append(title);
            data.append("&value[detail][raw]=").append(message);
            data.append("&value[detail][disp]=").append(message);
            if (tags != null)
            {
                for (String tag : tags)
                {
                    data.append("&value[tags][]=").append(tag);
                }
            }
            if (type == TopicType.Review)
            {
                data.append("&value[product][raw]=").append(product);
                data.append("&value[product][disp]=").append(product);
                data.append("&value[rating][star_rating]=").append(rating);
                data.append("&value[crReview][hasCR]=").append(cr ? 1 : 0);
                data.append("&value[srReview][hasSR]=").append(sr ? 1 : 0);
            }
            JsonObject json = Http.post("https://pantip.com/forum/new_topic/save", data.toString()).executeJson();
            if (json.has("error_message"))
            {
                throw new PantipException(title + "\n" + message + "\n\n" + json.get("error_message").getAsString());
            }
            String status = json.get("status").getAsString();
            if (status.equals("success"))
            {
                return json.get("id").getAsLong();
            }
            throw new IOException(status);
        });
    }

    public static Observable<String> reply(int topicType, long topicId, String message)
    {
        return replyComment(topicType, topicId, message, null, null, null, 0);
    }

    private static void preview(String raw, String ref, String ref_id, String ref_comment, long time) throws IOException
    {
        StringBuilder data = new StringBuilder();
        data.append("msg[raw]=").append(raw);
        if (ref != null) // reply comment
        {
            data.append("&msg[ref]=").append(ref);
            data.append("&msg[ref_id]=").append(ref_id);
            data.append("&msg[ref_comment]=").append(ref_comment);
            data.append("&msg[time]=").append(time);
        }
        String result = Http.postAjax("https://pantip.com/forum/topic/preview_comment", data.toString()).execute();
        L.d(result);
    }

    public static Observable<String> replyComment(int topicType, long topicId, String message, String ref, String ref_id, String ref_comment, long time)
    {
        return RxUtils.observe(() -> {
            preview(message, ref, ref_id, ref_comment, time);

            String url;
            StringBuilder data = new StringBuilder();
            data.append("type=").append(topicType);
            data.append("&topic_id=").append(topicId);
            data.append("&msg[raw]=").append(message);
            data.append("&msg[disp]=").append(message);
            if (ref == null)
                url = "https://pantip.com/forum/topic/save_comment";
            else
            {
                url = "https://pantip.com/forum/topic/save_reply";
                data.append("&msg[ref]=").append(ref);
                data.append("&msg[ref_id]=").append(ref_id);
                data.append("&msg[ref_comment]=").append(ref_comment);
                data.append("&msg[time]=").append(time);
            }
            return Http.post(url, data.toString()).execute();
        });
    }

    public static Observable<JsonObject> getInfo(long topicId, long commentId, int commentNo)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/forum/topic/get_comment_info";
            String data = "type_edit=comment"
                    + "&sendEdit[url]=/forum/topic/edit_comment_preview_and_update"
                    + "&preview[preview_tmpl]=#edit-comment-preview-tmpl"
                    + "&preview[url]=/forum/topic/edit_comment_preview"
                    + "&preview[sendEditPreviewing][url]=/forum/topic/edit_comment_update"
                    + "&topic_id=" + topicId
                    + "&redirect_no=" + commentNo
                    + "&cid=" + commentId
                    + "&comment_no=" + commentNo
                    + "&url=/forum/topic/get_comment_info";
            return Http.postAjax(url, data).executeJson();
        });
    }

    public static Observable<JsonObject> getInfo(long topicId, long commentId, int commentNo, long replyId, int replyNo)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/forum/topic/get_reply_info";
            String data = "type_edit=reply"
                    + "&sendEdit[url]=/forum/topic/edit_reply_preview_and_update"
                    + "&preview[preview_tmpl]=#edit-reply-preview-tmpl"
                    + "&preview[url]=/forum/topic/edit_reply_preview"
                    + "&preview[sendEditPreviewing][url]=/forum/topic/edit_reply_update"
                    + "&topic_id=" + topicId
                    + "&redirect_no=" + commentNo + "-" + replyNo
                    + "&comment_no=" + commentNo
                    + "&cid=" + commentId
                    + "&rp_id=" + replyId
                    + "&rp_no=" + replyNo
                    + "&url=/forum/topic/get_reply_info";
            return Http.postAjax(url, data).executeJson();
        });
    }

    public static Observable<JsonObject> edit(long topicId, long commentId, int commentNo, String message)
    {
        return RxUtils.observe(() -> {
            String encoded;
            try
            {
                encoded = URLEncoder.encode(message, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                encoded = message;
            }
            String url = "https://pantip.com/forum/topic/edit_comment_preview_and_update";
            String data = "raw=" + encoded
                    + "&cid=" + commentId
                    + "&topic_id=" + topicId
                    + "&comment_no=" + commentNo;
            return Http.postAjax(url, data).executeJson();
        });
    }

    public static Observable<JsonObject> edit(long topicId, long commentId, int commentNo, long replyId, int replyNo, String message)
    {
        return RxUtils.observe(() -> {
            String encoded;
            try
            {
                encoded = URLEncoder.encode(message, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                encoded = message;
            }
            String url = "https://pantip.com/forum/topic/edit_reply_preview_and_update";
            String data = "raw=" + encoded
                    + "&cid=" + commentId
                    + "&rp_no=" + replyNo
                    + "&rp_id=" + replyId
                    + "&topic_id=" + topicId
                    + "&comment_no=" + commentNo;
            return Http.postAjax(url, data).executeJson();
        });
    }
}
