package tarn.pantip.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

/**
 * User: Tarn
 * Date: 8/30/13 10:51 PM
 */
public class StyledAttributes
{
    private final Context context;
    private int index = 0;
    private final int[] attrs;
    private final Resources.Theme theme;
    private final int themeId;

    public StyledAttributes(Context context, int themeId, int... attrs)
    {
        this.context = context;
        theme = context.getTheme();
        this.themeId = themeId;
        this.attrs = attrs;
    }

    public int getColor()
    {
        return ContextCompat.getColor(context, getResourceId());
    }

    public Drawable getDrawable()
    {
        return ContextCompat.getDrawable(context, getResourceId());
    }

    public int getDimensionPixel()
    {
        return context.getResources().getDimensionPixelSize(getResourceId());
    }

    public int getResourceId()
    {
        int i = index++;
        TypedArray array = theme.obtainStyledAttributes(themeId, new int[] { attrs[i] });
        return array.getResourceId(0, 0);
    }
}