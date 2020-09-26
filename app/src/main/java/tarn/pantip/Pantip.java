package tarn.pantip;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.UnknownHostException;
import java.util.Calendar;

import tarn.pantip.app.MainActivity;
import tarn.pantip.content.EmoticonCache;
import tarn.pantip.content.Emoticons;
import tarn.pantip.content.HttpException;
import tarn.pantip.content.ImageCache;
import tarn.pantip.model.DetailException;
import tarn.pantip.model.Size;
import tarn.pantip.model.User;
import tarn.pantip.store.DataStore;
import tarn.pantip.store.FileStore;
import tarn.pantip.store.PersistentCookieStore;
import tarn.pantip.util.StyledAttributes;
import tarn.pantip.util.Utils;

/**
 * User: tarn
 * Date: 1/16/13 10:40 PM
 */
public class Pantip extends Application
{
    public static Pantip instance;
    private static PersistentCookieStore cookieStore;
    public static MainActivity main;
    public static Context context;
    public static ImageCache imageCache;
    public static int displayWidth;
    public static Size imagePlaceholderSize;
    public static int textSize;
    public static int spacer;
    public static boolean loggedOn;
    public static boolean xLarge;
    public static final float readAlpha = 0.8f;

    public static int nightMode;
    public static int currentTheme;
    public static boolean isNightMode;

    public static int colorAccent;
    public static int textColor;
    public static int textColorSecondary;
    public static int textColorTertiary;
    public static int textColorHint;
    public static int linkColor;

    public static int selectableItemBackground;
    public static int selectableSecondaryBackground;
    public static int backgroundSecondary;
    public static int authorColor;
    public static int topicTitleColor;
    public static int[] topicBackground;
    public static int[] commentBackground;
    public static int[] replyBackground;
    public static int barBackgroundColor;
    public static int pageMarginDrawable;
    public static int feedbackDividerColor;
    public static int feedbackDividerReplyColor;
    public static int dangerColor;
    public static int actionBarSize;
    public static int overlayColor;
    public static int[] barColors;

    public static User currentUser;

    private DataStore dataStore;
    public static final int REFRESH_TIME = 300000; // 5 minutes

    public Pantip()
    {
        super();
        instance = this;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        //Utils.printSignature();

        context = getApplicationContext();
        imageCache = new ImageCache(context);
        Resources resources = context.getResources();

        dataStore = new DataStore(context);

        initCookieStore();
        loadPreferences();

        barColors = new int[] {
            ContextCompat.getColor(instance, R.color.bar_color1),
            ContextCompat.getColor(instance, R.color.bar_color2),
            ContextCompat.getColor(instance, R.color.bar_color3),
            ContextCompat.getColor(instance, R.color.bar_color4),
            ContextCompat.getColor(instance, R.color.bar_color5),
            ContextCompat.getColor(instance, R.color.bar_color6),
            ContextCompat.getColor(instance, R.color.bar_color7),
            ContextCompat.getColor(instance, R.color.bar_color8)
        };

        DisplayMetrics display = resources.getDisplayMetrics();
        displayWidth = Math.min(display.widthPixels, display.heightPixels);
        //Log.d(Tag, "image width: " + imageWidth);
        imagePlaceholderSize = new Size(displayWidth / 3, displayWidth / 5);

        Pantip.spacer = Utils.getDimension(R.dimen.spacer);

        Emoticons.cache = new EmoticonCache(this);

        xLarge = getResources().getBoolean(R.bool.xlarge);

        new FileStore().removeOldFiles();
    }

    public static Context getContext()
    {
        return context;
    }

    public static DataStore getDataStore()
    {
        return instance.dataStore;
    }

