package tarn.pantip.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.util.GlideApp;

/**
 * User: tarn
 * Date: 4/14/13 9:13 PM
 */
public class BarChart extends RelativeLayout
{
    private ImageView imageView;
    private TextView textView;
    private TextView scoreView;
    private ProgressBar progress;

    public BarChart(Context context)
    {
        this(context, null);
    }

    public BarChart(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public BarChart(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context)
    {
        LayoutInflater.from(context).inflate(R.layout.barchart, this);
        imageView = findViewById(R.id.image);
        imageView.setBackgroundColor(Pantip.barBackgroundColor);
        textView = findViewById(R.id.text);
        textView.setTextColor(Pantip.textColor);
        scoreView = findViewById(R.id.score);
        scoreView.setTextColor(Pantip.textColor);
        progress = findViewById(android.R.id.progress);
    }

    public void setData(int n, String title, int maxVote, int score, String image)
    {
        textView.setText(title);
        scoreView.setText(String.valueOf(score));
        progress.setProgressDrawable(getProgressDrawable(Pantip.barColors[n % Pantip.barColors.length]));
        progress.setMax(maxVote);
        progress.setProgress(score);
        if (image == null) imageView.setVisibility(View.GONE);
        else if (image.length() > 0) GlideApp.with(getContext()).load(image).into(imageView);
    }

    private LayerDrawable getProgressDrawable(int color)
    {
        GradientDrawable background = new GradientDrawable();
        //background.setSize(MATCH_PARENT, Utils.toPixels(20));
        background.setColor(Pantip.barBackgroundColor);
        GradientDrawable secondaryProgress = new GradientDrawable();
        secondaryProgress.setColor(Pantip.textColorHint);
        GradientDrawable progress = new GradientDrawable();
        progress.setColor(color);
        Drawable[] layers = new Drawable[]
                {
                        background,
                        new ScaleDrawable(secondaryProgress, 3, 1, -1),
                        new ScaleDrawable(progress, 3, 1, -1),
                };
        LayerDrawable layer = new LayerDrawable(layers);
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.secondaryProgress);
        layer.setId(2, android.R.id.progress);
        return layer;
    }
}