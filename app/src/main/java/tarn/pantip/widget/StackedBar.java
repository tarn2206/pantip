package tarn.pantip.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import tarn.pantip.Pantip;

/**
 * Created by Tarn on 13 February 2017
 */

public class StackedBar extends LinearLayout implements ViewTreeObserver.OnGlobalLayoutListener
{
    private int[] values;
    private float max;

    public StackedBar(Context context)
    {
        super(context);
        setBackgroundColor(Pantip.barBackgroundColor);
    }

    public void setData(int[] values, float max)
    {
        this.values = values;
        this.max = max;

        if (values != null) getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout()
    {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        int maxWidth = getWidth();
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] <= 0) continue;
            TextView view = new TextView(getContext());
            view.setBackgroundColor(Pantip.barColors[i % Pantip.barColors.length]);
            view.setText(String.valueOf(values[i]));
            view.setTextColor(Color.BLACK);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            view.setGravity(Gravity.CENTER);

            int width = (int)(values[i] * maxWidth / max);
            LayoutParams layout = new LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
            addView(view, layout);
        }
    }
}