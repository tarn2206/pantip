package tarn.pantip.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.R;
import tarn.pantip.content.Gallery;
import tarn.pantip.util.GlideApp;

public class MyGalleryAdapter extends RecyclerView.Adapter<MyGalleryAdapter.ViewHolder>
{
    final List<Gallery> list = new ArrayList<>();
    final List<Gallery> selected = new ArrayList<>();
    private final int size;
    private MyGalleryView.OnSelectListener listener;

    MyGalleryAdapter(int size)
    {
        this.size = size;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Gallery item = list.get(position);
        holder.imageView.setImageDrawable(null);
        holder.updateSelect(item.selected);
        RequestOptions options = new RequestOptions().centerCrop();
        GlideApp.with(holder.imageView).load(item.url).apply(options).into(holder.imageView);
    }

    @Override
    public int getItemCount()
    {
        return list.size();
    }

    public Observable<Void> save()
    {
        return Gallery.save(list);
    }

    public void update(List<Gallery> newItems)
    {
        RecyclerUtil.update(this, list, newItems, false, new RecyclerUtil.Callback<Gallery>()
        {
            @Override
            protected boolean areItemsTheSame(Gallery oldItem, Gallery newItem)
            {
                if (oldItem.id == newItem.id) newItem.selected = oldItem.selected;
                return oldItem.id == newItem.id;
            }
        });
    }

    public void addAll(Gallery[] items)
    {
        int start = list.size();
        Collections.addAll(list, items);
        notifyItemRangeInserted(start, items.length);
    }

    public void remove(Gallery item)
    {
        int position = list.indexOf(item);
        if (position != -1)
        {
            list.remove(position);
            notifyItemRemoved(position);
        }
        selected.remove(item);
    }

    void setOnSelectListener(MyGalleryView.OnSelectListener listener)
    {
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private final ImageView imageView;
        private final ImageView select;

        ViewHolder(ViewGroup parent)
        {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false));
            imageView = itemView.findViewById(R.id.image);
            imageView.getLayoutParams().width = size;
            imageView.getLayoutParams().height = size;
            imageView.setOnClickListener(this);
            select = itemView.findViewById(R.id.select);
            select.setOnClickListener(this);
        }

        void updateSelect(boolean selected)
        {
            select.setImageResource(selected ? R.drawable.ic_selected_24dp : R.drawable.ic_select_empty_24dp);
        }

        @Override
        public void onClick(View v)
        {
            Gallery item = list.get(getAdapterPosition());
            updateSelect(item.selected = !item.selected);
            if (item.selected) selected.add(item);
            else selected.remove(item);

            if (listener != null) listener.onSelectChanged(selected.size());
        }
    }
}
