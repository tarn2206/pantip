package tarn.pantip.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * User: Tarn
 * Date: 5/20/13 11:56 AM
 */
public class NoPressedLayout extends LinearLayout
{
    public NoPressedLayout(Context context)
    {
        super(context);
    }

    public NoPressedLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public NoPressedLayout(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void setPressed(boolean pressed)
    {
        // prevent pressed
    }
}
