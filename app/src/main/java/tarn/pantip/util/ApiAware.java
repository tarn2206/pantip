package tarn.pantip.util;

import android.app.ActivityManager;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.apache.commons.lang3.StringUtils;

import tarn.pantip.R;

/**
 * Created by Tarn on 29/10/2015.
 */
public final class ApiAware
{
    public static void setTaskDescription(AppCompatActivity activity, String label)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;

        int bgColor = ContextCompat.getColor(activity, R.color.colorPrimary);
        if (StringUtils.isBlank(label)) label = activity.getResources().getString(R.string.app_label);
        try
        {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(label, R.drawable.ic_logo, bgColor);
            activity.setTaskDescription(taskDescription);
        }
        catch (Throwable e)
        {/*ignored*/}
    }

    public static void TranslucentSystemUI(Window window)
    {
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    public static void displaySystemUI(Window window, boolean show)
    {
        // see http://developer.android.com/training/system-ui/immersive.html
        View decorView = window.getDecorView();
        if (show)
        {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        else
        {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}