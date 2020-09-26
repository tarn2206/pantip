package tarn.pantip.widget;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;

import org.apache.commons.lang3.StringUtils;

import tarn.pantip.L;
import tarn.pantip.R;
import tarn.pantip.app.MyGalleryActivity;
import tarn.pantip.app.SelectTagActivity;
import tarn.pantip.content.Gallery;
import tarn.pantip.util.Utils;

public class PostCommandBar extends LinearLayout implements View.OnClickListener, MyGalleryView.OnSelectListener
{
    private static final int REQUEST_SELECT_TAGS = 10;
    private static final int REQUEST_MANAGE_GALLERY = 11;
    private AppCompatActivity activity;
    private Button actionButton;
    private String postText;
    private ImageButton selectTag;
    private ImageButton pickImage;
    private RelativeLayout galleryGroup;
    private MyGalleryView gallery;
    private ContentLoadingProgressBar progressBar;
    private CommandListener listener;
    public int roomId;
    public String[] tags;
    private boolean readyToPost;
    private EditText editText;

    public PostCommandBar(Context context)
    {
        this(context, null, 0);
    }

    public PostCommandBar(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public PostCommandBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr)
    {
        setOrientation(VERTICAL);
        activity = getActivity(context);
        LayoutInflater.from(context).inflate(R.layout.post_command, this);
        selectTag = findButton(R.id.select_tag);

        try
        {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PostCommandBar, defStyleAttr, 0);
            postText = a.getString(R.styleable.PostCommandBar_commandText);
            selectTag.setVisibility(a.getBoolean(R.styleable.PostCommandBar_selectTag, false) ? VISIBLE : GONE);
            a.recycle();
        }
        catch (Exception e)
        {
            L.e(e);
        }

        pickImage = findButton(R.id.pick_image);
        pickImage.setEnabled(false);
        ImageButton addEmo = findButton(R.id.emoticons);
        addEmo.setVisibility(GONE);
        ImageButton links = findButton(R.id.add_link);
        ImageButton spoil = findButton(R.id.add_spoil);

        TooltipCompat.setTooltipText(selectTag, "เลือกแท็ก");
        TooltipCompat.setTooltipText(pickImage, "ใส่รูปประกอบ");
        TooltipCompat.setTooltipText(addEmo, "อีโมติคอน");
        TooltipCompat.setTooltipText(links, "ลิ้งก์");
        TooltipCompat.setTooltipText(spoil, "สปอย");

        actionButton = findViewById(R.id.action_button);
        actionButton.setText(postText);
        actionButton.setOnClickListener(this);

        gallery = findViewById(R.id.gallery);
        gallery.setOnSelectListener(this);

        galleryGroup = findViewById(R.id.gallery_group);
        galleryGroup.setVisibility(GONE);

        findButton(R.id.my_gallery);
        progressBar = findViewById(R.id.gallery_progress);
        progressBar.setVisibility(View.GONE);

        onConfigurationChanged(context.getResources().getConfiguration());

