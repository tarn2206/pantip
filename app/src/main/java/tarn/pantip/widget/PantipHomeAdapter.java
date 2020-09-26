package tarn.pantip.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.TopicActivity;
import tarn.pantip.content.HomeData;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 21 December 2016
 */

public class PantipHomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private final Context context;
    private final LayoutInflater inflater;
    public final List<HomeData> list = new ArrayList<>();
    private static int maxWidth;
    private static int maxHeight;

    public PantipHomeAdapter(Context context)
    {
        this.context = context;
        inflater = LayoutInflater.from(context);
        onConfigurationChanged(context.getResources().getConfiguration());
    }

    public void onConfigurationChanged(Configuration newConfig)
    {
        boolean wideScreen = context.getResources().getBoolean(R.bool.wide);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || wideScreen)
            maxWidth = Utils.getDisplaySize().x / 3;
        else
            maxWidth = Utils.getDisplaySize().x - Utils.toPixels(28);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && wideScreen)
            maxHeight = (int)(maxWidth / 2f);
        else
            maxHeight = (int)(maxWidth * 2 / 3f);
    }

    @Override
    public int getItemCount()
    {
        return list.size();
    }

    public HomeData getItem(int position)
    {
        return position < list.size() ? list.get(position) : null;
    }

    public int getPosition(HomeData item)
    {
        for (int i = 0; i < list.size(); i++)
        {
            HomeData data = list.get(i);
            if (data != null && item.topic_id == data.topic_id) return i;
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position)
    {
        HomeData item = list.get(position);
        if (item == null) return 0;
        return StringUtils.isBlank(item.thumbnail_url) ? 1 : 2;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == 0) return new LoadMoreViewHolder(inflater, parent);
        View itemView = viewType == 1
                      ? inflater.inflate(R.layout.pantip_home_item, parent, false)
                      : inflater.inflate(R.layout.pantip_home_item_with_image, parent, false);
        return viewType == 1 ? new ViewHolder(itemView) : new ViewHolderWithImage(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        HomeData item = list.get(position);
        if (item == null) return;
        if (!item.unescaped)
        {
            item.unescaped = true;
            if (item.title != null)
                item.title = StringEscapeUtils.unescapeHtml4(item.title);
            if (item.author != null && item.author.name != null)
                item.author.name = StringEscapeUtils.unescapeHtml4(item.author.name);
        }
        bindViewHolder((ViewHolder)holder, item);
        if (StringUtils.isNotBlank(item.thumbnail_url))
        {
            bindViewHolderWithImage((ViewHolderWithImage)holder, item);
        }
    }

    private void bindViewHolder(ViewHolder holder, HomeData item)
    {
        boolean unread = Pantip.getDataStore().readCount(item.topic_id) == 0;
        TopicAdapter.setTitle(holder.title, TopicType.fromValue(item.topic_type), item.title, unread ? 0 : Pantip.textColorTertiary);
        String author = item.author.name + " ";
        holder.author.setText(author);
        holder.timestamp.setText(item.getRelativeTime());
        holder.comments.setText(item.getStatText());

        holder.title.setTextColor(unread ? Pantip.textColor : Pantip.textColorSecondary);
        holder.author.setTextColor(unread ? Pantip.authorColor : Pantip.textColorTertiary);
        holder.timestamp.setTextColor(unread ? Pantip.textColorSecondary : Pantip.textColorTertiary);
        holder.comments.setTextColor(unread ? Pantip.textColorSecondary : Pantip.textColorTertiary);

        holder.itemView.setAlpha(unread ? 1 : Pantip.readAlpha);
    }

    private void bindViewHolderWithImage(ViewHolderWithImage holder, HomeData item)
    {
        long id = holder.imageView.getTag() == null ? 0 : (long)holder.imageView.getTag();
        if (id != item.topic_id) holder.imageView.setImageBitmap(null);
        holder.imageView.getLayoutParams().height = maxHeight;
        GlideApp.with(context).downloadOnly().load(item.thumbnail_url).into(new MyTarget(holder.imageView, item));
    }

    public void append(HomeData[] items, boolean hasMore)
    {
        RecyclerUtil.append(this, list, items, hasMore);
    }

    public void update(HomeData[] items, boolean hasMore)
    {
        RecyclerUtil.update(this, list, items, hasMore, new RecyclerUtil.Callback<HomeData>()
        {
            @Override
            protected boolean areItemsTheSame(HomeData oldItem, HomeData newItem)
            {
                return oldItem.topic_id == newItem.topic_id;
            }
        });
    }

    class ViewHolder extends ViewHolderOnClick
    {
        final TextView title;
        final TextView author;
        final TextView timestamp;
        final TextView comments;

        ViewHolder(View itemView)
        {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            title.setTextSize(Pantip.textSize);
            author = itemView.findViewById(R.id.author);
            author.setTextSize(Pantip.textSize - 1);
            timestamp = itemView.findViewById(R.id.timestamp);
            timestamp.setTextSize(Pantip.textSize - 3);
            comments = itemView.findViewById(R.id.comments);
            comments.setTextSize(Pantip.textSize - 3);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(int position)
        {
            if (position < 0 || position >= list.size()) return;

            HomeData data = list.get(position);
            Pantip.getDataStore().markAsRead(data.topic_id);

            Intent intent = new Intent(context, TopicActivity.class);
            intent.putExtra("id", data.topic_id);
            context.startActivity(intent);

            itemView.postDelayed(() -> notifyItemChanged(position), 1000);
        }
    }

    class ViewHolderWithImage extends ViewHolder
    {
        final ImageView imageView;

        ViewHolderWithImage(View itemView)
        {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(Pantip.backgroundSecondary);
        }
    }

    static class MyTarget extends CustomTarget<File>
    {
        private final ImageView view;
        private final HomeData data;

        MyTarget(ImageView view, HomeData data)
        {
            this.view = view;
            this.data = data;
        }

        @Override
        public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition)
        {
            decodeFile(resource).subscribe(result -> {
                if (result.viewHeight == 0) result.viewHeight = data.view_height;
                else data.view_height = result.viewHeight;
                if (result.viewHeight > 0) view.getLayoutParams().height = result.viewHeight;
                view.setImageBitmap(result.bm);
                view.setTag(data.topic_id);
                Utils.fadeIn(view);
            }, L::e);
        }

        @Override
        public void onLoadCleared(@Nullable Drawable placeholder)
        { }
    }

    private static class DecodeResult
    {
        Bitmap bm;
        int viewHeight;
    }

    private static Observable<DecodeResult> decodeFile(File resource)
    {
        return RxUtils.observe(() -> {
            DecodeResult result = new DecodeResult();

            String key = "HOME:" + resource.getName();
            result.bm = Pantip.imageCache.get(key);
            if (result.bm != null) return result;

            String fileName = resource.getAbsolutePath();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, options);

            int width = options.outWidth;
            int height = options.outHeight;

            result.viewHeight = (int)(maxWidth * height / (float)width);
            if (result.viewHeight > maxHeight) result.viewHeight = maxHeight;

            options.inSampleSize = Utils.calcSampleSize(width, height, maxWidth, maxHeight);
            options.inJustDecodeBounds = false;
            result.bm = BitmapFactory.decodeFile(fileName, options);
            if (result.bm != null) Pantip.imageCache.add(key, result.bm);
            return result;
        });
    }
}
