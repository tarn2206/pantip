package tarn.pantip.app;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import tarn.pantip.Pantip;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 10 September 2016
 */
public class ConfirmDialog
{
    static void delete(AppCompatActivity activity, CharSequence message, DialogInterface.OnClickListener positiveListener)
    {
        delete(activity, message, positiveListener, null);
    }

    public static void delete(AppCompatActivity activity, CharSequence message, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener)
    {
        delete(activity, null, message, "ใช่, ลบเลย", positiveListener, negativeListener);
    }

    public static void delete(AppCompatActivity activity, String title, CharSequence message, String positiveText, DialogInterface.OnClickListener positiveListener)
    {
        delete(activity, title, message, positiveText, positiveListener, null);
    }

    private static void delete(AppCompatActivity activity, String title, CharSequence message, String positiveText, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener)
    {
        AlertDialog dialog = Utils.createDialog(activity)
                                  .setTitle(title)
                                  .setMessage(message)
                                  .setPositiveButton(positiveText, positiveListener)
                                  .setNegativeButton("ยกเลิก", negativeListener)
                                  .setCancelable(true)
                                  .show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Pantip.dangerColor);
    }
}