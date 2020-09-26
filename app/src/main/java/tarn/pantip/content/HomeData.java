package tarn.pantip.content;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import tarn.pantip.L;
import tarn.pantip.model.Topic;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 21 December 2016
 */

public class HomeData
{
    public int uid_ranking;
    public long topic_id;
    public int topic_type;
    public String title;
    public String thumbnail_url;
    public Author author;
    public String created_time;
    public String ranking_time;
    public int comments_count;
    public int views_count;
    public int votes_count;
    public boolean unescaped;
    public int view_height;
    private String relativeTime;
    private String statText;

    public static class Author
    {
        public long id;
        public String name;
        public String slug;
        public Avatar avatar;
    }

    public static class Avatar
    {
        public String original;
        public String large;
        public String medium;
        public String small;
    }

    public String getRelativeTime()
    {
        if (relativeTime == null)
        {
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            try
            {
                Date date = dateParser.parse(created_time);
                Calendar cal = Calendar.getInstance();
                long time = date.getTime() + cal.getTimeZone().getRawOffset();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(time);
                relativeTime = Utils.getRelativeTime(calendar, "d MMM HH.mm น.");
            }
            catch (ParseException e)
            {
                L.e(e, created_time);
                relativeTime = created_time;
            }
        }
        return relativeTime;
    }

    public String getStatText()
    {
        if (statText == null)
        {
            DecimalFormat nFormat = new DecimalFormat("#,##0");
            StringBuilder s = new StringBuilder();
            if (votes_count > 0) s.append('+').append(nFormat.format(votes_count));
            if (comments_count > 0)
            {
                if (s.length() > 0) s.append(Topic.BULLET);
                s.append(nFormat.format(comments_count)).append(" ความคิดเห็น");
            }
            statText = s.toString();
        }
        return statText;
    }
}
