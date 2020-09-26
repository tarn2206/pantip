package tarn.pantip.model;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.content.Json;
import tarn.pantip.content.LocalObject;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

/**
 * User: tarn
 * Date: 1/25/13 3:13 PM
 */
public class Comment implements Serializable
{
    private static final long serialVersionUID = 1L;

    public long id;
    public int no;
    public long replyId;
    public int replyNo;
    public boolean hasPrev;
    public boolean hasNext;
    public int replyCount;

    public List<Story> storyList;
    transient String statText;
    public int mid;
    public String author;
    public String avatar;
    public Calendar time;
    int votes;
    public Emotion emotions;
    public String edit;
    public Calendar editTime;
    public boolean liked;

    public String ref;
    public String ref_id;
    public long created_time;

    public boolean isNew;
    public String deleteMessage;
    public String notify;
    public boolean deleted;

    public Comment()
    { }

    public boolean isTopic()
    {
        return false;
    }

    public void setTime(Date date)
    {
        time = convertTimeZone(date);
    }

    public void setEditTime(Date date)
    {
        editTime = convertTimeZone(date);
    }

    private Calendar convertTimeZone(Date date)
    {
        long ms = date.getTime() - 25200000; // Bangkok to GMT
        Calendar cal = Calendar.getInstance();
        ms += cal.getTimeZone().getRawOffset(); // GMT to local
        cal.setTimeInMillis(ms);
        return cal;
    }

    public void setCommentStat(int votes, Emotion emotions)
    {
        this.votes = votes;
        this.emotions = emotions;
        statText = null;
    }

    public void update(Emotion emotions)
    {
        this.emotions = emotions;
        statText = null;
    }

    public void toggleVote()
    {
        liked = !liked;
        votes += liked ? 1 : -1;
        statText = null;
    }

    public String getStatText()
    {
        if (statText == null)
        {
            StringBuilder s = new StringBuilder();
            if (votes > 0) s.append('+').append(Topic.nFormat.format(votes));
            if (emotions != null && emotions.total > 0)
            {
                if (s.length() > 0) s.append(Topic.BULLET);
                s.append(Topic.nFormat.format(emotions.total)).append(" ความรู้สึก");
            }
            statText = s.toString();
        }
        return statText;
    }

    public Comment createCopy()
    {
        return copy(this, new Comment());
    }

    static <T extends Comment> T copy(Comment a, T b)
    {
        b.id = a.id;
        b.no = a.no;
        b.replyId = a.replyId;
        b.replyNo = a.replyNo;
        b.hasPrev = a.hasPrev;
        b.hasNext = a.hasNext;
        b.replyCount = a.replyCount;

        b.storyList = a.storyList;
        b.statText = a.statText;
        b.mid = a.mid;
        b.author = a.author;
        b.avatar = a.avatar;
        b.time = a.time;
        b.votes = a.votes;
        b.emotions = a.emotions;
        b.edit = a.edit;
        b.editTime = a.editTime;
        b.liked = a.liked;

        b.ref = a.ref;
        b.ref_id = a.ref_id;
        b.created_time = a.created_time;

        b.isNew = a.isNew;
        b.deleteMessage = a.deleteMessage;
        b.notify = a.notify;
        b.deleted = a.deleted;

        return b;
    }

    @NonNull
    @Override
    public String toString()
    {
        if (replyNo == 0) return "No. " + no;
        return "No. " + no + "-" + replyNo;
    }

    public static Observable<LocalObject<Comment>> load(long topicId)
    {
        return RxUtils.observe(() -> {
            LocalObject<Comment> result = new LocalObject<>();
            File file = getDataFile(topicId);
            if (file.exists())
            {
                TopicData data = Json.fromFile(file, TopicData.class);
                if (data != null)
                {
                    result.lastModified = file.lastModified();
                    result.items = data.comments;
                    result.items[0] = data.topic;
                }
            }
            return result;
        });
    }

    private static File getDataFile(long topicId) throws IOException
    {
        return new File(Utils.getFileDir(), "topic/" + topicId + ".json");
    }

    public static class TopicData
    {
        public TopicEx topic;
        public Comment[] comments;
    }

    public static Observable<Void> save(Comment[] items)
    {
        return RxUtils.observe(emitter -> {
            if (items != null && items.length > 0)
            {
                TopicData data = new TopicData();
                data.topic = (TopicEx)items[0];
                data.comments = items;
                data.comments[0] = null;
                File file = getDataFile(data.topic.id);
                Json.toFile(data, TopicData.class, file);
            }
            emitter.onComplete();
        });
    }
}