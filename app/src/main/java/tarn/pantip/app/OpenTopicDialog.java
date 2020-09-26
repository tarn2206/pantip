package tarn.pantip.app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import tarn.pantip.Pantip;
import tarn.pantip.content.PantipClient;
import tarn.pantip.util.Utils;

/**
 * User: Tarn
 * Date: 11/4/13 10:43 PM
 */
class OpenTopicDialog
{
    public static void show(AppCompatActivity activity)
    {
        RelativeLayout view = new RelativeLayout(activity);
        EditText editor = new EditText(activity);
        editor.setTextSize(Pantip.textSize * 2);
        editor.setFilters(new InputFilter[] {new InputFilter.LengthFilter(8)});
        editor.setInputType(InputType.TYPE_CLASS_NUMBER);
        editor.setGravity(Gravity.CENTER);
        view.addView(editor, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int padding = Utils.toPixels(20);
        view.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = Utils.createDialog(activity)
                                  .setTitle("หมายเลขกระทู้ ")
                                  .setView(view)
                                  .setPositiveButton("ตกลง", null)
                                  .setNegativeButton("ยกเลิก", null)
                                  .show();
        dialog.setCancelable(false);
        if (dialog.getWindow() != null)
        {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        editor.requestFocus();
        final OnClickListener clickListener = new OnClickListener(dialog, editor);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(clickListener);
        editor.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE)
            {
                clickListener.onClick(null);
                return true;
            }
            return false;
        });
    }

    private static class OnClickListener implements View.OnClickListener
    {
        private final Context context;
        private final Dialog dialog;
        private final EditText editor;
        private long lastClick;

        OnClickListener(Dialog dialog, EditText editor)
        {
            this.context = dialog.getContext();
            this.dialog = dialog;
            this.editor = editor;
        }

        @Override
        public void onClick(View v)
        {
            if (System.currentTimeMillis() - lastClick < 1000) return;
            lastClick = System.currentTimeMillis();

            String text = editor.getText().toString().trim();
            if (text.length() == 0)
            {
                Utils.showToast(context, "คุณไม่ได้กรอกหมายเลขกระทู้");
                return;
            }
            if (text.length() < 8)
            {
                Utils.showToast(context, "หมายเลขกระทู้ " + text + " ไม่ถูกต้อง");
                return;
            }

            long id;
            try
            {
                id = Long.parseLong(text);
            }
            catch (Exception e)
            {
                Utils.showToast(context, "หมายเลขกระทู้ " + text + " ไม่ถูกต้อง");
                return;
            }

            PantipClient.openTopic(id).subscribe(html -> {
                if (html.contains("ไม่พบหน้าที่คุณต้องการ"))
                {
                    Utils.showToast(dialog.getContext(), "ไม่มีกระทู้หมายเลข " + id);
                    return;
                }

                Intent intent = new Intent(context, TopicActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra("id", id);
                context.startActivity(intent);
                dialog.dismiss();
            }, tr -> Utils.showToast(dialog.getContext(), tr.getMessage()));
        }
    }
}