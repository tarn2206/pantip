package tarn.pantip.widget;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.BaseActivity;
import tarn.pantip.app.PhotoActivity;
import tarn.pantip.app.SpoilDialog;
import tarn.pantip.content.Json;
import tarn.pantip.model.Size;
import tarn.pantip.model.Story;
import tarn.pantip.model.StoryType;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Optional;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

import static android.widget.ImageView.ScaleType.CENTER_INSIDE;
import static android.widget.ImageView.ScaleType.FIT_CENTER;

/**
 * User: tarn
 * Date: 1/20/13 11:17 AM
 */
public class PhotoLayout extends FrameLayout implements View.OnClickListener
{
    private ImageView imageView;
    private ImageView play;

    private final Story story;
    private final BaseActivity activity;
    private String imageUrl;
    private String link;
    private boolean loading;
    private boolean loaded;
    private boolean largeImage;
    private final boolean isReply;
    private final SpoilDialog dialog;
    private boolean recycled;
    private final List<Story>imageList;
    private final int imageIndex;
    private final long topicId;
    private Bitmap bitmapRef;

    public PhotoLayout(BaseActivity activity, Story story, boolean isReply)
    {
        this(activity, story, isReply, 0, null, 0, null);
    }

    public PhotoLayout(BaseActivity activity, Story story, boolean isReply, long topicId, List<Story> imageList, int imageIndex)
    {
        this(activity, story, isReply, topicId, imageList, imageIndex, null);
    }

    public PhotoLayout(BaseActivity activity, Story story, long topicId, List<Story> imageList, int imageIndex, SpoilDialog dialog)
    {
        this(activity, story, false, topicId, imageList, imageIndex, dialog);
    }

    private PhotoLayout(BaseActivity activity, Story story, boolean isReply, long topicId, List<Story> imageList, int imageIndex, SpoilDialog dialog)
    {
        super(activity);
        this.activity = activity;
        this.topicId = topicId;
        this.isReply = isReply;
        this.story = story;
        this.imageList = imageList;
        this.imageIndex = imageIndex;
        this.dialog = dialog;
        try
        {
            detectSource();
            initLayout();
            //if (story.thumbSize != null) loadImage();
        }
        catch (Exception e)
        {
            Pantip.handleException(activity, e);
        }
    }

    private void detectSource()
    {
        String src = story.text;
        if (story.type == StoryType.YouTube)
        {
            String id = Utils.getYouTubeId(src);
            if (id == null) imageUrl = src;
            else
            {
                imageUrl = "https://img.youtube.com/vi/" + id + "/0.jpg";
                link = "https://www.youtube.com/watch?v=" + id;
            }
        }
        else imageUrl = src;
    }

    private void initLayout()
    {
        LayoutInflater.from(activity).inflate(R.layout.photo_layout, this, true);
        if (dialog == null)
        {
            int verticalPadding = Utils.toPixels(14);
            setPadding(0, verticalPadding, 0, verticalPadding);
        }

        imageView = findViewById(R.id.photo_view);
        imageView.setBackgroundColor(Pantip.backgroundSecondary);
        //if (BuildConfig.DEBUG) imageView.setBackgroundColor(0x99009900);
        play = findViewById(R.id.play);
        play.setVisibility(GONE);

        if ((story.type == StoryType.YouTube || story.type == StoryType.Maps) && StringUtils.isNotBlank(link))
        {
            final Uri uri = Uri.parse(link);
            imageView.setOnClickListener(v -> Utils.startActivity(activity, new Intent(Intent.ACTION_VIEW, uri)));
        }
    }

    public void loadImage()
    {
        if (loading || loaded) return;

        loading = true;
        //startShimmer();
        if (story.thumbSize != null) setImageViewSize(story.thumbSize);

        GlideApp.with(activity).downloadOnly().load(imageUrl).into(target);
    }

