package tarn.pantip.widget;

import android.content.Intent;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.ConfirmDialog;
import tarn.pantip.app.NotificationActivity;
import tarn.pantip.app.TopicActivity;
import tarn.pantip.content.Bookmark;
import tarn.pantip.content.Json;
import tarn.pantip.content.Notify;
import tarn.pantip.content.Preferences;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 21/1/2561.
 */

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private final NotificationActivity context;
    private final LayoutInflater inflater;
    private final List<Notify> list = new ArrayList<>();
    private final int avatarSize;
    private final int replyTextSize;

    public NotificationAdapter(NotificationActivity context)
    {
        this.context = context;
        inflater = LayoutInflater.from(context);
        replyTextSize = Pantip.textSize - 1;
        avatarSize = (int)(2 * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, replyTextSize, context.getResources().getDisplayMetrics()));
    }

    public void append(Notify[] result)
    {
        if (result == null || result.length == 0) return;
        int start = list.size();
        Collections.addAll(list, result);
        notifyItemRangeInserted(start, result.length);
    }

    public void update(Notify[] result)
    {
        RecyclerUtil.update(this, list, result, false, new RecyclerUtil.Callback<Notify>()
        {
            @Override
            protected boolean areItemsTheSame(Notify oldItem, Notify newItem)
            {
                return StringUtils.equals(oldItem.url, newItem.url);
            }

            @Override
            protected boolean areContentsTheSame(Notify oldItem, Notify newItem)
            {
                return oldItem.isNew == newItem.isNew;
            }
        });
    }

    public boolean remove(long topicId)
    {
        int start = -1;
        int count = 0;
        for (int i = list.size() - 1; i >= 0; i--)
        {
            Notify n = list.get(i);
            if (n.getTopicId() == topicId)
            {
                list.remove(i);
                start = i;
                count++;
            }
        }
        if (start > -1) notifyItemRangeRemoved(start, count);
        return count > 0;
    }

    public List<Notify> getItems()
    {
        return list;
    }

    @Override
    public int getItemCount()
    {
        return list.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        return list.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return viewType == 0 ? new ViewHolder(inflater, parent) : new ReplyViewHolder(inflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        Notify n = list.get(position);
        if (n.type == 0) bind((ViewHolder)holder, n);
        else bind((ReplyViewHolder)holder, n);
    }

    private void bind(ViewHolder holder, Notify n)
    {
        holder.topic.setText(n.text);
        holder.topic.setTextColor(n.isNew ? Pantip.topicTitleColor : Pantip.textColor);
        holder.itemView.setAlpha(n.isNew ? 1 : Pantip.readAlpha);
    }

    private void bind(ReplyViewHolder holder, Notify n)
    {
        if (n.avatar == null || n.avatar.startsWith("/images/unknown-avatar"))
            holder.avatar.setImageResource(Pantip.currentTheme == R.style.AppTheme ? R.drawable.avatar_pantip : R.drawable.avatar_light);
        else
        {
            String url = n.avatar;
            if (url.startsWith("http://")) url = "https://" + url.substring(7);
            GlideApp.with(context).load(url)
                    .transform(new CircleCrop()).into(holder.avatar);
        }
        holder.text.setText(n.text);

        holder.text.setTextColor(n.isNew ? Pantip.textColor : Pantip.textColorSecondary);
        holder.itemView.setAlpha(n.isNew ? 1 : Pantip.readAlpha);
    }

    private class ViewHolder extends ViewHolderOnClick implements View.OnLongClickListener
    {
        final TextView topic;

        ViewHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.notify_item, parent, false));
            topic = (TextView)itemView;
            topic.setTextSize(Pantip.textSize);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setBackgroundResource(Pantip.selectableItemBackground);
        }

        @Override
        public void onClick(int position)
        {
            if (position < 0 || position >= list.size()) return;

            Notify n = list.get(position);
            Intent intent = new Intent(context, TopicActivity.class);
            intent.putExtra("id", n.getTopicId());
            context.startActivityForResult(intent, NotificationActivity.FOLLOW);

            markAsRead(position);
        }

        @Override
        public boolean onLongClick(View v)
        {
            final int position = getAdapterPosition();
            if (position < 0 || position >= list.size())
            {
                return false;
            }

            final Notify n = list.get(position);
            if (n.type != 0) return false;

            ConfirmDialog.delete(context, "เลิกติดตามกระทู้", n.text, "เลิกติดตาม", (dialog, which) -> unFollow(position, n));
            return false;
        }
    }

    private void unFollow(int position, Notify n)
    {
        Bookmark.unfollow(n.getTopicId())
                .subscribe(result -> {
                    list.remove(position);
                    int count = 1;
                    while (position < list.size() && list.get(position).type == 1)
                    {
                        list.remove(position);
                        count++;
                    }
                    notifyItemRangeRemoved(position, count);
                }, this::onError);
    }

    private void onError(Throwable tr)
    {
        L.e(tr);
        Utils.showToast(context, tr.getMessage());
    }

    private class ReplyViewHolder extends ViewHolderOnClick
    {
        final ImageView avatar;
        final TextView text;

        ReplyViewHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.notify_event, parent, false));
            avatar = itemView.findViewById(R.id.avatar);
            avatar.getLayoutParams().width = avatarSize;
            avatar.getLayoutParams().height = avatarSize;
            text = itemView.findViewById(R.id.text);
            text.setTextSize(replyTextSize);
            text.setMinHeight(avatarSize);
            text.setGravity(Gravity.CENTER_VERTICAL);
            itemView.setOnClickListener(this);
            itemView.setBackgroundResource(Pantip.selectableSecondaryBackground);
        }

        @Override
        public void onClick(int position)
        {
            if (position < 0 || position >= list.size()) return;

            Notify n = list.get(position);
            String[] a = n.url.split("/");
            Intent intent = new Intent(context, TopicActivity.class);
            intent.putExtra("id", Long.parseLong(a[a.length - 2]));
            intent.putExtra("goTo", a[a.length - 1].substring(7));
            context.startActivityForResult(intent, NotificationActivity.FOLLOW);

            markAsRead(position);
        }
    }

    private void markAsRead(int position)
    {
        try
        {
            while (position > 0 && list.get(position).type == 1) position--;
            list.get(position).isNew = false;
            int i = position + 1;
            while (i < list.size() && list.get(i).type == 1)
            {
                list.get(i++).isNew = false;
            }
            notifyItemRangeChanged(position, i - position);

            Preferences preferences = new Preferences(context);
            preferences.putString("data", Json.toJson(list)).commit();
        }
        catch (Exception e)
        {
            L.e(e);
        }
    }
}
