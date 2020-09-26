package tarn.pantip.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Json;
import tarn.pantip.model.Story;
import tarn.pantip.util.ApiAware;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.SimpleAnimationListener;
import tarn.pantip.util.Utils;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * User: tarn
 * Date: 2/18/13 3:48 PM
 */
public class PhotoActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final int RC_SAVE_PHOTO = 1;
    public static View currentPhoto;
    private ViewPager viewPager;
    private View backdrop;
    private View clippedView;
    private PhotoView photoView;
    private View commandBar;
    private long topicId;
    private String url;
    private File file;
    private Animation fadeIn;
    private Animation fadeOut;
    private Rect startBounds;
    private boolean dialog;
    private static final int animTime = 300;
    private List<Story> imageList;
    private int imageIndex;
    private final Handler handler = new Handler();
    private final int progressSize = Utils.toPixels(36);
    private boolean backPressed;
    private File[] files;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_photo);
        ApiAware.TranslucentSystemUI(getWindow());

        if (savedInstanceState == null)
        {
            topicId = getIntent().getLongExtra("topic_id", 0);
            url = getIntent().getStringExtra("url");
            startBounds = getIntent().getParcelableExtra("startBounds");
            dialog = getIntent().getBooleanExtra("dialog", false);
            Story[] stories = Json.fromJson(getIntent().getStringExtra("imageList"), Story[].class);
            if (stories != null) imageList = Arrays.asList(stories);
            imageIndex = getIntent().getIntExtra("imageIndex", 0);
        }
        else
        {
            topicId = savedInstanceState.getLong("topic_id");
            url = savedInstanceState.getString("url");
            startBounds = savedInstanceState.getParcelable("startBounds");
            dialog = savedInstanceState.getBoolean("dialog");
            Story[] stories = Json.fromJson(savedInstanceState.getString("imageList"), Story[].class);
            if (stories != null) imageList = Arrays.asList(stories);
            imageIndex = savedInstanceState.getInt("imageIndex");
        }
        if (url == null)
        {
            finish();
            return;
        }
        commandBar = findViewById(R.id.photo_command);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            int bottom;
            int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (id > 0) bottom = getResources().getDimensionPixelSize(id);
            else bottom = Utils.toPixels(48);
            commandBar.setPadding(commandBar.getPaddingLeft(), commandBar.getPaddingTop(), commandBar.getPaddingRight(), bottom);
        }
        try
        {
            loadImage();
        }
        catch (Throwable e)
        {
            Pantip.handleException(this, e);
        }

        ApiAware.setTaskDescription(this, null);
        fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        fadeOut.setAnimationListener(new SimpleAnimationListener()
        {
            @Override
            public void onAnimationEnd(Animation animation)
            {
                Utils.setVisible(commandBar, false);
            }
        });

        viewPager = findViewById(R.id.viewPager);
        viewPager.setPageMargin(Utils.toPixels(20));
        viewPager.setVisibility(View.GONE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putLong("topic_id", topicId);
        outState.putString("url", url);
        outState.putParcelable("startBounds", startBounds);
        outState.putBoolean("dialog", dialog);
        if (imageList != null) outState.putString("imageList", Json.toJson(imageList));
        outState.putInt("imageIndex", imageIndex);
    }

    private void loadImage()
    {
        GlideApp.with(this).downloadOnly().load(url).into(new CustomTarget<File>()
        {
            @Override
            public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition)
            {
                onReady(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder)
            {

            }
        });
    }

    private void onReady(File resource)
    {
        file = resource;
        photoView = findViewById(R.id.photo);
        photoView.setOnClickListener(this);
        try
        {
            Bitmap bitmap = BitmapFactory.decodeFile(resource.getAbsolutePath());
            photoView.setImageBitmap(bitmap);
        }
        catch (OutOfMemoryError e)
        {
            onFinish();
            Utils.showToast("Error: Out of Memory");
            return;
        }
        catch (Exception e)
        {
            onFinish();
            Utils.showToast(e.getMessage());
            return;
        }
        backdrop = findViewById(R.id.backdrop);
        backdrop.setAlpha(dialog ? 0 : 0.7f);
        clippedView = (View)photoView.getParent();

        if (startBounds.height() > 0)
        {
            Rect finalBounds = calcFinalBounds();
            clip(finalBounds);
            float startScale = calcStartScale(finalBounds);
            photoView.getLayoutParams().width = finalBounds.width();
            photoView.getLayoutParams().height = finalBounds.height();

            AnimatorSet set = new AnimatorSet();
            AnimatorSet.Builder builder = set.play(ObjectAnimator.ofFloat(photoView, View.Y, startBounds.top, finalBounds.top));
            if (startBounds.left != finalBounds.left) builder.with(ObjectAnimator.ofFloat(photoView, View.X, startBounds.left, finalBounds.left));
            if (startScale != 1f)
            {
                builder.with(ObjectAnimator.ofFloat(photoView, View.SCALE_X, startScale, 1f))
                       .with(ObjectAnimator.ofFloat(photoView, View.SCALE_Y, startScale, 1f));
                photoView.setPivotX(0f);
                photoView.setPivotY(0f);
            }
            builder.with(ObjectAnimator.ofFloat(backdrop, View.ALPHA, 1));
            set.setDuration(animTime);
            set.setInterpolator(new DecelerateInterpolator());
            set.addListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationStart(Animator animation)
                {
                    if (currentPhoto != null)
                    {
                        new Handler().postDelayed(() -> {
                            if (currentPhoto != null) currentPhoto.setVisibility(View.INVISIBLE);
                        }, animTime / 5);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation)
                {
                    onAnimationFinish();
                }

                @Override
                public void onAnimationCancel(Animator animation)
                {
                    onAnimationFinish();
                }
            });
            set.start();
            commandBar.startAnimation(fadeIn);
        }
        else onAnimationFinish();
    }

    private void clip(Rect finalBounds)
    {
        Point size = Utils.getDisplaySize();
        int top = getIntent().getIntExtra("toolbarHeight", 0);
        Rect rect;
        if (finalBounds == null)
        {
            if (dialog) rect = getIntent().getParcelableExtra("dialogBounds");
            else rect = new Rect(0, top, size.x, size.y);
        }
        else rect = new Rect(0, finalBounds.top <= top ? 0 : top, size.x, size.y);
        clippedView.setClipBounds(rect);
    }

    private Rect calcFinalBounds()
    {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(size);
        int width;
        int height;
        float xRatio = startBounds.width() / (float)size.x;
        float yRatio = startBounds.height() / (float)size.y;
        if (xRatio < yRatio)
        {
            height = size.y;
            width = height * startBounds.width() / startBounds.height();
        }
        else
        {
            width = size.x;
            height = width * startBounds.height() / startBounds.width();
        }
        int x = (size.x - width) / 2;
        int y = (size.y - height) / 2;
        return new Rect(x, y, x + width, y + height);
    }

    private float calcStartScale(Rect finalBounds)
    {
        if ((float)finalBounds.width() / finalBounds.height() > (float)startBounds.width() / startBounds.height())
        {
            return (float)startBounds.height() / finalBounds.height();
        }
        else
        {
            return (float)startBounds.width() / finalBounds.width();
        }
    }

    private void onAnimationFinish()
    {
        clippedView.setClipBounds(null);
        if (photoView != null)
        {
            photoView.setX(0);
            photoView.setY(0);
            photoView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            photoView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            photoView.requestLayout();
        }
        findViewById(R.id.photo_share).setOnClickListener(this);
        findViewById(R.id.photo_save).setOnClickListener(this);
        if (imageList != null && imageList.size() > 1)
        {
            files = new File[imageList.size()];
            viewPager.setAdapter(new MyPagerAdapter());
            viewPager.setCurrentItem(imageIndex, false);
            viewPager.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasChanges()
    {
        return getResources().getConfiguration().orientation != getIntent().getIntExtra("orientation", -1)
                || (imageList != null && viewPager.getCurrentItem() != imageIndex);
    }

    @Override
    public void onBackPressed()
    {
        backPressed = true;
        if (startBounds.height() <= 0 || hasChanges())
        {
            onFinish();
            return;
        }
        photoView.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.GONE);
        clip(null);
        Utils.setVisible(commandBar, false);
        Rect finalBounds = calcFinalBounds();
        float startScale = calcStartScale(finalBounds);
        photoView.setX(finalBounds.left);
        photoView.setY(finalBounds.top);
        photoView.getLayoutParams().width = finalBounds.width();
        photoView.getLayoutParams().height = finalBounds.height();
        photoView.setScaleX(1f);
        photoView.setScaleY(1f);
        photoView.requestLayout();
        backdrop.setAlpha(dialog ? 0 : 1);
        AnimatorSet set = new AnimatorSet();
        AnimatorSet.Builder builder = set.play(ObjectAnimator.ofFloat(photoView, View.Y, startBounds.top));
        if (startBounds.left != finalBounds.left) builder.with(ObjectAnimator.ofFloat(photoView, View.X, startBounds.left));
        if (startScale != 1f)
        {
            builder.with(ObjectAnimator.ofFloat(photoView, View.SCALE_X, startScale))
                   .with(ObjectAnimator.ofFloat(photoView, View.SCALE_Y, startScale));
            photoView.setPivotX(0f);
            photoView.setPivotY(0f);
        }
        if (!dialog) builder.with(ObjectAnimator.ofFloat(backdrop, View.ALPHA, 0));
        set.setDuration(animTime);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                onFinish();
            }

            @Override
            public void onAnimationCancel(Animator animation)
            {
                onFinish();
            }
        });
        set.start();
    }

    private void onFinish()
    {
        if (currentPhoto != null)
        {
            Utils.setVisible(currentPhoto, true);
            currentPhoto = null;
        }
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == RC_SAVE_PHOTO)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                findViewById(R.id.photo_save).callOnClick();
            else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE))
                Utils.showAppSettings(PhotoActivity.this);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (photoView != null)
        {
            Utils.recycle(photoView);
            photoView = null;
        }
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.photo_share) sharePhoto();
        else if (v.getId() == R.id.photo_save) savePhoto(this);
        else tapPhoto();
    }

    private void tapPhoto()
    {
        if (commandBar.getVisibility() == View.VISIBLE)
        {
            ApiAware.displaySystemUI(getWindow(), false);
            commandBar.startAnimation(fadeOut);
        }
        else
        {
            ApiAware.displaySystemUI(getWindow(), true);
            Utils.setVisible(commandBar, true);
            commandBar.startAnimation(fadeIn);
        }
    }

    private void sharePhoto()
    {
        try
        {
            File f = files == null ? file : files[viewPager.getCurrentItem()];
            final Uri uri = FileProvider.getUriForFile(this, "tarn.pantip.fileProvider", f);
            String fullUrl = files == null ? url : imageList.get(viewPager.getCurrentItem()).text;
            int i = fullUrl.lastIndexOf('/');
            String fileName = fullUrl.substring(i + 1);
            String type = fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";

            Intent intent = ShareCompat.IntentBuilder.from(this).setStream(uri).setType(type).setSubject(fileName).getIntent();
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share"));
        }
        catch (IllegalArgumentException e)
        {
            Pantip.handleException(this, e);
        }
    }

    private void savePhoto(Context context)
    {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            Utils.createDialog(this)
                 .setTitle("SD card unavailable")
                 .setMessage("The SD card is missing or not mounted.")
                 .show();
            return;
        }
        File copy = null;
        try
        {
            if (!Utils.hasPermission(this, WRITE_EXTERNAL_STORAGE, RC_SAVE_PHOTO)) return;
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            dir = new File(dir, "Pantip");
            FileUtils.forceMkdir(dir);

            String fullUrl = files == null ? url : imageList.get(viewPager.getCurrentItem()).text;
            int i = fullUrl.lastIndexOf('/');
            String fileName = fullUrl.substring(i + 1);
            copy = new File(dir, fileName);
            FileUtils.copyFile(files == null ? file : files[viewPager.getCurrentItem()], copy);
            showSaveResult(copy);
        }
        catch (Exception e)
        {
            Pantip.handleException(this, e);
            FileUtils.deleteQuietly(copy);
        }
    }

    class MyPagerAdapter extends PagerAdapter
    {
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position)
        {
            Story story = imageList.get(position);
            final FrameLayout frame = new FrameLayout(PhotoActivity.this);
            container.addView(frame);
            final PhotoView imageView = new PhotoView(PhotoActivity.this);
            frame.addView(imageView);
            final ProgressBar progressBar = new ProgressBar(PhotoActivity.this, null, android.R.attr.progressBarStyle);
            frame.addView(progressBar);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)progressBar.getLayoutParams();
            params.width = params.height = progressSize;
            params.gravity = Gravity.CENTER;
            GlideApp.with(PhotoActivity.this).downloadOnly().load(story.text).listener(new RequestListener<File>()
            {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource)
                {
                    if (e != null) Utils.showToast(e.getMessage());
                    return false;
                }

                @Override
                public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource)
                {
                    return false;
                }
            }).into(new CustomTarget<File>()
            {
                @Override
                public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition)
                {
                    files[position] = resource;
                    if (!backPressed && photoView.getVisibility() != View.GONE)
                    {
                        handler.postDelayed(() -> {
                            if (!backPressed && photoView != null)
                                photoView.setVisibility(View.GONE);
                        }, 500);
                    }
                    try
                    {
                        Bitmap bitmap = BitmapFactory.decodeFile(resource.getAbsolutePath());
                        imageView.setImageBitmap(bitmap);
                        frame.removeView(progressBar);
                    }
                    catch (OutOfMemoryError e)
                    {
                        Utils.showToast("Error: Out of Memory");
                    }
                    catch (Exception e)
                    {
                        Utils.showToast(e.getMessage());
                    }
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder)
                {

                }
            });
            imageView.setOnClickListener(PhotoActivity.this);
            return frame;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object)
        {
            FrameLayout frame = (FrameLayout)object;
            PhotoView view = (PhotoView)frame.getChildAt(0);
            Utils.recycle(view);
            container.removeView(frame);
        }

        @Override
        public int getCount()
        {
            return imageList.size();
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return imageList.get(position).text;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object)
        {
            return view.equals(object);
        }
    }

    private void showSaveResult(File file)
    {
        final Uri uri = FileProvider.getUriForFile(this, "tarn.pantip.fileProvider", file);
        final String type = file.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
        Snackbar snackbar = Snackbar.make(view, "Save as " + file.getPath(), Snackbar.LENGTH_LONG);
        snackbar.setAction("View", v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, type);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });
        snackbar.getView().setBackgroundResource(R.color.colorPrimary);
        view = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        if (view instanceof TextView) ((TextView)view).setTextColor(Color.WHITE);
        view = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_action);
        if (view instanceof TextView) ((TextView)view).setTextColor(ContextCompat.getColor(this, R.color.accent_color_pantip));
        snackbar.show();
    }
}
