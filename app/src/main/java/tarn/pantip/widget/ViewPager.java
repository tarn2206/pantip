package tarn.pantip.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import tarn.pantip.L;

/**
 * Created by Tarn on 03 October 2017
 */

public class ViewPager extends androidx.viewpager.widget.ViewPager
{
    public ViewPager(Context context)
    {
        super(context);
    }

    public ViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        try
        {
            return super.onInterceptTouchEvent(ev);
        }
        catch (Exception e)
        {
            L.e(e);
            return true;
        }
    }
}
