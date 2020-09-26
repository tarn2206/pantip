package tarn.pantip.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import tarn.pantip.R;

/**
 * Created by Tarn on 29 January 2017
 */

public class FilterButton extends AppCompatImageView
{
    private int tintColor;

    public FilterButton(Context context)
    {
        super(context);
        init(context);
    }

    public FilterButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public FilterButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)
    {
        tintColor = ContextCompat.getColor(context, R.color.filter_tint);
        setColorFilter(tintColor);
    }

    @Override
    public void setSelected(boolean selected)
    {
        super.setSelected(selected);
        setColorFilter(selected ? 0 : tintColor);
    }
}