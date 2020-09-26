package tarn.pantip.model;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.content.Json;
import tarn.pantip.content.LocalObject;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 04 September 2016
 */
public class Topic
{
    public TopicType type;
    public long id;
    public String title;
    public String author;
    public int comments;
    public int votes;
    public Long time;
    public int status;
    public String deleteMessage;
    public List<Tag> tags;
    public boolean favorite;
    public static final DecimalFormat nFormat = new DecimalFormat("#,##0");
    private transient String statText;
    public static final String BULLET = " · ";

    public String getStatText()
    {
        if (statText == null)
        {
            StringBuilder s = new StringBuilder();
            if (votes > 0) s.append('+').append(nFormat.format(votes));
            if (comments > 0)
            {
                if (s.length() > 0) s.append(BULLET);
                s.append(nFormat.format(comments)).append(" ความคิดเห็น");
            }
            statText = s.toString();
        }
        return statText;
    }

    public void setTime(String s)
    {
        try
        {
            SimpleDateFormat dateParser = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
            Date date = dateParser.parse(s);
            if (date != null)
            {
                time = date.getTime();
            }
        }
        catch (Exception e)
        {
            L.e(e, s);
        }
    }

    public void setTime2(String s)
    {
        try
        {
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = dateParser.parse(s);
            if (date != null)
            {
                Calendar cal = Calendar.getInstance();
                time = date.getTime() + cal.getTimeZone().getRawOffset();
            }
        }
        catch (Exception e)
        {
            L.e(e, s);
        }
    }

    public String getRelativeTime()
    {
        Calendar calendar = Calendar.getInstance();
        if (time != null) calendar.setTimeInMillis(time);
        return Utils.getRelativeTime(calendar, "d MMM HH.mm น.");
    }

    @NonNull
    @Override
    public String toString()
    {
        return id + " " + title;
    }

    public static Observable<LocalObject<Topic>> load(String key, TopicType type, boolean isForum)
    {
        return RxUtils.observe(() -> {
            LocalObject<Topic> result = new LocalObject<>();
            File file = getDataFile(key, type, isForum);
            if (file.exists())
            {
                result.lastModified = file.lastModified();
                result.items = Json.fromFile(file, Topic[].class);
            }
            return result;
        });
    }

    private static File getDataFile(String key, TopicType type, boolean isForum) throws IOException
    {
        String fileName;
        if (isForum) fileName = "forum/" + key;
        else fileName = "tags/" + key;
        fileName += "_" + type.name().toLowerCase();
        return new File(Utils.getFileDir(), fileName + ".json");
    }

    public static Observable<Void> save(String key, TopicType type, boolean isForum, Topic[] items)
    {
        return RxUtils.observe(emitter -> {
            File file = getDataFile(key, type, isForum);
            Json.toFile(items, Topic[].class, file);
            emitter.onComplete();
        });
    }
}