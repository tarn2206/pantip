package tarn.pantip.app;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.text.DecimalFormat;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;

/**
 * User: tarn
 * Date: 2/17/13 2:03 PM
 */
public class SettingsActivity extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager().beginTransaction().add(R.id.settings, new SettingsFragment()).commit();
    }

    public static String formatSize(long size)
    {
        DecimalFormat format = new DecimalFormat("#,##0.0");
        if (size < 1048576) return formatSize(format.format(size / 1024.0), " KB");
        if (size < 1073741824) return formatSize(format.format(size / 1048576.0), " MB");
        return formatSize(format.format(size / 1073741824), " GB");
    }

    private static String formatSize(String size, String unit)
    {
        if (size.endsWith(".0")) return size.substring(0, size.length() - 2) + unit;
        return size + unit;
    }

    public static String version()
    {
        try
        {
            PackageInfo info = Pantip.context.getPackageManager().getPackageInfo(Pantip.context.getPackageName(), 0);
            return info.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            L.e(e);
            return e.getMessage();
        }
    }
}