    private final CustomTarget<File> target = new CustomTarget<File>()
    {
        @Override
        public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition)
        {
            if (recycled) return;
            //stopShimmer();
            if (story.fullSize == null)
            {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(resource.getAbsolutePath(), options);
                story.fullSize = new Size(options.outWidth, options.outHeight);
                //L.d("get full image size " + imageUrl);
            }
            //if (story.thumbSize == null)
            if (imageUrl.endsWith(".gif"))
            {
                story.thumbSize = new Size(story.fullSize);
                if (story.fullSize.width < Pantip.displayWidth / 5)
                {
                    story.thumbSize.width *= 2.5;
                    story.thumbSize.height *= 2.5;
                }
                else if (story.fullSize.width < Pantip.displayWidth / 4)
                {
                    story.thumbSize.width *= 2;
                    story.thumbSize.height *= 2;
                }
                else if (story.fullSize.width < Pantip.displayWidth / 3)
                {
                    story.thumbSize.width *= 1.75f;
                    story.thumbSize.height *= 1.75f;
                }
                else if (story.fullSize.width < Pantip.displayWidth / 2)
                {
                    story.thumbSize.width *= 1.5f;
                    story.thumbSize.height *= 1.5f;
                }
            }
            else
            {
                int thumbWidth = (int)(story.fullSize.width * getResources().getDisplayMetrics().density);
                if (dialog == null)
                {
                    if (story.type == StoryType.YouTube || story.type == StoryType.Maps || thumbWidth > Pantip.displayWidth)
                    {
                        largeImage = true;
                        thumbWidth = Pantip.displayWidth;
                        if (Pantip.xLarge) thumbWidth -= activity.getResources().getDimension(R.dimen.comment_padding) * 2;
                        if (isReply)
                        {
                            thumbWidth -= CommentAdapter.padding * 2;
                            if (Pantip.xLarge) thumbWidth -= 8; // comment background space
                        }
                    }
                    else largeImage = thumbWidth > Pantip.displayWidth / 2;
                }
                else
                {
                    if (thumbWidth > Pantip.displayWidth)
                    {
                        largeImage = true;
                        int width = Pantip.displayWidth;
                        if (Pantip.xLarge && width - Utils.toPixels(150) > Pantip.displayWidth / 2) width -= Utils.toPixels(150);
                        dialog.setWidth(width);
                        thumbWidth = width - Utils.toPixels(52);
                    }
                    else largeImage = thumbWidth > Pantip.displayWidth / 2;
                }
                int thumbHeight = Math.round(thumbWidth * story.fullSize.height / (float)story.fullSize.width);
                story.thumbSize = new Size(thumbWidth, thumbHeight);
            }
            setImageViewSize(story.thumbSize);
            if (dialog != null) dialog.setWidth(story.thumbSize.width + Utils.toPixels(52));

            if (imageUrl.endsWith(".gif"))
            {
                // take a time to generate scaled gif
                // do not set scaleType to ImageView
                RequestBuilder<GifDrawable> request = GlideApp.with(activity).asGif().load(imageUrl).listener(gifListener);
                try
                {
                    request.into(imageView);
                }
                catch (OutOfMemoryError e)
                {/*ignored*/}
                return;
            }

            loading = false;
            loaded = true;

            final File thumb = new File(resource.getAbsolutePath() + ".thumb");
            //L.i("%s : %d", thumb.getAbsolutePath(), thumb.length());
            if (thumb.exists() && thumb.length() > 0)
            {
                decode(thumb).subscribe(result -> setImageBitmap(result.orElse(null)),
                        tr -> showDefaultImage(false));
            }
            else
            {
                createThumbnail(story, resource, thumb).subscribe(result -> {
                    if (!result.isPresent())
                    {
                        showDefaultImage(true);
                        if (story.type == StoryType.Image)
                        {
                            loading = loaded = false;
                            imageView.setOnClickListener(v -> loadImage());
                        }
                    }
                    else if (recycled)
                    {
                        result.get().recycle();
                    }
                    else setImageBitmap(result.get());
                }, tr -> {
                    showDefaultImage(false);
                    FileUtils.deleteQuietly(thumb);
                    if (tr.getMessage() != null && tr.getMessage().contains("Permission denied")) Utils.showAppSettings(activity);
                    else if (tr instanceof OutOfMemoryError) Utils.showToast("Out of Memory");
                    else L.e(tr);
                });
            }

            if (story.type == StoryType.YouTube)
            {
                play.setVisibility(VISIBLE);
            }
            else if (story.type == StoryType.Image && largeImage)
            {
                imageView.setOnClickListener(PhotoLayout.this);
            }
        }

