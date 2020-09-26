package tarn.pantip.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import tarn.pantip.R;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 22 January 2017 00:27
 */

public class TopicTypeAdapter extends ArrayAdapter<String>
{
    private final int[] icons = new int[] {
        R.drawable.topic_chat, R.drawable.topic_question, R.drawable.topic_news,
        R.drawable.topic_poll, R.drawable.topic_review, R.drawable.topic_trade
    };
    private int selectedColor;
    private int selected;

    public TopicTypeAdapter(Context context)
    {
        super(context, R.layout.topic_type_spinner_item, android.R.id.text1, context.getResources().getStringArray(R.array.topic_type));
        selectedColor = ContextCompat.getColor(context, R.color.colorAccent);
        selectedColor = Utils.setAlpha(selectedColor, 0x99);
    }

    @Override
    public boolean isEnabled(int position)
    {
        return icons[position] != R.drawable.topic_poll;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        return bindView(position, super.getView(position, convertView, parent));
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        View view = super.getDropDownView(position, convertView, parent);
        if (position == selected) view.setBackgroundColor(selectedColor);
        return bindView(position, view);
    }

    private View bindView(int position, View view)
    {
        ImageView icon = view.findViewById(android.R.id.icon);
        icon.setImageResource(icons[position]);
        view.setAlpha(isEnabled(position) ? 1f : 0.4f);
        return view;
    }

    public void setSelection(int position)
    {
        selected = position;
    }
}