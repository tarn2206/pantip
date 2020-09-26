package tarn.pantip.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import tarn.pantip.Pantip;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 13 February 2017
 */

public class Legend extends LinearLayout
{
    private final View legend;
    private final TextView textView;

    public Legend(Context context)
    {
        super(context);
        legend = new View(context);
        addView(legend, new LayoutParams(Utils.toPixels(28), Utils.toPixels(15)));
        ((LayoutParams)legend.getLayoutParams()).gravity = Gravity.CENTER_VERTICAL;

        textView = new TextView(context);
        textView.setTextColor(Pantip.textColor);
        textView.setPadding(Utils.toPixels(4), 0, Utils.toPixels(16), 0);
        addView(textView);
    }

    public void setBackgroundColor(int color)
    {
        legend.setBackgroundColor(color);
    }

    public void setText(String text)
    {
        textView.setText(text);
    }
}