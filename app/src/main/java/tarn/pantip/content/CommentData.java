package tarn.pantip.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.model.Comment;
import tarn.pantip.model.Emotion;
import tarn.pantip.model.TopicEx;
import tarn.pantip.util.RxUtils;

/**
 * User: tarn
 * Date: 1/28/13 11:03 AM
 */
public class CommentData
{
    public static Observable<List<Comment>> loadComments(TopicEx topic)
    {
        return RxUtils.observe(() -> {
            List<Comment> all = new ArrayList<>();
            int page = 1;
            while (true)
            {
                List<Comment> list = loadComments(topic, page++);
                if (list.size() == 0) break;

                all.addAll(list);
                int lastReply = list.get(list.size() - 1).no;
                if (lastReply == 0 || lastReply % 100 > 0) break;
            }
            return all;
        });
    }

    private static List<Comment> loadComments(TopicEx topic, int page) throws IOException, ParseException
    {
        List<Comment> results = new ArrayList<>();
        if (page == 1)
        {
            results = TopicParser.load(topic);
            if (results == null)
            {
                results = new ArrayList<>();
                results.add(topic);
                return results;
            }
        }

        String url = "https://pantip.com/forum/topic/render_comments?tid=" + topic.id + "&param=page" + page;
        JsonObject json = Http.getAjax(url).executeJson();
        if (!json.has("comments")) return results;

        if (page == 1) ((TopicEx)results.get(0)).comments = json.get("count").getAsInt();

        JsonArray comments = json.getAsJsonArray("comments");
        for (JsonElement element : comments)
        {
            json = element.getAsJsonObject();
            Comment comment = parseComment(json);
            TopicParser.splitComment(results, comment);

            if (comment.replyCount > 0) parseReply(comment, json, results);
        }
        return results;
    }

    private static Comment parseComment(JsonObject obj) throws ParseException, UnsupportedEncodingException
    {
        Comment item = new Comment();
        item.id = obj.get(obj.has("_id") ? "_id" : "comment_id").getAsLong();
        item.no = obj.get("comment_no").getAsInt();
        if (obj.has("reply_no"))
        {
            item.replyId = obj.get("reply_id").getAsLong();
            item.replyNo = obj.get("reply_no").getAsInt();
            if (obj.has("admin_reply_del")) item.deleteMessage = "ความคิดเห็นนี้ได้ถูก Pantip.com ลบออกไปจากระบบแล้ว";//getDeleteMessage(obj.get("admin_reply_del").getAsString());
        }
        else
        {
            item.ref = obj.get("ref_reply").getAsString();
            item.ref_id = obj.get("ref_reply_id").getAsString();
            item.created_time = obj.get("created_time").getAsLong();
            if (obj.has("admin_comment_del")) item.deleteMessage = "ความคิดเห็นนี้ได้ถูก Pantip.com ลบออกไปจากระบบแล้ว";//getDeleteMessage(obj.get("admin_comment_del").getAsString());
        }
        item.replyCount = obj.has("reply_count") ? obj.get("reply_count").getAsInt() : 0;

        String message = obj.get("message").getAsString().replace("\n", "");
        TopicParser.parseStory(item, Jsoup.parse(message).child(0).child(1), false);

        TopicParser.removeDuplicateLink(item.storyList);

        JsonObject user = obj.getAsJsonObject("user");
        item.mid = user.get("mid").getAsInt();
        item.author = StringEscapeUtils.unescapeHtml4(user.get("name").getAsString());
        if (user.has("avatar"))
        {
            JsonElement large = user.getAsJsonObject("avatar").get("large");
            if (large != null) item.avatar = large.getAsString();
        }
        if (item.avatar != null && item.avatar.endsWith("/unknown-avatar-128x128.png")) item.avatar = null;

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
        item.setTime(dateFormat.parse(obj.get("data_utime").getAsString()));
        if (obj.has("last_mod_iso_time")) item.setEditTime(dateFormat.parse(obj.get("last_mod_iso_time").getAsString()));

        if (obj.has("good_bad_vote"))
        {
            item.liked = obj.getAsJsonObject("good_bad_vote").get("good_voted").getAsString().equals("i-vote");
        }
        Emotion emotions = parseEmotions(obj.getAsJsonObject("emotion"));
        item.setCommentStat(obj.get("point").getAsInt(), emotions);

        return item;
    }

    private static void parseReply(Comment comment, JsonObject json, List<Comment> results)
    {
        int count = 0;
        while (count < comment.replyCount)
        {
            try
            {
                if (count > 0)
                {
                    String url = "https://pantip.com/forum/topic/render_replys?cid=" + comment.id + "&last=" + count + "&c=0&ac=n&o=0";
                    json = Http.getAjax(url).executeJson();
                }
                JsonArray replies = json.getAsJsonArray("replies");
                if (replies == null || replies.size() == 0) break;
                for (JsonElement r : replies)
                {
                    Comment reply = parseComment(r.getAsJsonObject());
                    reply.ref = comment.ref;
                    reply.ref_id = comment.ref_id;
                    reply.created_time = comment.created_time;
                    reply.deleted = comment.deleteMessage != null;
                    TopicParser.splitComment(results, reply);
                    count++;
                }
            }
            catch (Exception e)
            {
                L.e(e);
                break;
            }
        }
    }

    public static Emotion parseEmotions(JsonObject emo)
    {
        Emotion emotions = new Emotion();
        emotions.total = emo.get("sum").getAsInt();
        getEmotion(emotions.like, emo, "like");
        getEmotion(emotions.laugh, emo, "laugh");
        getEmotion(emotions.love, emo, "love");
        getEmotion(emotions.impress, emo, "impress");
        getEmotion(emotions.scary, emo, "scary");
        getEmotion(emotions.surprised, emo, "surprised");
        if (emo.has("latest"))
        {
            JsonArray array = emo.getAsJsonArray("latest");
            for (JsonElement e : array)
            {
                JsonObject o = e.getAsJsonObject();
                Emotion.Latest latest = new Emotion.Latest();
                latest.name = o.get("name").getAsString();
                latest.emotion = o.get("emotion").getAsString();
                emotions.latest.add(latest);
            }
        }
        return emotions;
    }

    private static void getEmotion(Emotion.Info info, JsonObject emo, String value)
    {
        if (!emo.has(value)) return;
        JsonObject o = emo.getAsJsonObject(value);
        if (o.has("count")) info.count = o.get("count").getAsInt();
        if (o.has("status")) info.selected = o.get("status").getAsBoolean();
    }
}