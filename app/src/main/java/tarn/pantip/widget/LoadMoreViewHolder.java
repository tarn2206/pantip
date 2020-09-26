package tarn.pantip.widget;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import tarn.pantip.R;

class LoadMoreViewHolder extends RecyclerView.ViewHolder
{
    LoadMoreViewHolder(LayoutInflater inflater, ViewGroup parent)
    {
        super(inflater.inflate(R.layout.load_more, parent, false));
    }
}
