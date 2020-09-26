package tarn.pantip.util;

import androidx.recyclerview.widget.ListUpdateCallback;

import tarn.pantip.L;

class DiffUpdates implements ListUpdateCallback
{
    public DiffUpdates()
    {
        L.d("----- begin update -----");
    }

    @Override
    public void onInserted(int position, int count)
    {
        L.d("insert[%d] %d", position, count);
    }

    @Override
    public void onRemoved(int position, int count)
    {
        L.d("remove[%d] %d", position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition)
    {
        L.d("move[%d->%d]", fromPosition, toPosition);
    }

    @Override
    public void onChanged(int position, int count, Object payload)
    {
        L.d("changed[%d] %d", position, count);
    }
}
