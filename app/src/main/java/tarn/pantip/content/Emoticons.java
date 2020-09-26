package tarn.pantip.content;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Created by Tarn on 6 August 2017
 */
public class Emoticons
{
    public static EmoticonCache cache;

    public static BitmapDrawable get(AppCompatActivity activity, String url)
    {
        if (!isEmoticon(url)) return null;

        int i = url.lastIndexOf('/');
        String fileName = url.substring(i + 1);
        BitmapDrawable drawable = cache.get(fileName);
        if (drawable != null) return drawable;

        try (InputStream in = activity.getAssets().open("emoticons/" + fileName))
        {
            drawable = decodeStream(activity.getResources(), in, url);
            if (drawable != null) cache.put(fileName, drawable);
            return drawable;
        }
        catch (FileNotFoundException e)
        {
            try
            {
                File file = new File(Utils.getFileDir(Pantip.getContext()), "emoticons/" + fileName);
                if (file.exists() && file.length() > 0)
                {
                    try (FileInputStream in = new FileInputStream(file))
                    {
                        drawable = decodeStream(activity.getResources(), in, url);
                        if (drawable != null) cache.put(fileName, drawable);
                        return drawable;
                    }
                }
                else if (ContextCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    Utils.showAppSettings(activity);
                }
                else
                {
                    RxUtils.observe(emitter -> {
                        Http.download(url, file);
                        emitter.onComplete();
                    }).subscribe();
                }
            }
            catch (Exception e1)
            {
                L.e(e1);
            }
        }
        catch (Exception e)
        {
            L.e(e);
        }
        return null;
    }

    static boolean isEmoticon(String url)
    {
        return !StringUtils.isBlank(url)
                && (url.startsWith("https://ptcdn.info/emoticons/")
                || url.startsWith("https://ptcdn.info/toy/"));
    }

    private static BitmapDrawable decodeStream(Resources res, InputStream in, String url)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
        if (bitmap == null) return null;

        BitmapDrawable drawable = new BitmapDrawable(res, bitmap);
        setBounds(drawable, url, res);
        return drawable;
    }

    private static void setBounds(BitmapDrawable drawable, String url, Resources res)
    {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        float ratio;
        if (url.contains("/toy/"))
        {
            ratio = res.getDisplayMetrics().scaledDensity;
        }
        else if (url.contains("/emoticon-"))
        {
            ratio = 2f;
        }
        else if (url.contains("/mao_investor/"))
        {
            ratio = 1.5f;
        }
        else if (url.contains("/smiley/"))
        {
            ratio = 0.5f;
        }
        else
        {
            ratio = 0.65f;
        }

        width = (int)(width * ratio);
        height = (int)(height * ratio);
        drawable.setBounds(0, 0, width, height);
    }
}
