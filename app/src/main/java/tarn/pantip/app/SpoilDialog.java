package tarn.pantip.app;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialog;

import java.util.ArrayList;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.model.Story;
import tarn.pantip.model.StoryType;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.PhotoLayout;

/**
 * User: Tarn
 * Date: 8/13/13 4:29 PM
 */
public class SpoilDialog extends AppCompatDialog
{
    private SpoilDialog(BaseActivity activity, long topicId, List<Story> spoil)
    {
        super(activity);

        setContentView(R.layout.dialog_spoil);
        ViewGroup container = findViewById(R.id.spoil_container);
        assert container != null;
        int padding = container.getPaddingTop();
        View previous = null;
        int maxLength = 0;
        List<Story> imageList = new ArrayList<>();
        for (Story story : spoil)
        {
            if (story.type == StoryType.Text)
            {
                TextView textView = wrapText(activity, "", Pantip.textSize, Pantip.textColor);
                textView.setText(story.getSpannable(activity, textView));
                container.addView(textView);
                Utils.addLinkMovementMethod(textView);
                if (maxLength < story.text.length()) maxLength = story.text.length();
                if (container.getChildCount() == 1)
                {
                    textView.setPadding(0, padding, 0, 0);
                    container.setPadding(container.getPaddingLeft(), 0, container.getPaddingRight(), container.getPaddingBottom());
                }
                previous = textView;
            }
            else if (story.type == StoryType.Image)
            {
                PhotoLayout imageView = new PhotoLayout(activity, story, topicId, imageList, imageList.size(), this);
                imageList.add(story);
                container.addView(imageView);
                imageView.loadImage();
                if (previous instanceof PhotoLayout)
                    previous.setPadding(0, 0, 0, padding);
                previous = imageView;
            }
            else if (story.type == StoryType.YouTube)
            {
                PhotoLayout imageView = new PhotoLayout(activity, story, 0, null, 0, this);
                container.addView(imageView);
                imageView.loadImage();
                if (previous instanceof PhotoLayout)
                    previous.setPadding(0, 0, 0, padding);
                previous = imageView;
            }
        }
        if (maxLength > 75) setWidth(Pantip.displayWidth);
    }

    public void setWidth(int width)
    {
        setWidth(this, width);
    }

    static void setWidth(Dialog dialog, int width)
    {
        Window window = dialog.getWindow();
        if (window == null) return;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.copyFrom(window.getAttributes());
        if (params.width < width)
        {
            params.width = width;
            window.setAttributes(params);
        }
    }

    public static SpoilDialog show(BaseActivity context, long topicId, List<Story> spoil)
    {
        final SpoilDialog dialog = new SpoilDialog(context, topicId, spoil);
        dialog.show();
        return dialog;
    }

    private static TextView wrapText(Context context, CharSequence text, int size, int foreColor)
    {
        TextView textView = new TextView(context);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(layout);
        textView.setText(text);
        textView.setTextColor(foreColor);
        textView.setLinkTextColor(Pantip.linkColor);
        textView.setTextSize(size);
        textView.setTextIsSelectable(true);
        return textView;
    }
}