        @Override
        public void onLoadCleared(@Nullable Drawable placeholder)
        {

        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable)
        {
            loading = false;
            //stopShimmer();
            showDefaultImage(true);
            if (story.type == StoryType.YouTube || story.type == StoryType.Maps)
            {
                if (story.type == StoryType.YouTube)
                {
                    play.setVisibility(VISIBLE);
                }
                return;
            }
            imageView.setOnClickListener(v -> {
                imageView.setOnClickListener(null);
                imageView.setImageDrawable(null);
                //startShimmer();
                postDelayed(PhotoLayout.this::loadImage, 800);
            });
        }
    };

    @Override
    public void onClick(View v)
    {
        if (PhotoActivity.currentPhoto != null) return;
        PhotoActivity.currentPhoto = imageView;
        Intent intent = new Intent(activity, PhotoActivity.class);
        intent.putExtra("topic_id", topicId);
        intent.putExtra("url", imageUrl);
        if (imageList != null) intent.putExtra("imageList", Json.toJson(imageList));
        intent.putExtra("imageIndex", imageIndex);

        int[] location = new int[2];
        imageView.getLocationOnScreen(location);
        int width = story.thumbSize.width;
        int height = story.thumbSize.height;
        Rect startBounds = new Rect(location[0], location[1], location[0] + width, location[1] + height);
        //L.i("%s %dx%d", startBounds, startBounds.width(), startBounds.height());
        intent.putExtra("dialog", dialog != null);
        intent.putExtra("orientation", getResources().getConfiguration().orientation);
        intent.putExtra("startBounds", startBounds);
        if (dialog == null)
        {
            intent.putExtra("toolbarHeight", activity.getToolbarHeight());
        }
        else
        {
            ViewGroup content = dialog.findViewById(android.R.id.content);
            if (content != null && content.getChildCount() > 0)
            {
                View view = content.getChildAt(0);
                view.getLocationOnScreen(location);
                Rect rect = new Rect(0, location[1], Pantip.displayWidth, location[1] + view.getHeight());
                intent.putExtra("dialogBounds", rect);
            }
        }
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    private static Observable<Optional<Bitmap>> createThumbnail(Story story, File resource, File thumb)
    {
        return RxUtils.observe(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Utils.calcSampleSize(story.fullSize.width, story.fullSize.height, story.thumbSize.width, story.thumbSize.height);
            Bitmap bm = BitmapFactory.decodeFile(resource.getAbsolutePath(), options);
            if (bm != null && options.inSampleSize > 1)
            {
                try (FileOutputStream out = new FileOutputStream(thumb))
                {
                    Bitmap.CompressFormat format = "image/png".equals(options.outMimeType) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                    bm.compress(format, 80, out);
                }
            }
            return Optional.of(bm);
        });
    }

    private void setImageBitmap(Bitmap bm)
    {
        imageView.setImageBitmap(bm);
        imageView.setScaleType(FIT_CENTER);
        imageView.setBackgroundColor(Color.TRANSPARENT);
        Utils.fadeIn(imageView);
        recycleBitmap();
        bitmapRef = bm;
    }

    private Observable<Optional<Bitmap>> decode(File file)
    {
        return RxUtils.observe(() -> {
            String path = file.getAbsolutePath();
            Bitmap bm = Pantip.imageCache.get(path);
            if (bm == null)
            {
                bm = BitmapFactory.decodeFile(path);
                Pantip.imageCache.add(path, bm);
            }
            return Optional.of(bm);
        });
    }

    public void recycle()
    {
        imageView.setImageDrawable(null);
        recycleBitmap();
        recycled = true;
    }

    private void recycleBitmap()
    {
        if (bitmapRef != null)
        {
            bitmapRef.recycle();
            bitmapRef = null;
        }
    }

    private final RequestListener<GifDrawable> gifListener = new RequestListener<GifDrawable>()
    {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource)
        {
            loading = false;
            loaded = true;
            //stopShimmer();
            showDefaultImage(true);
            return false;
        }

        @Override
        public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource)
        {
            loading = false;
            loaded = true;
            //stopShimmer();
            imageView.setBackgroundColor(Color.TRANSPARENT);
            return false;
        }
    };

    private void showDefaultImage(boolean error)
    {
        imageView.setScaleType(CENTER_INSIDE);
        try
        {
            if (error)
            {
                setImageViewSize(Pantip.imagePlaceholderSize);
                imageView.setImageResource(R.drawable.ic_report_black_48dp);
            }
            else
            {
                Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.pantip_logo);
                if (drawable != null)
                {
                    drawable = DrawableCompat.wrap(drawable);
                    DrawableCompat.setTint(drawable.mutate(), Pantip.textColorTertiary);
                }
                imageView.setImageDrawable(drawable);
            }
        }
        catch (Throwable tr)
        {
            imageView.setImageDrawable(null);
        }
    }

    private void setImageViewSize(Size size)
    {
        getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
        imageView.getLayoutParams().width = size.width;
        imageView.getLayoutParams().height = size.height;
    }
}