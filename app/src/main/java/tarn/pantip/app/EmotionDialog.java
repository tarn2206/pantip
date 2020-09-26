package tarn.pantip.app;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;

import com.google.gson.JsonObject;

import tarn.pantip.L;
import tarn.pantip.R;
import tarn.pantip.content.Callback;
import tarn.pantip.content.CommentData;
import tarn.pantip.content.EmotionApi;
import tarn.pantip.content.TargetType;
import tarn.pantip.model.Emotion;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 21 September 2017
 */

public class EmotionDialog
{
    public static void show(AppCompatActivity activity, TargetType type, long topicId, long commentId, int commentNo, long replyId, int replyNo, Callback<Emotion> callback)
    {
        Dialog dialog = Utils.createDialog(activity).setCancelable(true).setView(R.layout.emotions).create();
        View decorView = null;
        Window window = dialog.getWindow();
        if (window != null) decorView = window.getDecorView();
        if (decorView != null)
        {
            decorView.setBackgroundResource(R.drawable.emoticons_background);
        }
        dialog.show();
        if (decorView != null)
        {
            init(decorView, R.id.reaction_like, "ถูกใจ", new OnClickListener(dialog, "like", type, topicId, commentId, commentNo, replyId, replyNo, callback));
            init(decorView, R.id.reaction_haha, "ขำกลิ้ง", new OnClickListener(dialog, "laugh", type, topicId, commentId, commentNo, replyId, replyNo, callback));
            init(decorView, R.id.reaction_love, "หลงรัก", new OnClickListener(dialog, "love", type, topicId, commentId, commentNo, replyId, replyNo, callback));
            init(decorView, R.id.reaction_impress, "ซึ้ง", new OnClickListener(dialog, "impress", type, topicId, commentId, commentNo, replyId, replyNo, callback));
            init(decorView, R.id.reaction_scary, "สยอง", new OnClickListener(dialog, "scary", type, topicId, commentId, commentNo, replyId, replyNo, callback));
            init(decorView, R.id.reaction_wow, "ทึ่ง", new OnClickListener(dialog, "surprised", type, topicId, commentId, commentNo, replyId, replyNo, callback));
            try
            {
                ViewGroup content = decorView.findViewById(android.R.id.content);
                ((ViewGroup)content.getChildAt(0)).getChildAt(2).setMinimumHeight(0);
            }
            catch (Exception e)
            {
                L.e(e);
            }
        }
        /*
         * decorView
         * +--FrameLayout
         *    +--FrameLayout
         *       +--app:id/action_bar_root
         *          +--android:id/content
         *             +--app:id/parentPanel
         *          +--app:id/action_mode_bar_stub
         *    +--ViewStub
         */
    }

    private static void init(View decorView, @IdRes int id, String tooltipText, OnClickListener l)
    {
        View v = decorView.findViewById(id);
        TooltipCompat.setTooltipText(v, tooltipText);
        v.setOnClickListener(l);
    }

    private static class OnClickListener implements View.OnClickListener
    {
        private final Dialog dialog;
        private final String emo;
        private final TargetType type;
        private final long topicId;
        private final long commentId;
        private final int commentNo;
        private final long replyId;
        private final int replyNo;
        private final Callback<Emotion> callback;

        OnClickListener(Dialog dialog, String emo, TargetType type, long topicId, long commentId, int commentNo, long replyId, int replyNo, Callback<Emotion> callback)
        {
            this.dialog = dialog;
            this.emo = emo;
            this.type = type;
            this.topicId = topicId;
            this.commentId = commentId;
            this.commentNo = commentNo;
            this.replyId = replyId;
            this.replyNo = replyNo;
            this.callback = callback;
        }

        @Override
        public void onClick(View v)
        {
            Window window = dialog.getWindow();
            if (window != null)
            {
                View decorView = window.getDecorView();
                decorView.findViewById(R.id.reaction_like).setEnabled(false);
                decorView.findViewById(R.id.reaction_haha).setEnabled(false);
                decorView.findViewById(R.id.reaction_love).setEnabled(false);
                decorView.findViewById(R.id.reaction_impress).setEnabled(false);
                decorView.findViewById(R.id.reaction_scary).setEnabled(false);
                decorView.findViewById(R.id.reaction_wow).setEnabled(false);
            }
            Utils.playSound(dialog.getContext(), R.raw.double_pop);

            EmotionApi.post(emo, topicId, type, commentId, commentNo, replyId, replyNo)
                    .subscribe(json -> {
                        if (!"ok".equals(json.get("status").getAsString()) || !json.has("emotion"))
                        {
                            callback.complete(null);
                        }
                        if (json.has("emotion"))
                        {
                            JsonObject e = json.get("emotion").getAsJsonObject();
                            getEmotion(e);
                        }
                        else dialog.dismiss();
                    }, tr -> {
                        dialog.dismiss();
                        callback.error(tr);
                    });
        }

        private void getEmotion(JsonObject e)
        {
            EmotionApi.get(e).subscribe(json -> {
                Emotion result = CommentData.parseEmotions(json.getAsJsonObject("emotion"));
                callback.complete(result);
            }, callback::error, dialog::dismiss);
        }
    }
}
