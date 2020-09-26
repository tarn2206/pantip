package tarn.pantip.model;

import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

import java.io.Serializable;
import java.util.List;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.app.BaseActivity;
import tarn.pantip.content.Emoticons;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.EmoticonSpan;

/**
 * User: tarn
 * Date: 1/31/13 12:53 PM
 */
public class Story implements Serializable
{
    private static final long serialVersionUID = 1L;

    // SpannableString cannot serializable
    public StoryType type;
    public String text;
    public List<LinkSpec> spans;
    public Size fullSize;
    public Size thumbSize;
    public List<Story> spoil;

    public Story()
    { }

    public Story(StoryType type, String text)
    {
        this(type, text, null);
    }

    public Story(StoryType type, String text, List<LinkSpec> spans)
    {
        this.type = type;
        this.text = text;
        this.spans = spans;
    }

    public Story(List<Story> spoil)
    {
        type = StoryType.Spoil;
        this.spoil = spoil;
    }

    public CharSequence getSpannable(BaseActivity activity, TextView textView)
    {
        if (spans == null || spans.size() == 0) return text;

        String s;
        int n  = 0;
        LinkSpec first = spans.get(0);
        if (first.start == 0 && first.type == LinkSpec.SpanType.Emoticon)
        {
            s = " " + text + " ";
            n = 1;
        }
        else s = text + " ";
        SpannableString sp = new SpannableString(s);
        for (LinkSpec si : spans)
        {
            if (si.type == LinkSpec.SpanType.Url)
            {
                URLSpan span = new URLSpan(si.url);
                sp.setSpan(span, si.start + n, si.end + n, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else if (si.type == LinkSpec.SpanType.Bold)
            {
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                sp.setSpan(span, si.start + n, si.end + n, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else if (si.type == LinkSpec.SpanType.Italic)
            {
                StyleSpan span = new StyleSpan(Typeface.ITALIC);
                sp.setSpan(span, si.start + n, si.end + n, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else if (si.type == LinkSpec.SpanType.Underline)
            {
                UnderlineSpan span = new UnderlineSpan();
                sp.setSpan(span, si.start + n, si.end + n, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else if (si.type == LinkSpec.SpanType.Emoticon)
            {
                BitmapDrawable drawable = Emoticons.get(activity, si.url);
                if (drawable == null) setEmoTextStyle(sp, si, n);
                else
                {
                    try
                    {
                        EmoticonSpan span = EmoticonSpan.create(drawable, textView);
                        sp.setSpan(span, si.start + n, si.end + n, 0);
                    }
                    catch (Exception e)
                    {
                        L.e(e);
                        setEmoTextStyle(sp, si, n);
                    }
                }
            }
        }
        return sp;
    }

    private void setEmoTextStyle(SpannableString sp, LinkSpec si, int n)
    {
        int fontSize = (int)Utils.textSize(Utils.fixTextSize(Pantip.textSize - 3));
        sp.setSpan(new StyleSpan(Typeface.ITALIC), si.start + n, si.end + n, 0);
        sp.setSpan(new AbsoluteSizeSpan(fontSize), si.start + n, si.end + n, 0);
    }
}