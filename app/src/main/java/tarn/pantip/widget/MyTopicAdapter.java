package tarn.pantip.widget;

import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.BaseActivity;
import tarn.pantip.app.MyTopicFragment;
import tarn.pantip.app.TopicActivity;
import tarn.pantip.content.Json;
import tarn.pantip.model.Topic;

public class MyTopicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private final static int ITEM = 0;
    private final static int LOAD_MORE = 1;
    private final BaseActivity activity;
    private final LayoutInflater inflater;
    private final boolean notifyFavoriteChange;
    private final List<Topic> list = new ArrayList<>();

    public MyTopicAdapter(BaseActivity activity, boolean notifyFavoriteChange)
    {
        this.activity = activity;
        inflater = LayoutInflater.from(activity);
        this.notifyFavoriteChange = notifyFavoriteChange;
    }

    @Override
    public int getItemViewType(int position)
    {
        return list.get(position) == null ? LOAD_MORE : ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return viewType == ITEM
                ? new ViewHolder(inflater, parent)
                : new LoadMoreViewHolder(inflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if (getItemViewType(position) == ITEM) bindViewHolder((ViewHolder)holder, list.get(position));
    }

    private void bindViewHolder(ViewHolder holder, Topic topic)
    {
        boolean deleted = topic.status == 2;
        boolean normal = Pantip.getDataStore().readCount(topic.id) == 0 && !deleted;
        CharSequence title;
        if (deleted)
        {
            SpannableString s = new SpannableString(topic.title);
            s.setSpan(new StrikethroughSpan(), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            title = s;
        }
        else title = topic.title;
        TopicAdapter.setTitle(holder.title, topic.type, title, normal ? 0 : (deleted ? Pantip.textColorHint : Pantip.textColorTertiary));
        String author = topic.author + " ";
        holder.author.setText(author);
        holder.timestamp.setText(topic.getRelativeTime());
        holder.comments.setText(topic.getStatText());

        holder.title.setTextColor(normal ? Pantip.textColor : (deleted ? Pantip.textColorHint : Pantip.textColorSecondary));
        holder.author.setTextColor(normal ? Pantip.authorColor : (deleted ? Pantip.textColorHint : Pantip.textColorTertiary));
        holder.timestamp.setTextColor(normal ? Pantip.textColorSecondary : (deleted ? Pantip.textColorHint : Pantip.textColorTertiary));
        holder.comments.setTextColor(normal ? Pantip.textColorSecondary : (deleted ? Pantip.textColorHint : Pantip.textColorTertiary));

        holder.itemView.setAlpha(normal ? 1 : Pantip.readAlpha);
    }

    @Override
    public int getItemCount()
    {
        return list.size();
    }

    public void append(Topic[] items, boolean hasMore)
    {
        RecyclerUtil.append(this, list, items, hasMore);
    }

    public void update(Topic[] items, boolean hasMore)
    {
        RecyclerUtil.update(this, list, items, hasMore, new RecyclerUtil.Callback<Topic>()
        {
            @Override
            protected boolean areItemsTheSame(Topic oldItem, Topic newItem)
            {
                return oldItem.id == newItem.id;
            }
        });
    }

    public boolean remove(long topicId)
    {
        for (int i = 0; i < list.size(); i++)
        {
            Topic topic = list.get(i);
            if (topic != null && topic.id == topicId)
            {
                list.remove(i);
                notifyItemRemoved(i);
                return true;
            }
        }
        return false;
    }

    public Topic[] getItems()
    {
        int n = list.size();
        if (n == 0) return new Topic[0];

        if (list.get(n - 1) == null) n--;
        return list.subList(0, n).toArray(new Topic[n]);
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

            TopicAdapter.init(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(int position)
        {
            if (position < 0 || position >= list.size()) return;

            Topic topic = list.get(position);
            if (notifyFavoriteChange) topic.favorite = true;
            Pantip.getDataStore().markAsRead(topic.id);

            Intent intent = new Intent(activity, TopicActivity.class);
            intent.putExtra("topic", Json.toJson(topic));
            if (notifyFavoriteChange) activity.startActivityForResult(intent, MyTopicFragment.RC_FAVORITES);
            else activity.startActivity(intent);

            notifyItemChanged(position);
        }
    }
}
