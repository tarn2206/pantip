package tarn.pantip.widget;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class RecyclerUtil
{
    static <T> void append(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, List<T> list, T[] items, boolean hasMore)
    {
        List<T> newList = new ArrayList<>();
        Collections.addAll(newList, items);

        int start = list.size();
        int count = newList.size();
        if (start > 0 && list.get(start - 1) == null) start--;

        list.addAll(start, newList);

        int last = list.size() - 1;
        if (hasMore && list.get(last) != null)
        {
            list.add(null);
            count++;
        }
        adapter.notifyItemRangeInserted(start, count);
        if (!hasMore && list.get(last) == null)
        {
            list.remove(last);
            adapter.notifyItemRemoved(last);
        }
    }

    static <T> void update(RecyclerView.Adapter adapter, List<T> list, T[] items, boolean hasMore, Callback<T> callback)
    {
        List<T> newList = new ArrayList<>();
        Collections.addAll(newList, items);
        update(adapter, list, newList, hasMore, callback);
    }

    static <T> DiffUtil.DiffResult update(RecyclerView.Adapter adapter, List<T> list, List<T> newList, boolean hasMore, final Callback<T> callback)
    {
        if (hasMore) newList.add(null);

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback<>(list, newList, callback));
        list.clear();
        list.addAll(newList);
        diff.dispatchUpdatesTo(adapter);
        return diff;
    }

    private static class DiffCallback<T> extends DiffUtil.Callback
    {
        private final List<T> oldList;
        private final List<T> newList;
        private final Callback<T> callback;

        DiffCallback(List<T> oldList, List<T> newList, Callback<T> callback)
        {
            this.oldList = oldList;
            this.newList = newList;
            this.callback = callback;
        }

        @Override
        public int getOldListSize()
        {
            return oldList.size();
        }

        @Override
        public int getNewListSize()
        {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition)
        {
            T oldItem = oldList.get(oldItemPosition);
            T newItem = newList.get(newItemPosition);
            return (oldItem == null && newItem == null) || (oldItem != null && newItem != null && callback.areItemsTheSame(oldItem, newItem));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition)
        {
            T oldItem = oldList.get(oldItemPosition);
            T newItem = newList.get(newItemPosition);
            return callback.areContentsTheSame(oldItem, newItem);
        }
    }

    public static abstract class Callback<T>
    {
        protected abstract boolean areItemsTheSame(T oldItem, T newItem);

        boolean areContentsTheSame(T oldItem, T newItem)
        {
            return true;
        }
    }
}
