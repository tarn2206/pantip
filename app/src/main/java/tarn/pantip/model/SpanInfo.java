package tarn.pantip.model;

import androidx.annotation.ColorInt;

/**
 * Created by Tarn on 10 September 2016
 */
public class SpanInfo
{
    public int start;
    public int end;
    @ColorInt
    public int color;

    public SpanInfo()
    { }

    public SpanInfo(int start, int end, @ColorInt int color)
    {
        this.start = start;
        this.end = end;
        this.color = color;
    }
}