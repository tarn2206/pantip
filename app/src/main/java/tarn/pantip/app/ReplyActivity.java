package tarn.pantip.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonObject;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Json;
import tarn.pantip.content.ObjectStore;
import tarn.pantip.content.PostComment;
import tarn.pantip.model.Comment;
import tarn.pantip.model.Story;
import tarn.pantip.model.StoryType;
import tarn.pantip.model.TopicEx;
import tarn.pantip.util.ApiAware;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.PostCommandBar;

/**
 * User: Tarn
 * Date: 6/13/13 11:14 PM
 */
public class ReplyActivity extends BaseActivity implements PostCommandBar.CommandListener, View.OnFocusChangeListener
{
    private TopicEx topic;
    private Comment comment;
    private EditText editor;
    private boolean editMode;
    private String original;
    private PostCommandBar commandBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply);

        setHomeIcon(R.drawable.ic_close_white_24dp);
        setTitle(Pantip.currentUser.name);
        MyTopicActivity.addAvatar(this, toolbar);

        editor = findViewById(R.id.message);
        editor.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            { }

            @Override
            public void afterTextChanged(Editable s)
            {
                updatePostButton();
            }
        });
        editor.setOnFocusChangeListener(this);

        commandBar = findViewById(R.id.command_bar);
        commandBar.setListener(this);
        commandBar.setEditText(editor);
        commandBar.loadInstanceState(savedInstanceState);

        if (savedInstanceState == null)
        {
            String key = getIntent().getStringExtra("topic");
            topic = (TopicEx)ObjectStore.get(key);
            key = getIntent().getStringExtra("comment");
            comment = key == null ? topic : (Comment)ObjectStore.get(key);
            editMode = getIntent().getBooleanExtra("edit", false);
            if (editMode)
            {
                editor.setEnabled(false);
                getCommentInfo();
            }
        }
        else
        {
            topic = Json.fromJson(savedInstanceState.getString("topic"), TopicEx.class);
            comment = Json.fromJson(savedInstanceState.getString("comment"), Comment.class);
            editor.setText(savedInstanceState.getString("message"));
            editMode = savedInstanceState.getBoolean("edit");
            original = savedInstanceState.getString("original");
        }

        TextView text = findViewById(R.id.text);
        if (editMode) Utils.setVisible(text, false);
        else
        {
            //text.setTextSize(Pantip.textSize);
            StringBuilder s = new StringBuilder();
            if (comment == null || comment.isTopic() || comment.no == 0) s.append(topic.title);
            else
            {
                s.append('#').append(comment.no);
                if (comment.replyNo > 0) s.append('-').append(comment.replyNo);
                if (comment.storyList != null)
                {
                    for (Story st : comment.storyList)
                    {
                        if (st.type == StoryType.Text) s.append(' ').append(st.text);
                    }
                }
            }
            text.setText(s.toString());
        }
        ApiAware.setTaskDescription(this, null);
    }

    private void getCommentInfo()
    {
        Observable<JsonObject> observable = comment.replyNo == 0
                ? PostComment.getInfo(topic.id, comment.id, comment.no)
                : PostComment.getInfo(topic.id, comment.id, comment.no, comment.replyId, comment.replyNo);
        observable.subscribe(json -> {
            editor.setEnabled(true);
            if (json.has("item"))
            {
                original = json.getAsJsonObject("item").get("raw_message").getAsString();
                editor.setText(original);
                editor.setSelection(editor.length());
            }
        }, tr -> {
            editor.setEnabled(true);
            Utils.showToast(ReplyActivity.this, tr.getMessage());
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (commandBar != null && commandBar.notPickImage()) editor.requestFocus();
    }

    private void updatePostButton()
    {
        commandBar.updateReady(editor.getText().toString().trim().length() > 0);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("topic", Json.toJson(topic));
        outState.putString("comment", Json.toJson(comment));
        outState.putString("message", editor.getText().toString());
        outState.putBoolean("edit", editMode);
        outState.putString("original", original);
        commandBar.saveInstanceState(outState);
    }

    @Override
    public void selectTag(String[] tags)
    {/*ignored*/}

    @Override
    public void onShowGallery()
    {
        editor.clearFocus();
    }

    @Override
    public void onPost()
    {
        String message = editor.getText().toString().trim();
        if (message.length() == 0)
        {
            Utils.showToast(this, "กรุณากรอกข้อความ", Toast.LENGTH_SHORT, true);
            editor.requestFocus();
            return;
        }

        commandBar.updateReady(false);
        if (editMode) editComment(message);
        else reply(message);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus)
    {
        if (hasFocus) commandBar.hideGallery();
    }

    private void editComment(final String message)
    {
        Observable<JsonObject> observable = comment.replyId == 0
                ? PostComment.edit(topic.id, comment.id, comment.no, message)
                : PostComment.edit(topic.id, comment.id, comment.no, comment.replyId, comment.replyNo, message);
        observable.subscribe(json -> {
            if (json.has("error_message"))
            {
                commandBar.updateReady(true);
                Utils.showToast(ReplyActivity.this, json.get("error_message").getAsString(), Toast.LENGTH_LONG, false);
            }
            else done();
        }, tr -> {
            commandBar.updateReady(true);
            Pantip.handleException(ReplyActivity.this, tr);
        });
    }

    private void reply(final String message)
    {
        Observable<String> observable = comment == null || comment.no == 0
                ? PostComment.reply(topic.type.getValue(), topic.id, message)
                : PostComment.replyComment(topic.type.getValue(), topic.id, message, comment.ref, comment.ref_id, "comment" + comment.no, comment.created_time);
        observable.subscribe(s -> {
            Utils.hideKeyboard(ReplyActivity.this);
            if (true) done();
            else
            {
                Utils.showToast(ReplyActivity.this, "เฉพาะสมาชิกที่ยืนยันตัวตนเท่านั้นที่สามารถตอบกระทู้นี้ได้", Toast.LENGTH_LONG, true);
                setResult(RESULT_CANCELED);
            }
        }, tr -> {
            commandBar.updateReady(true);
            Pantip.handleException(this, tr);
        });
    }

    private void done()
    {
        Intent intent = new Intent();
        if (!editMode) intent.putExtra("commentNo", comment.no);
        if (comment.replyNo == 0) intent.putExtra("no", String.valueOf(comment.no));
        else intent.putExtra("no", comment.no + "-" + comment.replyNo);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return item.getItemId() == android.R.id.home && confirmDiscard() || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        if (!confirmDiscard()) super.onBackPressed();
    }

    private boolean confirmDiscard()
    {
        if (editor.getText().toString().trim().length() == 0 || (editMode && editor.getText().toString().equals(original)))
        {
            return false;
        }
        AlertDialog dialog = Utils.createDialog(this)
                                  .setMessage("ละทิ้งข้อความนี้ไหม?")
                                  .setPositiveButton("ตกลง", (dialog1, which) -> {
                                      setResult(RESULT_CANCELED);
                                      finish();
                                  })
                                  .setNegativeButton("ยกเลิก", (dialog12, which) -> editor.requestFocus())
                                  .show();
        Window window = dialog.getWindow();
        if (window != null) window.setLayout(Pantip.displayWidth * 2 / 3, ViewGroup.LayoutParams.WRAP_CONTENT);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (!commandBar.onActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }
}
