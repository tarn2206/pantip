package tarn.pantip.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import tarn.pantip.R;
import tarn.pantip.app.SelectTagActivity;

/**
 * Created by Tarn on 23 January 2017 21:16
 */

public class SelectTagAdapter extends ArrayAdapter<String>
{
    private final SelectTagActivity activity;

    public SelectTagAdapter(SelectTagActivity context, List<String> items)
    {
        super(context, R.layout.tag_item, R.id.text, items);
        for (int i = 0; i < items.size(); i++)
        {
            String s = items.get(i);
            if (s.endsWith("์")) items.set(i, s + " ");
        }
        activity = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        View view = super.getView(position, convertView, parent);
        CheckBox checkBox = view.findViewById(R.id.text);
        checkBox.setButtonDrawable(TarnCompatCheckBox.createButton(getContext()));
        String text = checkBox.getText().toString();
        boolean found = false;
        for (String tag : activity.selectedTags)
        {
            if (tag.equals(text))
            {
                found = true;
                break;
            }
        }
        checkBox.setChecked(found);
        return view;
    }
}