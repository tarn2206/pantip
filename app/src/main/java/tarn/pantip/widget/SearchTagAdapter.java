package tarn.pantip.widget;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.model.Tag;
import tarn.pantip.util.Utils;

public class SearchTagAdapter extends BaseAdapter
{
    private static final DecimalFormat nFormat = new DecimalFormat("#,##0");
    private final LayoutInflater inflater;
    private final OnClickListener listener;
    public final List<Tag> list = new ArrayList<>();
    private final int small;
    private final int tertiary;

    public SearchTagAdapter(Context context, OnClickListener listener)
    {
        inflater = LayoutInflater.from(context);
        this.listener = listener;
        small = (int)Utils.textSize(Utils.fixTextSize(Pantip.textSize - 2));
        tertiary = ContextCompat.getColor(context, R.color.tertiary_text_pantip);
    }

    @Override
    public int getCount()
    {
        return list.size();
    }

    @Override
    public Tag getItem(int position)
    {
        return list.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final Tag item = list.get(position);
        String s = item.label + " - " + nFormat.format(item.count) + " กระทู้";
        SpannableString span = new SpannableString(s);
        span.setSpan(new AbsoluteSizeSpan(small), item.label.length(), s.length(), 0);
        span.setSpan(new ForegroundColorSpan(tertiary), item.label.length(), s.length(), 0);

        if (convertView == null) convertView = inflater.inflate(R.layout.search_tag_item, parent, false);
        TextView textView = (TextView)convertView;
        textView.setTextSize(Pantip.textSize);
        textView.setText(span);

        if (listener != null)
        {
            convertView.setOnClickListener(v -> listener.onClick(item.url, item.label));
        }
        return convertView;
    }

    interface OnClickListener
    {
        void onClick(String url, String label);
    }
}