        checkPhotoFeature();
    }

    private void checkPhotoFeature()
    {
        Gallery.checkAvailable().subscribe(json -> {
            pickImage.setEnabled(!json.has("error_message"));
            //Utils.showToast(activity, json.get("error_message").getAsString(), Toast.LENGTH_SHORT, true);
        }, L::e);
    }

    private ImageButton findButton(@IdRes int id)
    {
        ImageButton button = findViewById(id);
        if (button != null) button.setOnClickListener(this);
        return button;
    }

    private AppCompatActivity getActivity(Context context)
    {
        while (context instanceof ContextWrapper)
        {
            if (context instanceof AppCompatActivity)
            {
                return (AppCompatActivity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

    public void loadInstanceState(Bundle savedInstanceState)
    {
        if (pickImage.isSelected()) onClick(pickImage);
        if (savedInstanceState != null)
        {
            roomId = savedInstanceState.getInt("roomId");
            tags = savedInstanceState.getStringArray("tags");

            selectTag.setSelected(tags != null && tags.length > 0);
            listener.selectTag(tags);
        }
    }

    public void saveInstanceState(Bundle outState)
    {
        if (selectTag.getVisibility() == VISIBLE)
        {
            outState.putInt("roomId", roomId);
            outState.putStringArray("tags", tags);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        int itemHeight = gallery.calcItemHeight(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            galleryGroup.getLayoutParams().height = itemHeight + itemHeight;
        }
        else
        {
            galleryGroup.getLayoutParams().height = itemHeight;
        }
        gallery.updateLayoutManager(newConfig.orientation);
    }

    public void setEditText(EditText editText)
    {
        this.editText = editText;
    }

    public boolean notPickImage()
    {
        return !pickImage.isSelected();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.select_tag:
                selectTag();
                break;

            case R.id.pick_image:
                showPickImage(v);
                break;

            case R.id.add_link:
                addTag("url", "");
                break;

            case R.id.add_spoil:
                addTag("spoil", "");
                break;

            case R.id.action_button:
                if (pickImage.isSelected()) insertPicture();
                else listener.onPost();
                break;

            case R.id.my_gallery:
                Intent intent = new Intent(activity, MyGalleryActivity.class);
                activity.startActivityForResult(intent, REQUEST_MANAGE_GALLERY);
                break;
        }
    }

    private void selectTag()
    {
        Intent intent = new Intent(activity, SelectTagActivity.class);
        intent.putExtra("room_id", roomId);
        intent.putExtra("tags", tags);
        activity.startActivityForResult(intent, REQUEST_SELECT_TAGS);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_SELECT_TAGS)
        {
            if (resultCode == AppCompatActivity.RESULT_OK)
            {
                roomId = data.getIntExtra("room_id", 0);
                tags = data.getStringArrayExtra("tags");
                selectTag.setSelected(tags != null && tags.length > 0);
                listener.selectTag(tags);
            }
            return true;
        }
        else if (requestCode == REQUEST_MANAGE_GALLERY)
        {
            gallery.clearSelected();
            onSelectChanged(0);
            gallery.load(null);
            if (data != null && data.getBooleanExtra("inserted", false))
            {
                gallery.scrollToTop();
            }
        }
        return false;
    }

    private boolean ignoreHide;

    private void showPickImage(View v)
    {
        if (v.isSelected()) return;
        v.setSelected(true);
        ignoreHide = true;

        Utils.hideKeyboard(activity);

        actionButton.setText(" แทรก ");
        onSelectChanged(gallery.getSelectedItems().size());

        postDelayed(() -> {
            galleryGroup.setVisibility(VISIBLE);
            if (!gallery.loaded) gallery.load(progressBar);
            gallery.requestFocus();
            listener.onShowGallery();
            ignoreHide = false;
        }, 100);
    }

    public void hideGallery()
    {
        if (ignoreHide) return;
        pickImage.setSelected(false);
        galleryGroup.setVisibility(GONE);
        actionButton.setText(postText);
        actionButton.setEnabled(readyToPost);
    }

    private void insertPicture()
    {
        for (Gallery o : gallery.getSelectedItems())
        {
            addTag("img", o.url);
        }
        gallery.clearSelected();
    }

    public void updateReady(boolean ready)
    {
        readyToPost = ready;
        if (!pickImage.isSelected()) actionButton.setEnabled(ready);
    }

    private void addTag(String tagNme, String content)
    {
        StringBuilder s = new StringBuilder();
        s.append('[').append(tagNme).append(']');
        if (StringUtils.isNotBlank(content)) s.append(content);
        s.append("[/").append(tagNme).append(']');

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) editText.getText().insert(start, s);
        else editText.getText().replace(start, end, s);
        int x = start + (StringUtils.isBlank(content) ? tagNme.length() + 2 : s.length());
        editText.setSelection(x, x);
    }

    @Override
    public void onSelectChanged(int n)
    {
        actionButton.setEnabled(n > 0);
    }

    public void setListener(CommandListener listener)
    {
        this.listener = listener;
    }

    public interface CommandListener
    {
        void selectTag(String[] tags);
        void onShowGallery();
        void onPost();
    }
}
