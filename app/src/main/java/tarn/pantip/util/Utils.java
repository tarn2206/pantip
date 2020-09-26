package tarn.pantip.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.Html;
import android.text.method.MovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AnimRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Px;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.WebViewActivity;
import tarn.pantip.widget.OnGlobalLayoutListener;

/**
 * User: tarn
 * Date: 1/16/13 1:56 PM
 */
public class Utils
{
    private static final Locale TH = new Locale("th", "TH");

    @Px
    public static int toPixels(int value)
    {
        if (value == 0) return 0;
        DisplayMetrics metrics = Pantip.getContext().getResources().getDisplayMetrics();
        return (int)(value * metrics.density);
    }

    public static int getDimension(@DimenRes int id)
    {
        return Pantip.getContext().getResources().getDimensionPixelSize(id);
    }

    public static float fixTextSize(float size)
    {
        return Math.max(size, 10);
    }

    public static float textSize(float sp)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Pantip.getContext().getResources().getDisplayMetrics());
    }

    public static void recycle(ImageView view)
    {
        if (view == null) return;
        Drawable drawable = view.getDrawable();
        view.setImageDrawable(null);
        view.setVisibility(View.INVISIBLE);
        if (drawable instanceof BitmapDrawable)
        {
            Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
            if (bitmap != null) bitmap.recycle();
        }
    }

    public static void setVisible(View view, boolean visible)
    {
        if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static void showToast(CharSequence text)
    {
        showToast(Pantip.context, text, Toast.LENGTH_SHORT, false);
    }

    public static void showToast(Context context, CharSequence text)
    {
        showToast(context, text, Toast.LENGTH_SHORT, false);
    }

    public static void showToast(Context context, CharSequence text, int duration, boolean center)
    {
        if (context == null) return;
        Toast toast = Toast.makeText(context, text, duration);
        if (center) toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static String trim(String text)
    {
        if (text == null) return null;
        text = text.trim();
        if (text.indexOf((char)160) < 0) return text;

        StringBuilder builder = new StringBuilder(text);
        while (builder.length() > 0 && builder.charAt(0) == 160) builder.deleteCharAt(0);
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == 160) builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public static File getFileDir() throws IOException
    {
        return getFileDir(Pantip.getContext());
    }

    public static File getFileDir(Context context) throws IOException
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable())
        {
            return context.getExternalFilesDir(null);
        }

        File file = new File(context.getFilesDir(), "pantip");
        FileUtils.forceMkdir(file);
        return file;
    }

    public static File getTempDir(Context context) throws IOException
    {
        File dir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable())
            dir = context.getExternalCacheDir();
        if (dir == null) return context.getFilesDir();
        dir = new File(dir.getParentFile(), "temp");
        FileUtils.forceMkdir(dir);
        return dir;
    }

    public static File getAvatarFile()
    {
        return getAvatarFile(Pantip.currentUser.id);
    }

    public static File getAvatarFile(int mid)
    {
        return new File(Pantip.getContext().getFilesDir(), mid + "_avatar.jpg");
    }

    public static void openBrowser(Context context, String url)
    {
        ResolveInfo found = null;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.android.com"));
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : list)
        {
            if (info.activityInfo.packageName.equals("com.android.chrome")
                    || info.activityInfo.packageName.equals("com.android.browser")
                    || info.activityInfo.packageName.equals("com.sec.android.app.sbrowser"))
            {
                found = info;
                break;
            }
        }
        if (found != null)
        {
            intent = pm.getLaunchIntentForPackage(found.activityInfo.packageName);
            if (intent != null)
            {
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                ComponentName comp = new ComponentName(found.activityInfo.packageName, found.activityInfo.name);
                intent.setComponent(comp);
                intent.setData(Uri.parse(url));
                context.startActivity(intent);
                return;
            }
        }

        if (!StringUtils.startsWithIgnoreCase(url, "https://")
                && !StringUtils.startsWithIgnoreCase(url, "http://"))
            url = "http://" + url;
        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
        StringBuilder s = new StringBuilder(url);
        for (ResolveInfo info : list)
        {
            s.append("\n").append(info.activityInfo.packageName).append("/").append(info.activityInfo.name);
        }
    }

    public static void startActivity(Context context, Intent intent)
    {
        if (intent.resolveActivity(context.getPackageManager()) == null) showToast(context, "No apps can perform this action");
        else context.startActivity(intent);
    }

    public static int calcSampleSize(final int width, final int height, int reqWidth, int reqHeight)
    {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth)
        {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth)
            {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static long getDirectorySize(File root)
    {
        if (root == null || !root.exists()) return 0;

        if (!root.isDirectory()) return root.length();

        long sum = 0;
        File[] files = root.listFiles();
        if (files != null)
        {
            for (File x : files)
            {
                if (x.isDirectory()) sum += getDirectorySize(x);
                else sum += x.length();
            }
        }
        //L.d("%s %s", root.getAbsolutePath(), new java.text.DecimalFormat("#,##0").format(sum));
        return sum;
    }

    public static AlertDialog.Builder createDialog(Activity activity)
    {
        int theme = Pantip.isNightMode ? R.style.AppTheme_Dialog : R.style.AppTheme_Light_Dialog;
        return new AlertDialog.Builder(activity, theme).setCancelable(false);
    }

    public static void showOpenSourceLicense(Activity context)
    {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra("url", "file:///android_asset/license.html");
        context.startActivity(intent);
    }

    public static String getRelativeTime(Calendar calendar)
    {
        return getRelativeTime(calendar, "d MMMM HH.mm น.");
    }

    public static String getRelativeTime(Calendar calendar, String shortDate)
    {
        if (calendar == null) return null;
        Calendar now = Calendar.getInstance();
        long seconds = (now.getTimeInMillis() - calendar.getTimeInMillis()) / 1000;
        if (seconds == 0) return "เดี๋ยวนี้";
        if (seconds < 60) return seconds + " วินาทีที่แล้ว";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " นาทีที่แล้ว";

        Calendar yesterday = getYesterday();
        //Log.e("calendar", calendar.getTime() + " => " + now.getTime() + " => " + yesterday.getTime());
        SimpleDateFormat dateFormat;
        if (calendar.after(yesterday))
        {
            long hours = seconds / 3600;
            if (hours <= 8) return hours + " ชั่วโมงที่แล้ว";
            if (now.get(Calendar.DATE) == calendar.get(Calendar.DATE))
                dateFormat = new SimpleDateFormat("HH.mm น.", TH);
            else dateFormat = new SimpleDateFormat("เมื่อวานนี้ HH.mm น.", TH);
        }
        else if (seconds < 345600) // 4 days
            dateFormat = new SimpleDateFormat("EEEE HH.mm น.", TH);
        else
        {
            String pattern = now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) ? shortDate : "d MMM yyyy HH.mm น.";
            dateFormat = new SimpleDateFormat(pattern, TH);
        }
        return dateFormat.format(calendar.getTime());
    }

    private static Calendar getYesterday()
    {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.setTime(today.getTime());
        yesterday.add(Calendar.DATE, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        return yesterday;
    }

    public static void playSound(Context context, @RawRes int id)
    {
        try
        {
            MediaPlayer mp = MediaPlayer.create(context, id);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
        }
        catch (Exception e)
        {
            L.e(e);
        }
    }

    public static void fadeIn(View view)
    {
        startAnimation(view, android.R.anim.fade_in);
        view.setVisibility(View.VISIBLE);
    }

    private static Animation startAnimation(View view, @AnimRes int id)
    {
        if (view == null || view.getContext() == null) return null;
        Animation anim = AnimationUtils.loadAnimation(view.getContext(), id);
        if (anim != null) view.startAnimation(anim);
        return anim;
    }

    public static Point getDisplaySize()
    {
        Point point = new Point();
        WindowManager wm = (WindowManager)Pantip.instance.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null)
        {
            wm.getDefaultDisplay().getSize(point);
        }
        return point;
    }

    public static void alert(Context context, Throwable tr)
    {
        alert(context, tr.getMessage());
    }

    private static void alert(Context context, String message)
    {
        new AlertDialog.Builder(context).setMessage(message).show();
    }

    public static Rect getTextBounds(TextView view, String text)
    {
        Rect bounds = new Rect();
        view.getPaint().getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }

    public static int setAlpha(int color, int alpha)
    {
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static boolean hasPermission(AppCompatActivity activity, String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) return true;
        /*if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
        {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
        }*/
        ActivityCompat.requestPermissions(activity, new String[] { permission }, requestCode);
        return false;
    }

    public static void showAppSettings(final AppCompatActivity activity)
    {
        View view = ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
        Snackbar snackbar = Snackbar.make(view, "Write storage permission denied", Snackbar.LENGTH_LONG)
                                    .setAction("Settings", v -> {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                                        intent.setData(uri);
                                        activity.startActivity(intent);
                                    });
        snackbar.getView().setBackgroundResource(R.color.colorPrimary);
        view = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        if (view instanceof TextView) ((TextView)view).setTextColor(Color.WHITE);
        view = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_action);
        if (view instanceof TextView) ((TextView)view).setTextColor(ContextCompat.getColor(activity, R.color.accent_color_pantip));
        snackbar.show();
    }

    @SuppressWarnings("deprecation")
    public static String fromHtml(String html)
    {
        if (StringUtils.isBlank(html)) return html;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return Html.fromHtml(html).toString();

        return Html.fromHtml(html, 0).toString();
    }

    public static void addLinkMovementMethod(TextView textView)
    {
        MovementMethod m = textView.getMovementMethod();
        if (!(m instanceof MyLinkMovementMethod) && textView.getLinksClickable())
        {
            textView.setMovementMethod(new MyLinkMovementMethod());
        }
    }

    public static String getYouTubeId(String url)
    {
        if (url.contains("/vi/"))
        {
            int i = url.indexOf("/vi/") + 4;
            int j = url.lastIndexOf('/');
            return url.substring(i, j);
        }
        if (url.contains("/watch?"))
        {
            int i = url.indexOf("v=") + 2;
            int j = url.indexOf("&", i);
            if (j == -1) j = url.length();
            return url.substring(i, j);
        }
        if (url.contains("/embed/"))
        {
            int i = url.indexOf("/embed/") + 7;
            int j = url.indexOf("?", i);
            if (j == -1) j = url.length();
            return url.substring(i, j);
        }
        if (url.contains("youtu.be/"))
        {
            int i = url.indexOf("youtu.be/") + 9;
            int j = url.indexOf("?", i);
            if (j == -1) j = url.length();
            return url.substring(i, j);
        }
        return null;
    }

    public static String getMapLocation(String s)
    {
        int i = s.indexOf("q=");
        if (i != -1)
        {
            int j = s.indexOf('&', i);
            if (j == -1) j = s.length();
            return s.substring(i + 2, j);
        }
        i = s.indexOf("/@");
        if (i != -1)
        {
            int j = s.indexOf(',', i);
            if (j != -1) j = s.indexOf(',', j + 1);
            if (j == -1) j = s.length();
            return s.substring(i + 2, j);
        }
        return "";
    }

    public static String formatLocation(String s)
    {
        String[] a = s.split(",");
        String s1 = formatLocation(Double.parseDouble(a[0]), "N", "S");
        String s2 = formatLocation(Double.parseDouble(a[1]), "E", "W");
        return s1 + "+" + s2;
    }

    private static String formatLocation(double coordinate, String pos, String neg)
    {
        String x = coordinate >= 0 ? pos : neg;
        if (coordinate < 0) coordinate = -coordinate;
        StringBuilder s = new StringBuilder();
        int degrees = (int)Math.floor(coordinate);
        s.append(degrees).append('°');
        coordinate -= degrees;
        coordinate *= 60.0;
        int minutes = (int)Math.floor(coordinate);
        s.append(minutes).append('\'');
        coordinate -= minutes;
        coordinate *= 60.0;
        s.append(new DecimalFormat("0.0").format(coordinate)).append('"');
        return s.append(x).toString();
    }

    public static void hideKeyboard(AppCompatActivity activity)
    {
        if (activity == null) return;
        View v = activity.getCurrentFocus();
        if (v != null && v.getWindowToken() != null)
        {
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            v.clearFocus();
        }
        else activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public static void observeGlobalLayout(final View view, final OnGlobalLayoutListener listener)
    {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                listener.onGlobalLayout(view);
            }
        });
    }

    public static boolean needUpdate(long time)
    {
        return time + Pantip.REFRESH_TIME < new Date().getTime();
    }

    public static boolean after(long time, int amount, int field)
    {
        Calendar expiry = Calendar.getInstance();
        expiry.setTimeInMillis(time);
        expiry.add(field, amount);
        return Calendar.getInstance().after(expiry);
    }
}
