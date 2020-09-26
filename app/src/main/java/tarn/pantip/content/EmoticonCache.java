package tarn.pantip.content;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

/**
 * Created by Tarn on 08 March 2017
 */

public class EmoticonCache extends LruCache<String, BitmapDrawable> implements ComponentCallbacks2
{
    public EmoticonCache(Context context)
    {
        super(2097152);
        context.registerComponentCallbacks(this);
    }

    @Override
    protected int sizeOf(@NonNull String key, @NonNull BitmapDrawable drawable)
    {
        Bitmap bitmap = drawable.getBitmap();
        return bitmap == null ? 0 : bitmap.getByteCount();
    }

    @Override
    public void onTrimMemory(int level)
    {
        if (level >= 60)
        {
            evictAll();
        }
        else if (level >= 20)
        {
            trimToSize(size() / 2);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    { }

    @Override
    public void onLowMemory()
    {
        evictAll();
    }
}