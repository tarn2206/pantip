package tarn.pantip.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.flexbox.FlexboxLayout;

import org.apache.commons.lang3.StringUtils;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.PostComment;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.ApiAware;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.PostCommandBar;
import tarn.pantip.widget.TopicTypeAdapter;

/**
 * User: Tarn
 * Date: 5/14/13 11:40 PM
 */
public class PostActivity extends BaseActivity implements View.OnClickListener, PostCommandBar.CommandListener, View.OnFocusChangeListener
{
    private Spinner typeSpinner;
    private EditText titleText;
    private EditText productText;
    private EditText messageText;
    private FlexboxLayout selectedTags;
    private View productLayout;
    private View reviewType;
    private RatingBar ratingBar;
    private CheckBox consumerReview;
    private CheckBox sponsoredReview;
    private PostCommandBar commandBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setTheme(Pantip.currentTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        setTitle("");
        ApiAware.setTaskDescription(this, null);

        MyTopicActivity.addAvatar(this, toolbar);

        typeSpinner = findViewById(R.id.topic_type);
        final TopicTypeAdapter adapter = new TopicTypeAdapter(this);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                adapter.setSelection(position);
                selectTopicType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            { }
        });
        selectedTags = findViewById(R.id.tags);

        titleText = findViewById(R.id.post_topic);
        titleText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); //This will treat Enter as Next instead of new line:
        titleText.addTextChangedListener(textWatcher);
        titleText.setOnFocusChangeListener(this);
        productLayout = findViewById(R.id.product_layout);
        productText = findViewById(R.id.product_name);
        productText.addTextChangedListener(textWatcher);
        productText.setOnFocusChangeListener(this);
        ratingBar = findViewById(R.id.rating);
        reviewType = findViewById(R.id.review_type);
        consumerReview = findViewById(R.id.consumer_review);
        consumerReview.setOnClickListener(this);
        sponsoredReview = findViewById(R.id.sponsored_review);
        sponsoredReview.setOnClickListener(this);
        messageText = findViewById(R.id.post_message);
        messageText.addTextChangedListener(textWatcher);
        messageText.setOnFocusChangeListener(this);

        commandBar = findViewById(R.id.command_bar);
        commandBar.setListener(this);
        commandBar.setEditText(messageText);
        commandBar.loadInstanceState(savedInstanceState);

        updatePostButton();

        if (savedInstanceState == null) typeSpinner.setSelection(0);
        else
        {
            typeSpinner.setSelection(savedInstanceState.getInt("type"));
            titleText.setText(savedInstanceState.getString("title"));
            messageText.setText(savedInstanceState.getString("message"));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt("type", typeSpinner.getSelectedItemPosition());
        outState.putString("title", titleText.getText().toString());
        outState.putString("message", messageText.getText().toString());
        commandBar.saveInstanceState(outState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (commandBar != null && commandBar.notPickImage()) titleText.requestFocus();
    }

    private final TextWatcher textWatcher = new TextWatcher()
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
    };

    private final int[] titleHint = new int[] { R.string.post_topic1, R.string.post_topic2, R.string.post_topic3, R.string.post_topic4, R.string.post_topic5, R.string.post_topic6 };
    private final int[] messageHint = new int[] { R.string.post_message1, R.string.post_message2, R.string.post_message3, R.string.post_message4, R.string.post_message5, R.string.post_message6 };

    private void selectTopicType(int position)
    {
        titleText.setHint(titleHint[position]);
        messageText.setHint(messageHint[position]);

        boolean isReview = isReview(position);
        Utils.setVisible(productLayout, isReview);
        if (isReview && titleText.length() > 0)
        {
            productText.requestFocus();
            productText.setSelection(productText.length());
        }
        Utils.setVisible(ratingBar, isReview);
        Utils.setVisible(reviewType, isReview);
        updatePostButton();
    }

    private boolean isReview(int position)
    {
        return position == 4;
    }

    private void updatePostButton()
    {
        boolean enabled = titleText.getText().toString().trim().length() >= 5;
        if (getTopicType() == TopicType.Review)
        {
            enabled &= StringUtils.isNotBlank(productText.getText().toString())
                    & (consumerReview.isChecked() || sponsoredReview.isChecked());
        }
        commandBar.updateReady(enabled);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.consumer_review:
            case R.id.sponsored_review:
                updatePostButton();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (!commandBar.onActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    private TopicType getTopicType()
    {
        switch (typeSpinner.getSelectedItemPosition())
        {
            case 1: return TopicType.Question;
            case 2: return TopicType.News;
            case 3: return TopicType.Poll;
            case 4: return TopicType.Review;
            case 5: return TopicType.Shopping;
            default: return TopicType.Chat;
        }
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
        if (titleText.getText().toString().trim().length() < 5 && messageText.getText().toString().trim().length() == 0)
        {
            if (isReview(typeSpinner.getSelectedItemPosition()))
            {
                if (productText.getText().toString().trim().length() == 0) return false;
            }
            else return false;
        }
        AlertDialog dialog = Utils.createDialog(this)
                                  .setMessage("ละทิ้งข้อความนี้ไหม?")
                                  .setPositiveButton("ตกลง", (dialog1, which) -> {
                                      setResult(RESULT_CANCELED);
                                      finish();
                                  })
                                  .setNegativeButton("ยกเลิก", null)
                                  .show();
        Window window = dialog.getWindow();
        if (window != null) window.setLayout(Pantip.displayWidth * 2 / 3, ViewGroup.LayoutParams.WRAP_CONTENT);
        return true;
    }

    @Override
    public void selectTag(String[] tags)
    {
        selectedTags.removeAllViews();
        if (tags == null || tags.length == 0) return;
        for (String tag : tags)
        {
            TextView textView = (TextView)inflate(R.layout.static_tag, selectedTags);
            textView.setText(tag);
            selectedTags.addView(textView);
        }
    }

    @Override
    public void onShowGallery()
    {
        titleText.clearFocus();
        productText.clearFocus();
        messageText.clearFocus();
    }

    @Override
    public void onPost()
    {
        if (commandBar.roomId == 0)
        {
            Utils.createDialog(this)
                 .setTitle("แจ้งเตือน")
                 .setMessage("คุณยังไม่ได้เลือกแท็ก กระทู้นี้จะไปอยู่ในห้องไร้สังกัด")
                 .setPositiveButton("ส่งกระทู้", (dialog, which) -> post())
                 .setNegativeButton("กลับไปแก้ไข", null)
                 .show();
        }
        else post();
    }

    private void post()
    {
        commandBar.updateReady(false);

        String[] trimmed = null;
        if (commandBar.tags != null && commandBar.tags.length > 0)
        {
            trimmed = new String[commandBar.tags.length];
            for (int i = 0; i < commandBar.tags.length; i++)
            {
                trimmed[i] = commandBar.tags[i].trim();
            }
        }
        PostComment.post(commandBar.roomId, trimmed, getTopicType(), titleText.getText().toString().trim(),
                messageText.getText().toString().trim(), productText.getText().toString().trim(),
                ratingBar.getRating(), consumerReview.isChecked(), sponsoredReview.isChecked())
                .subscribe(id -> {
                    Utils.hideKeyboard(this);
                    finish();
                }, tr -> {
                    commandBar.updateReady(true);
                    Pantip.handleException(PostActivity.this, tr);
                });
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus)
    {
        if (hasFocus) commandBar.hideGallery();
    }
}