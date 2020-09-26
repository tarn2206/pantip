package tarn.pantip.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.IntentActivity;
import tarn.pantip.model.SearchResultItem;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 04 August 2017 10:26
 */

public class GoogleSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 2;
    private final Context context;
    private final LayoutInflater inflater;
    private final List<SearchResultItem> list = new ArrayList<>();

    public GoogleSearchAdapter(Context context)
    {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == TYPE_FOOTER) return new LoadMoreViewHolder(inflater, parent);
        return new ViewHolder(inflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if (getItemViewType(position) == TYPE_ITEM) bindItemViewHolder((GoogleSearchAdapter.ViewHolder)holder, position);
    }

    private void bindItemViewHolder(GoogleSearchAdapter.ViewHolder holder, int position)
    {
        SearchResultItem item = list.get(position);

        long id = item.getId();
        boolean unread = id <= 0 || Pantip.getDataStore().readCount(id) == 0;

        holder.title.setText(item.title.getText());
        holder.content.setText(item.content.getText());
        holder.url.setText(item.url);

        holder.title.setTextColor(unread ? Pantip.textColor : Pantip.textColorSecondary);
        holder.content.setTextColor(unread ? Pantip.textColorSecondary : Pantip.textColorTertiary);
        holder.url.setTextColor(unread ? Pantip.authorColor : Pantip.textColorTertiary);

        holder.itemView.setAlpha(unread ? 1 : Pantip.readAlpha);
    }

    @Override
    public int getItemCount()
    {
        return list.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        return list.get(position) == null ? TYPE_FOOTER : TYPE_ITEM;
    }

    public void clear()
    {
        list.clear();
    }

    public void appendItems(List<SearchResultItem> items, boolean hasMore)
    {
        if (list.size() > 0 && list.get(list.size() - 1) == null)
            list.remove(list.size() - 1);
        if (items != null)
        {
            for (SearchResultItem item : items) if (item != null) list.add(item);
        }
        if (hasMore && list.size() > 0 && list.get(list.size() - 1) != null) list.add(null);
        notifyDataSetChanged();
    }

    public List<SearchResultItem> getItems()
    {
        List<SearchResultItem> items = new ArrayList<>();
        for (SearchResultItem item : list)
            if (item != null) items.add(item);
        return items;
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        final TextView title;
        final TextView content;
        final TextView url;

        ViewHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.search_item, parent, false));
            title = itemView.findViewById(R.id.title);
            title.setTextSize(Pantip.textSize);
            content = itemView.findViewById(R.id.content);
            content.setTextSize(Pantip.textSize);
            url = itemView.findViewById(R.id.url);
            url.setTextSize(Pantip.textSize - 1);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            final int position = getAdapterPosition();
            if (position >= list.size()) return;

            SearchResultItem item = list.get(position);
            if (IntentActivity.openUrl(context, item.url))
            {
                itemView.postDelayed(() -> notifyItemChanged(position), 1000);
            }
            else Utils.openBrowser(context, item.url);
        }
    }
}
