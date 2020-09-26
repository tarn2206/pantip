package tarn.pantip.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Created by Tarn on 08 February 2017
 */

class TarnImageSpan extends ReplacementSpan
{
    private final Paint.FontMetricsInt mTmpFontMetrics = new Paint.FontMetricsInt();
    private short mWidth = -1;
    private short mHeight = -1;
    private Drawable drawable;
    private int srcWidth;
    private int srcHeight;

    TarnImageSpan(TextView textView, @DrawableRes int id, int tint)
    {
        drawable = ContextCompat.getDrawable(textView.getContext(), id);
        if (drawable != null)
        {
            srcWidth = drawable.getIntrinsicWidth();
            srcHeight = drawable.getIntrinsicHeight();

            if (tint != 0)
            {
                drawable = DrawableCompat.wrap(drawable).mutate();
                DrawableCompat.setTint(drawable, tint);
            }
        }
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm)
    {
        paint.getFontMetricsInt(mTmpFontMetrics);
        final int fontHeight = Math.abs(mTmpFontMetrics.descent - mTmpFontMetrics.ascent);

        float ratio = fontHeight * 1.0f / srcHeight;
        mHeight = (short)(srcHeight * ratio);
        mWidth = (short)(srcWidth * ratio);

        if (fm != null)
        {
            fm.ascent = mTmpFontMetrics.ascent;
            fm.descent = mTmpFontMetrics.descent;
            fm.top = mTmpFontMetrics.top;
            fm.bottom = mTmpFontMetrics.bottom;
        }

        return mWidth;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint)
    {
        if (drawable == null) return;

        drawable.setBounds(0, 0, mWidth, mHeight);

        canvas.save();
        int transY = (bottom - drawable.getBounds().bottom) / 2;
        canvas.translate(x, transY);
        drawable.draw(canvas);
        canvas.restore();
    }
}