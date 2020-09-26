package tarn.pantip.widget;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Tarn on 04 March 2017
 */

public class EmoticonSpan extends DynamicDrawableSpan
{
    private final Drawable drawable;
    private final TextView textView;

    private EmoticonSpan(Drawable drawable, TextView textView)
    {
        super(ALIGN_BASELINE);
        this.drawable = drawable;
        this.textView = textView;
    }

    public static EmoticonSpan create(Drawable drawable, TextView textView)
    {
        return new EmoticonSpan(drawable, textView);
    }

    @Override
    public Drawable getDrawable()
    {
        return drawable;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm)
    {
        float width = textView.getPaint().measureText("A");
        return super.getSize(paint, text, start, end, fm) + (int)(width / 3);
    }
}
