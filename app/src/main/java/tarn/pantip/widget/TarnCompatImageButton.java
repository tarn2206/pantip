package tarn.pantip.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;

import tarn.pantip.Pantip;

/**
 * Created by Tarn on 25 January 2017 01:07
 */

public class TarnCompatImageButton extends AppCompatImageButton
{
    public TarnCompatImageButton(Context context)
    {
        super(context);
    }

    public TarnCompatImageButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public TarnCompatImageButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void drawableStateChanged()
    {
        int color;
        if (isEnabled())
        {
            if (isSelected() || isPressed()) color = Pantip.colorAccent;
            else color = Pantip.textColorSecondary;
        }
        else color = Pantip.textColorHint;
        setColorFilter(color, PorterDuff.Mode.SRC_IN);
        super.drawableStateChanged();
    }
}