    public static SharedPreferences getSharedPreferences()
    {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void loadPreferences()
    {
        SharedPreferences preferences = getSharedPreferences();
        String userName = preferences.getString("user_name", null);
        loggedOn = userName != null;
        if (loggedOn)
        {
            currentUser = new User();
            currentUser.name = userName;
            currentUser.id = preferences.getInt("mid", 0);
            currentUser.avatar = preferences.getString("avatar", null);
        }

        textSize = Integer.parseInt(preferences.getString("font_size", "16"));

        nightMode = Integer.parseInt(preferences.getString("night_mode2", "2"));
        initTheme(false);
    }

    public static void initTheme(boolean detectChanges)
    {
        boolean isNight;
        if (nightMode == 2)
        {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            isNight = hour >= 18 || hour < 6;
            if (detectChanges && isNight == isNightMode) return;
        }
        else if (detectChanges) return;
        else isNight = nightMode == 1;
        loadTheme(isNight);
    }

    private static void loadTheme(boolean nightMode)
    {
        isNightMode = nightMode;
        currentTheme = isNightMode ? R.style.AppTheme : R.style.AppTheme_Light;
        instance.setTheme(currentTheme);

        StyledAttributes styled = new StyledAttributes(context, currentTheme, R.attr.colorAccent,
                android.R.attr.textColor, android.R.attr.textColorSecondary, android.R.attr.textColorTertiary,
                android.R.attr.textColorLink, android.R.attr.textColorHint,

                R.attr.authorColor, R.attr.topicTitleColor, R.attr.danger, android.R.attr.actionBarSize, R.attr.overlay_color,

                R.attr.secondaryBackground, R.attr.topicBgTop, R.attr.topicBgMiddle, R.attr.topicBgBottom, R.attr.commentBgTop, R.attr.commentBgMiddle, R.attr.commentBgBottom, R.attr.replyBgTop, R.attr.replyBgMiddle, R.attr.replyBgBottom,

                R.attr.feedbackDivider, R.attr.feedbackDivider2, R.attr.barBackground, R.attr.pagerMarginDrawable, android.R.attr.selectableItemBackground, R.attr.selectableSecondaryBackground);
        colorAccent = styled.getColor();
        textColor = styled.getColor();
        textColorSecondary = styled.getColor();
        textColorTertiary = styled.getColor();
        linkColor = styled.getColor();
        textColorHint = styled.getColor();

        authorColor = styled.getColor();
        topicTitleColor = styled.getColor();
        dangerColor = styled.getColor();
        actionBarSize = styled.getDimensionPixel();
        overlayColor = Utils.setAlpha(styled.getColor(), 0x99);

        backgroundSecondary = styled.getColor();
        topicBackground = new int[] { styled.getResourceId(), styled.getResourceId(), styled.getResourceId() };
        commentBackground = new int[] { styled.getResourceId(), styled.getResourceId(), styled.getResourceId() };
        replyBackground = new int[] { styled.getResourceId(), styled.getResourceId(), styled.getResourceId() };

        feedbackDividerColor = styled.getColor();
        feedbackDividerReplyColor = styled.getColor();
        barBackgroundColor = styled.getColor();
        pageMarginDrawable = styled.getResourceId();
        selectableItemBackground = styled.getResourceId();
        selectableSecondaryBackground = styled.getResourceId();
    }

    public static void handleException(Throwable tr)
    {
        handleException(instance, tr);
    }

    public static void handleException(Context context, Throwable tr)
    {
        L.e(tr);
        if (context == null) context = instance;

        String text;
        int duration = Toast.LENGTH_SHORT;
        if (tr instanceof HttpException)
        {
            text = ((HttpException)tr).text();
            duration = Toast.LENGTH_LONG;
        }
        else if (tr instanceof DetailException) text = ((DetailException)tr).getCauseMessage();
        else if (tr instanceof NullPointerException) text = "Null pointer exception";
        else if (tr instanceof UnknownHostException) text = tr.getMessage() + " can not be resolved";
        else
        {
            text = tr.getMessage();
            if (text == null || text.equals("null")) text = tr.getClass().getName();
        }
        new Handler(Looper.getMainLooper()).postDelayed(new ShowToast(context, text, duration, false), 200);
    }

    static class ShowToast implements Runnable
    {
        private final Context context;
        private final CharSequence text;
        private final int duration;
        private final boolean center;

        ShowToast(Context context, CharSequence text, int duration, boolean center)
        {
            this.context = context;
            this.text = text;
            this.duration = duration;
            this.center = center;
        }

        @Override
        public void run()
        {
            Utils.showToast(context, text, duration, center);
        }
    }

    private void initCookieStore()
    {
        cookieStore = new PersistentCookieStore(this);
        CookieManager cookieManager = new CookieManager(cookieStore, null);
        CookieHandler.setDefault(cookieManager);
    }

    public static boolean hasPantipSession()
    {
        for (HttpCookie cookie : cookieStore.getCookies())
        {
            if ("pantip_sessions".equals(cookie.getName()))
            {
                return true;
            }
        }
        return false;
    }

    public static void clearCookies()
    {
        cookieStore.removeAll();
    }

    public static void saveCookies()
    {
        cookieStore.save();
    }

    public static void invalidate()
    {
        getSharedPreferences().edit()
                .remove("user_name").remove("mid").remove("avatar")
                .apply();
        currentUser = null;
        loggedOn = false;
    }
}