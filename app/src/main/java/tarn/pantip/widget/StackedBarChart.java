package tarn.pantip.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.model.Choice;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 13 February 2017
 */

public class StackedBarChart extends LinearLayout
{
    private final FlexboxLayout legends;
    private static final int BAR_HEIGHT = Utils.toPixels(20);

    public StackedBarChart(Context context)
    {
        super(context);
        setOrientation(VERTICAL);

        legends = new FlexboxLayout(context);
        legends.setFlexWrap(FlexWrap.WRAP);
        addView(legends);
    }

    public void setBottomMargin(int margin)
    {
        LayoutParams layout = (LayoutParams)legends.getLayoutParams();
        layout.bottomMargin = margin;
    }

    public void setData(final List<Choice> choices, final int maxVote, String[] legend, final int verticalMargin)
    {
        if (legend != null)
        {
            for (int i = 0; i < legend.length; i++)
            {
                Legend view = new Legend(getContext());
                view.setText(legend[i]);
                view.setBackgroundColor(Pantip.barColors[i % Pantip.barColors.length]);
                legends.addView(view);
            }
        }
        if (choices == null) return;
        for (Choice choice : choices)
        {
            TextView textView = new TextView(getContext());
            textView.setText(choice.text);
            textView.setTextColor(Pantip.textColor);
            addView(textView);

            StackedBar bar = new StackedBar(getContext());
            addView(bar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, BAR_HEIGHT));
            bar.setData(choice.values, maxVote);

            if (StringUtils.isNotBlank(choice.image))
            {
                ImageView imageView = new ImageView(getContext());
                addView(imageView, new LayoutParams(Utils.getDisplaySize().x / 2, ViewGroup.LayoutParams.WRAP_CONTENT));
                LayoutParams params = (LayoutParams)imageView.getLayoutParams();
                params.gravity = Gravity.CENTER_HORIZONTAL;
                params.topMargin = params.bottomMargin = verticalMargin / 2;
                GlideApp.with(getContext()).load(choice.image).into(imageView);
            }
        }
    }
}