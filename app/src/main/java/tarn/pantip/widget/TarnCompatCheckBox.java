package tarn.pantip.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.StateSet;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatCheckBox;

import tarn.pantip.R;

/**
 * Created by Tarn on 30 January 2017
 */

public class TarnCompatCheckBox extends AppCompatCheckBox
{
    public TarnCompatCheckBox(Context context)
    {
        this(context, null);
    }

    public TarnCompatCheckBox(Context context, AttributeSet attrs)
    {
        this(context, attrs, com.google.android.material.R.attr.checkboxStyle);
    }

    public TarnCompatCheckBox(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        setButtonDrawable(createButton(context));
    }

    public static StateListDrawable createButton(@NonNull Context context)
    {
        Drawable checked = AppCompatResources.getDrawable(context, R.drawable.ic_check_circle_white_24dp);
        Drawable normal = AppCompatResources.getDrawable(context, R.drawable.ic_circle_white_24dp);
        StateListDrawable d = new StateListDrawable();
        d.addState(new int[] { android.R.attr.state_checked }, checked);
        d.addState(StateSet.WILD_CARD, normal);
        return d;
    }
}