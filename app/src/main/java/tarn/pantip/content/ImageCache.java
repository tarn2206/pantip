package tarn.pantip.content;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;

public class ImageCache
{
    private final BitmapCache memoryCache;

    public ImageCache(Context context)
    {
        MemorySizeCalculator memorySizeCalculator = new MemorySizeCalculator.Builder(context).build();
        memoryCache = new BitmapCache(memorySizeCalculator.getMemoryCacheSize());
    }

    public void add(String key, Bitmap bitmap)
    {
        memoryCache.put(key, bitmap);
    }

    public Bitmap get(String key)
    {
        return memoryCache.get(key);
    }

    private static class BitmapCache extends LruCache<String, Bitmap>
    {
        BitmapCache(int maxSize)
        {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap bitmap)
        {
            if (bitmap.isRecycled())
            {
                throw new IllegalStateException("Cannot obtain size for recycled Bitmap: " + bitmap
                        + "[" + bitmap.getWidth() + "x" + bitmap.getHeight() + "] " + bitmap.getConfig());
            }
            // Workaround for KitKat initial release NPE in Bitmap, fixed in MR1. See issue #148.
            try
            {
                return bitmap.getAllocationByteCount();
            }
            catch (@SuppressWarnings("PMD.AvoidCatchingNPE") NullPointerException e)
            {
                // Do nothing.
            }
            return bitmap.getByteCount();
        }
    }
}
