package tarn.pantip.app;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;

import org.apache.commons.io.FileUtils;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Account;
import tarn.pantip.content.MyTopic;
import tarn.pantip.util.Utils;

/**
 * User: Tarn
 * Date: 8/24/13 1:40 PM
 */
final class LogoutDialog
{
    private LogoutDialog()
    { }

    static void show(Activity activity, final LogoutCallback callback)
    {
        AlertDialog dialog = Utils.createDialog(activity)
                                  .setMessage("ออกจากระบบไหม?")
                                  .setPositiveButton("ใช่", createListener(activity, callback))
                                  .setNegativeButton("ไม่ใช่", (d, which) -> {
                                      d.dismiss();
                                      callback.cancel();
                                  })
                                  .setCancelable(true)
                                  .show();
        Window window = dialog.getWindow();
        if (window != null)
        {
            window.setLayout(Pantip.displayWidth * 2 / 3, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Pantip.dangerColor);
    }

    private static DialogInterface.OnClickListener createListener(final Activity activity, final LogoutCallback callback)
    {
        return (dialog, which) -> {
            dialog.dismiss();
            Account.logout()
                    .subscribe(empty -> {
                        Utils.showToast(activity, activity.getString(R.string.logged_out));
                        callback.complete();
                        invalidate();
                    }, tr -> {
                        callback.error(tr);
                        invalidate();
                    });
        };
    }

    private static void invalidate()
    {
        FileUtils.deleteQuietly(Utils.getAvatarFile());
        MyTopic.delete();
        Pantip.invalidate();
    }

    public interface LogoutCallback
    {
        default void cancel() {}
        void complete();
        void error(Throwable tr);
    }
}
