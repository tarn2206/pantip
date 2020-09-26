package tarn.pantip.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StyleableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.util.Utils;

import static android.widget.FrameLayout.LayoutParams.WRAP_CONTENT;

/**
 * Created by Tarn on 26 January 2017 16:33
 */

public class FeedbackButton extends FrameLayout
{
    private TextView textView;
    private ColorStateList textColor;
    private Drawable icon;
    private int size;
    private boolean tint;

    public FeedbackButton(Context context)
    {
        super(context);
        init(context, null);
    }

    public FeedbackButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public FeedbackButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet set)
    {
        setClickable(true);
        textView = new TextView(context);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setMaxLines(1);
        textView.setTextSize(Pantip.textSize - 4);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        addView(textView, params);
        size = Utils.getTextBounds(textView, "Z").height();
        int padding = (int)(size * .5f);
        textView.setPadding(padding, padding, padding, padding);
        textView.setCompoundDrawablePadding((int)(size * .3f));
        size += size;

        int[] attrs = { android.R.attr.text, R.attr.feedbackButtonBg, R.attr.feedbackButtonColor };
        TypedArray a = context.obtainStyledAttributes(set, attrs);
        try
        {
            @StyleableRes int index = 0;
            textView.setText(a.getText(index++));
            setBackground(a.getDrawable(index++));
            textView.setTextColor(textColor = a.getColorStateList(index));
        }
        catch (Exception e)
        {
            L.e(e);
        }
        a.recycle();

        try
        {
            a = getContext().obtainStyledAttributes(set, R.styleable.FeedbackButton);
            int resId = a.getResourceId(R.styleable.FeedbackButton_icon, 0);
            a.recycle();
            if (resId > 0) setIconResource(resId, true);
        }
        catch (Exception e)
        {
            L.e(e);
        }
    }

    public void setIconResource(@DrawableRes int resId, boolean tint)
    {
        this.tint = tint;
        icon = AppCompatResources.getDrawable(getContext(), resId);
        if (icon != null)
        {
            icon = DrawableCompat.wrap(icon).mutate();
            icon.setBounds(0, 0, size, size);
            if (tint)
            {
                int color = textColor.getColorForState(getDrawableState(), 0);
                DrawableCompat.setTint(icon, color);
            }
        }
        textView.setCompoundDrawables(icon, null, null, null);
    }

    public CharSequence getText()
    {
        return textView.getText();
    }

    public void setText(CharSequence text)
    {
        textView.setText(text);
    }

    @Override
    public void setSelected(boolean selected)
    {
        textView.setSelected(selected);
        if (icon != null && tint)
        {
            int color = textColor.getColorForState(textView.getDrawableState(), 0);
            DrawableCompat.setTint(icon, color);
        }
    }
}