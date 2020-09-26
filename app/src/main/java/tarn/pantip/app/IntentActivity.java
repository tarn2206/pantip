package tarn.pantip.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.util.Utils;

/**
 * User: tarn
 * Date: 2/20/13 8:52 PM
 */
public class IntentActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null)
        {
            Uri uri = intent.getData();
            if (uri != null)
            {
                Pantip.initTheme(true);
                openUrl(this, uri.toString());
            }
        }
        finish();
    }

    public static boolean openUrl(Context context, String url)
    {
        if (url == null)
        {
            return false;
        }
        int i = url.indexOf("pantip.com");
        if (i == -1)
        {
            return false;
        }
        String[] a = url.substring(i + 11).split("/");
        if (a.length < 2)
        {
            return false;
        }

        Intent intent;
        String type = a[0].toUpperCase();
        if ("TOPIC".equals(type))
        {
            long id;
            try
            {
                int x = 0;
                while (x < a[1].length() && Character.isDigit(a[1].charAt(x))) x++;
                id = Long.parseLong(a[1].substring(0, x));
            }
            catch (Exception e)
            {
                L.e(e, url);
                Utils.showToast(context, e.getMessage());
                return false;
            }
            intent = new Intent(context, TopicActivity.class);
            intent.putExtra("id", id);
            if (a.length > 2 && a[2].startsWith("comment"))
            {
                intent.putExtra("goTo", a[2].substring(7));
            }
        }
        else if ("FORUM".equals(type) || "TAG".equals(type))
        {
            intent = new Intent(context, MainActivity.class);
            intent.putExtra("view", type).putExtra("value", a[1]);
        }
        else
        {
            return false;
        }

        intent.setAction(Intent.ACTION_VIEW);
        context.startActivity(intent);
        return true;
    }
}