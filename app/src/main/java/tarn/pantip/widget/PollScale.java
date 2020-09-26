package tarn.pantip.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.model.Choice;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 06 February 2017
 */

public class PollScale extends LinearLayout
{
    private RadioGroup scores;
    private TextView minText;
    private TextView maxText;
    private ColorStateList textColor;

    public PollScale(Context context)
    {
        this(context, null);
    }

    public PollScale(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public PollScale(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)
    {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.poll_scale, this);
        scores = findViewById(R.id.scores);
        minText = findViewById(R.id.min_text);
        maxText = findViewById(R.id.max_text);

        int[][] states = new int[][] { new int[] { android.R.attr.state_checked }, StateSet.WILD_CARD };
        int[] colors = new int[] { Pantip.backgroundSecondary, Pantip.textColor };
        textColor = new ColorStateList(states, colors);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setChoices(String minText, String maxText, List<Choice> choices, boolean require)
    {
        this.minText.setText(minText);
        this.maxText.setText(maxText);
        scores.removeAllViews();
        for (Choice choice : choices)
        {
            final RadioButton score = new RadioButton(getContext());
            score.setId(View.generateViewId());
            score.setButtonDrawable(null);
            score.setBackground(getScoreBackground());
            score.setTag(choice.id);
            score.setTextColor(textColor);
            score.setText(choice.text);
            score.setTextSize(Pantip.textSize);
            score.setGravity(Gravity.CENTER);
            score.setMaxLines(1);
            score.setChecked(choice.selected);
            if (!require)
            {
                score.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        if (score.isChecked())
                        {
                            scores.clearCheck();
                            return true;
                        }
                    }
                    return false;
                });
            }
            scores.addView(score);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.weight = 1;
            layout.leftMargin = layout.rightMargin = Utils.toPixels(2);
            score.setLayoutParams(layout);
        }
    }

    private StateListDrawable getScoreBackground()
    {
        StateListDrawable background = new StateListDrawable();
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(Utils.toPixels(5));
        d.setColor(Pantip.colorAccent);
        background.addState(new int[] { android.R.attr.state_checked }, d);

        d = new GradientDrawable();
        d.setCornerRadius(Utils.toPixels(5));
        d.setStroke(Utils.toPixels(1), Pantip.textColorHint);

        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(Utils.toPixels(5));
        mask.setColor(Color.WHITE);
        RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(Pantip.colorAccent), d, mask);
        background.addState(StateSet.WILD_CARD, ripple);

        return background;
    }

    public void setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener listener)
    {
        scores.setOnCheckedChangeListener(listener);
    }

    public int getCheckedRadioButtonId()
    {
        return scores.getCheckedRadioButtonId();
    }

    public String getSelectedId()
    {
        int id = scores.getCheckedRadioButtonId();
        return id == NO_ID ? null : (String)scores.findViewById(id).getTag();
    }
}