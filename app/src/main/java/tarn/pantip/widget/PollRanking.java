package tarn.pantip.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatSpinner;

import tarn.pantip.Pantip;
import tarn.pantip.model.Choice;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 09 February 2017
 */

public class PollRanking extends RelativeLayout
{
    private final AppCompatSpinner spinner;
    private final TextView textView;

    public PollRanking(Context context)
    {
        super(context);

        spinner = new AppCompatSpinner(context);
        spinner.setId(View.generateViewId());
        addView(spinner);

        textView = new TextView(context);
        textView.setId(View.generateViewId());
        textView.setTextColor(Pantip.textColor);
        textView.setTextSize(Pantip.textSize);
        addView(textView);
    }

    public void setChoice(Choice choice)
    {
        if (choice.choices == null) return;
        spinner.setAdapter(new PollSpinner.ChoiceAdapter(getContext(), choice.choices));
        for (int i = 0; i < choice.choices.size(); i++)
        {
            Choice c = choice.choices.get(i);
            if (c.selected) spinner.setSelection(i);
        }
        textView.setText(choice.text);
        Utils.observeGlobalLayout(textView, view -> {
            LayoutParams params = (LayoutParams)textView.getLayoutParams();
            if (textView.getLineCount() > 2)
            {
                params.addRule(BELOW, spinner.getId());
            }
            else
            {
                params.leftMargin = Utils.toPixels(4);
                params.addRule(RIGHT_OF, spinner.getId());
                params.addRule(ALIGN_BASELINE, spinner.getId());
            }
            requestLayout();
        });
        setImage(choice.image);
    }

    private void setImage(String url)
    {
        if (url == null) return;

        ImageView imageView = new ImageView(getContext());
        LayoutParams params = new LayoutParams(Utils.getDisplaySize().x / 2, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(BELOW, spinner.getId());
        params.addRule(CENTER_HORIZONTAL);
        params.bottomMargin = Utils.toPixels(12);
        addView(imageView, params);
        GlideApp.with(getContext()).load(url).into(imageView);
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener)
    {
        spinner.setOnItemSelectedListener(listener);
    }

    public int getSelectedItemPosition()
    {
        return spinner.getSelectedItemPosition();
    }

    public String getSelectedId()
    {
        return ((Choice)spinner.getSelectedItem()).id;
    }
}