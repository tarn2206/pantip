package tarn.pantip.widget;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.MainActivity;
import tarn.pantip.model.ForumMenuItem;
import tarn.pantip.model.MenuItemType;
import tarn.pantip.store.DataStore;
import tarn.pantip.util.Utils;

public class ForumAdapter extends BaseAdapter
{
    private final MainActivity activity;
    private final LayoutInflater layoutInflater;
    private final List<ForumMenuItem> data = new ArrayList<>();
    private final DataStore store;

    public ForumAdapter(MainActivity activity)
    {
        this.activity = activity;
        store = Pantip.getDataStore();
        layoutInflater = LayoutInflater.from(activity);
        loadData();
    }

    public void loadData()
    {
        data.clear();
        store.listForums().subscribe(list -> {
            boolean hasFavoritesHeader = false;
            boolean hasForumHeader = false;
            Resources resources = activity.getResources();
            for (ForumMenuItem item : list)
            {
                if (item.hitCount > 0)
                {
                    if (!hasFavoritesHeader)
                    {
                        data.add(new ForumMenuItem(MenuItemType.Header, "เลือกห้อง"));
                        hasFavoritesHeader = true;
                    }
                }
                else if (!hasForumHeader)
                {
                    data.add(new ForumMenuItem(MenuItemType.Header, "ห้องทั้งหมด"));
                    hasForumHeader = true;
                }
                item.iconId = resources.getIdentifier("ic_forum_" + (item.iconId + 1), "drawable", activity.getPackageName());
                data.add(item);
            }
            notifyDataSetChanged();
        });
    }

    @Override
    public int getCount()
    {
        return data.size();
    }

    @Override
    public boolean isEnabled(int position)
    {
        MenuItemType type = data.get(position).type;
        return type != MenuItemType.Header;
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public Object getItem(int position)
    {
        return data.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ForumMenuItem item = data.get(position);
        try
        {
            if (item.type == MenuItemType.Item) convertView = getItemView(convertView, item, parent);
            else convertView = getSectionView(convertView, item.label, parent);
        }
        catch (Exception e)
        {
            Pantip.handleException(activity, e);
        }
        return convertView;
    }

    private View getSectionView(View view, String text, ViewGroup parent)
    {
        if (view == null || view.findViewById(R.id.menu_section) == null)
            view = layoutInflater.inflate(R.layout.drawer_section, parent, false);
        TextView textView = view.findViewById(R.id.menu_section);
        textView.setText(text);
        textView.setTextSize(Pantip.textSize);
        return view;
    }

    private View getItemView(View view, ForumMenuItem item, ViewGroup parent)
    {
        if (view == null || view.findViewById(R.id.title) == null)
        {
            view = layoutInflater.inflate(R.layout.drawer_forum_item, parent, false);
        }
        ImageView iconView = view.findViewById(android.R.id.icon);
        iconView.setImageResource(item.iconId);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        TextView textView = view.findViewById(R.id.title);
        textView.setText(item.label.endsWith("์") ? item.label + " " : item.label);
        textView.setTextSize(Pantip.textSize);
        textView = view.findViewById(R.id.description);
        textView.setText(item.description);
        textView.setTextSize(Pantip.textSize - 5);
        Utils.setVisible(textView, item.description != null);
        return view;
    }
}