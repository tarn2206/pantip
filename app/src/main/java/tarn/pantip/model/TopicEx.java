package tarn.pantip.model;

import androidx.annotation.NonNull;

import org.apache.commons.text.StringEscapeUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * User: tarn
 * Date: 1/27/13 4:43 PM
 */
public class TopicEx extends Comment implements Serializable
{
    private static final long serialVersionUID = 1L;

    public TopicType type;
    public String title;
    public int comments;
    public boolean follow;
    public boolean favorite;
    public List<Tag> tags;

    public String reviewProduct;
    public float reviewRating;
    public String mapsUrl;

    public String pollRemark;
    public String deadline;
    public boolean closeVote;
    public boolean voted;
    public boolean hasResult;
    public boolean requiredAnswer;
    public List<Question> questions;
    public String error;

    public TopicEx()
    { }

    private TopicEx(TopicType type, long id, String title)
    {
        this.type = type;
        this.id = id;
        this.title = StringEscapeUtils.unescapeHtml4(title);
    }

    public TopicEx(Topic o)
    {
        type = o.type;
        id = o.id;
        title = o.title;
        author = o.author;
        comments = o.comments;
        votes = o.votes;
        if (o.time != null)
        {
            time = Calendar.getInstance();
            time.setTimeInMillis(o.time);
        }
        if (o.tags != null)
        {
            tags = new ArrayList<>();
            for (Tag tag : o.tags)
            {
                Tag e = new Tag(tag.label, tag.url);
                tags.add(e);
            }
        }
        favorite = o.favorite;
        deleteMessage = o.deleteMessage;
    }

    @Override
    public boolean isTopic()
    {
        return true;
    }

    public Question addQuestion(Question q)
    {
        if (questions == null) questions = new ArrayList<>();
        questions.add(q);
        return q;
    }

    @Override
    public TopicEx createCopy()
    {
        TopicEx clone = copy(this, new TopicEx(type, id, title));
        clone.comments = comments;
        clone.favorite = favorite;
        clone.tags = tags;

        clone.reviewProduct = reviewProduct;
        clone.reviewRating = reviewRating;
        clone.mapsUrl = mapsUrl;

        clone.pollRemark = pollRemark;
        clone.deadline = deadline;
        clone.closeVote = closeVote;
        clone.voted = voted;
        clone.hasResult = hasResult;
        clone.requiredAnswer = requiredAnswer;
        clone.questions = questions;
        return clone;
    }

    @NonNull
    @Override
    public String toString()
    {
        return title == null ? "" : title;
    }
}