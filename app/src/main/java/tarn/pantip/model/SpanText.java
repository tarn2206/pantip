package tarn.pantip.model;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tarn on 10 September 2016
 */
public class SpanText
{
    public String text;
    public List<SpanInfo> spans;
    private transient SpannableString spanText;

    public SpanText()
    { }

    public SpanText(String text)
    {
        this.text = text;
        spans = new ArrayList<>();
    }

    public synchronized SpannableString getText()
    {
        if (spanText == null)
        {
            spanText = new SpannableString(text);
            for (SpanInfo info : spans)
            {
                spanText.setSpan(new ForegroundColorSpan(info.color), info.start, info.end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
        return spanText;
    }

    @NonNull
    @Override
    public String toString()
    {
        return text;
    }
}