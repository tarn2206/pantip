package tarn.pantip.model;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import tarn.pantip.Pantip;

/**
 * Created by Tarn on 15 September 2016
 */
class SearchDetail
{
    public SpanText text;
    public String author;
    private transient SpannableStringBuilder spanText;

    public synchronized SpannableStringBuilder getText()
    {
        if (spanText == null)
        {
            spanText = new SpannableStringBuilder();
            CharSequence s = text.getText();
            spanText.append(s.length() <= 150 ? s : s.subSequence(0, 150));
            if (s.length() > 150) spanText.append("...");

            int start = spanText.length();
            spanText.append(" « ").append(author);
            spanText.setSpan(new ForegroundColorSpan(Pantip.authorColor), start, spanText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return spanText;
    }
}