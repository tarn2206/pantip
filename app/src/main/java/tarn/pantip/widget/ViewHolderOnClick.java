package tarn.pantip.widget;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class ViewHolderOnClick extends RecyclerView.ViewHolder implements View.OnClickListener
{
    private long lastClick;

    ViewHolderOnClick(@NonNull View itemView)
    {
        super(itemView);
    }

    @Override
    final public void onClick(View view)
    {
        if (System.currentTimeMillis() - lastClick < 1000) return;

        lastClick = System.currentTimeMillis();
        onClick(getAdapterPosition());
    }

    public abstract void onClick(int position);
}
