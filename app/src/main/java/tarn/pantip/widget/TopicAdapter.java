package tarn.pantip.widget;

import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.BaseActivity;
import tarn.pantip.app.TopicActivity;
import tarn.pantip.content.Json;
import tarn.pantip.model.Topic;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 08 September 2016
 */
public class TopicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private final static int TYPE_ITEM = 0;
    private final static int TYPE_HEADER = 1;
    private final static int TYPE_FOOTER = 2;
    private final BaseActivity activity;
    private final LayoutInflater inflater;
    private View header;
    private final List<Topic> list = new ArrayList<>();
    private OnItemClickListener listener;
    private static final int padding = (int)(Utils.getDimension(R.dimen.avatar_padding) * 0.7f);

    public TopicAdapter(BaseActivity activity)
    {
        this.activity = activity;
        inflater = LayoutInflater.from(activity);
    }

    public void append(View header, Topic[] items, boolean hasMore)
    {
        this.header = header;
        list.add(new Topic());
        RecyclerUtil.append(this, list, items, hasMore);
    }

    public void append(Topic[] items, boolean hasMore)
    {
        RecyclerUtil.append(this, list, items, hasMore);
    }

    public DiffUtil.DiffResult update(Topic[] items, boolean hasMore)
    {
        List<Topic> newList = new ArrayList<>();
        Collections.addAll(newList, items);
        if (list.size() == 0 || list.get(0).id == 0) newList.add(0, new Topic());

        return RecyclerUtil.update(this, list, newList, hasMore, new RecyclerUtil.Callback<Topic>()
        {
            @Override
            protected boolean areItemsTheSame(Topic oldItem, Topic newItem)
            {
                return oldItem.id == newItem.id;
            }

            @Override
            protected boolean areContentsTheSame(Topic oldItem, Topic newItem)
            {
                return (oldItem == null && newItem == null) || (oldItem != null && newItem != null && oldItem.comments == newItem.comments);
            }
        });
    }

    public void clear()
    {
        int count = list.size() - 1;
        while (list.size() > 1) list.remove(1);
        notifyItemRangeRemoved(1, count);
    }

    public void setItems(List<Topic> list)
    {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public Topic[] getItems()
    {
        int from = 0;
        while (from < list.size() && list.get(from).id == 0) from++;
        int to = list.size();
        if (to > 500) to = 500;
        while (from < to && list.get(to - 1) == null) to--;
        return from <= to ? list.subList(from, to).toArray(new Topic[to - from]) : new Topic[0];
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == TYPE_HEADER)
        {
            header.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new HeaderViewHolder(header);
        }
        if (viewType == TYPE_FOOTER) return new LoadMoreViewHolder(inflater, parent);
        return new ViewHolder(inflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if (getItemViewType(position) == TYPE_ITEM) bindItemViewHolder((ViewHolder)holder, position);
    }

    private void bindItemViewHolder(ViewHolder holder, int position)
    {
        Topic topic = list.get(position);
        boolean unread = Pantip.getDataStore().readCount(topic.id) == 0;

        setTitle(holder.title, topic.type, topic.title, unread ? 0 : Pantip.textColorTertiary);
        String author = topic.author + " ";
        holder.author.setText(author);
        holder.timestamp.setText(topic.getRelativeTime());
        holder.comments.setText(topic.getStatText());

        holder.title.setTextColor(unread ? Pantip.textColor : Pantip.textColorSecondary);
        holder.author.setTextColor(unread ? Pantip.authorColor : Pantip.textColorTertiary);
        holder.timestamp.setTextColor(unread ? Pantip.textColorSecondary : Pantip.textColorTertiary);
        holder.comments.setTextColor(unread ? Pantip.textColorSecondary : Pantip.textColorTertiary);

        holder.itemView.setAlpha(unread ? 1 : Pantip.readAlpha);
    }

    public static void setTitle(TextView textView, TopicType type, CharSequence title, int tint)
    {
        if (type == null)
        {
            textView.setText(title);
            return;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder(title);
        builder.insert(0, "  "); // use no-break space character to make icon shown

        TarnImageSpan image = new TarnImageSpan(textView, type.getIcon(), tint);
        builder.setSpan(image, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        textView.setText(builder);
    }

    @Override
    public int getItemCount()
    {
        return list.size();
    }

    @Override
    public long getItemId(int position)
    {
        Topic topic = getItem(position);
        return topic == null ? 0 : topic.id;
    }

    public Topic getItem(int position)
    {
        return (position < 0 || position >= list.size() || list.get(position) == null) ? null : list.get(position);
    }

    public int getPosition(Topic item)
    {
        for (int i = 0; i < list.size(); i++)
        {
            Topic topic = list.get(i);
            if (topic != null && item.id == topic.id) return i;
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position)
    {
        if (list.get(position) == null) return TYPE_FOOTER;
        return list.get(position).id == 0 ? TYPE_HEADER : TYPE_ITEM;
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder
    {
        HeaderViewHolder(View itemView)
        {
            super(itemView);
        }
    }

    private class ViewHolder extends ViewHolderOnClick
    {
        final TextView title;
        final TextView author;
        final TextView timestamp;
        final TextView comments;

        ViewHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.topic_item, parent, false));
            title = itemView.findViewById(R.id.title);
            title.setTextSize(Pantip.textSize);
            author = itemView.findViewById(R.id.author);
            author.setTextSize(Pantip.textSize - 1);
            timestamp = itemView.findViewById(R.id.timestamp);
            timestamp.setTextSize(Pantip.textSize - 3);
            comments = itemView.findViewById(R.id.comments);
            comments.setTextSize(Pantip.textSize - 3);

            init(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(int position)
        {
            Topic topic = getItem(position);
            if (topic == null) return;

            Pantip.getDataStore().markAsRead(topic.id);
            if (listener != null) listener.onItemClick(topic);
            Intent intent = new Intent(activity, TopicActivity.class);
            intent.putExtra("topic", Json.toJson(topic));
            activity.startActivity(intent);

            notifyItemChanged(position);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.listener = listener;
    }

    public interface OnItemClickListener
    {
        void onItemClick(Topic topic);
    }

    public static void init(View itemView)
    {
        itemView.setBackgroundResource(Pantip.selectableItemBackground);
        ViewGroup.MarginLayoutParams layout = (ViewGroup.MarginLayoutParams)itemView.getLayoutParams();
        layout.leftMargin = layout.rightMargin = Utils.getDimension(R.dimen.list_margin);
        itemView.setPadding(Pantip.spacer, padding, Pantip.spacer, padding);
    }
}