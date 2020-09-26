package tarn.pantip.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Json;
import tarn.pantip.content.Preferences;
import tarn.pantip.content.Tags;
import tarn.pantip.model.MenuItemType;
import tarn.pantip.model.Tag;
import tarn.pantip.model.TagMenuItem;
import tarn.pantip.store.DataStore;
import tarn.pantip.util.Utils;

/**
 * User: tarn
 * Date: 1/27/13 1:17 AM
 */
public class TagAdapter extends BaseAdapter
{
    private final DataStore store;
    private final LayoutInflater inflater;
    private List<TagMenuItem> data = new ArrayList<>();
    private List<TagMenuItem> favorites;

    public TagAdapter(AppCompatActivity context)
    {
        store = Pantip.getDataStore();
        inflater = LayoutInflater.from(context);
    }

    public synchronized void saveInstanceState(Preferences preferences)
    {
        try
        {
            preferences.putString("main:tagAdapter", Json.toJson(data));
        }
        catch (Exception e)
        {/*ignored*/}
    }

    public void loadInstanceState(Preferences preferences, String forum, String tag)
    {
        String json = preferences.getString("main:tagAdapter");
        data = Json.toList(json, TagMenuItem[].class);
        if (data == null) reload(forum, tag);
    }

    public synchronized void updateFavorites()
    {
        while (data.size() > 0)
        {
            TagMenuItem item = data.get(0);
            if (item.type == MenuItemType.Header)
            {
                if (!"Favorites".equals(item.label)) break;
            }
            data.remove(0);
        }
        favorites = store.loadFavoriteTags();
        if (favorites.size() > 0)
        {
            int i = 0;
            data.add(i++, new TagMenuItem("Favorites"));
            for (TagMenuItem item : favorites)
                data.add(i++, item);
        }
        notifyDataSetChanged();
    }

    public synchronized void reload(final String forum, final String tag)
    {
        data.clear();
        favorites = store.loadFavoriteTags();
        if (favorites.size() > 0)
        {
            data.add(new TagMenuItem("Favorites"));
            data.addAll(favorites);
        }

        Tags.loadCache(forum, tag)
                .subscribe(data -> {
                    if (data.isPresent())
                    {
                        Tags tags = data.get();
                        if (!tags.isEmpty())
                        {
                            complete(forum, tags);
                        }
                        if (tags.expired)
                        {
                            onRefresh(forum, tag);
                        }
                    }
                    else onRefresh(forum, tag);
                }, this::error);
    }

    public void complete(String forum, Tags result)
    {
        List<TagMenuItem> list = filterFavorites(forum, result.tags);
        if (list != null && list.size() > 0)
        {
            for (int i = 0; i < data.size(); i++)
            {
                TagMenuItem item = data.get(i);
                if (item.type == MenuItemType.Header && "แท็ก".equals(item.label))
                {
                    while (i < data.size())
                        data.remove(i);
                    break;
                }
            }
            data.add(new TagMenuItem("แท็ก"));
            data.addAll(list);
            notifyDataSetChanged();
        }
    }

    public void error(Throwable tr)
    {
        notifyDataSetChanged();
        Pantip.handleException(tr);
    }

    public void onRefresh(String forum, String tag)
    {
        Tags.loadFromNetwork(forum, tag)
                .subscribe(data -> complete(forum, data), this::error);
    }

    private List<TagMenuItem> filterFavorites(String forum, List<Tag> tags)
    {
        if (tags == null) return null;

        List<TagMenuItem> list = new ArrayList<>();
        for (Tag tag : tags)
        {
            boolean found = false;
            for (TagMenuItem item : favorites)
            {
                if (tag.url.equals(item.url))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                TagMenuItem item = new TagMenuItem();
                item.forum = forum;
                item.label = tag.label;
                item.url = tag.url;
                list.add(item);
            }
        }
        return list;
    }

    @Override
    public int getCount()
    {
        return data.size();
    }

    @Override
    public boolean isEnabled(int position)
    {
        return data.get(position).type == MenuItemType.Item;
    }

    @Override
    public TagMenuItem getItem(int position)
    {
        return data.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        TagMenuItem item = data.get(position);
        convertView = getView(item.type, convertView, parent);
        TextView textView;
        if (item.type == MenuItemType.Header)
        {
            textView = convertView.findViewById(R.id.menu_section);
        }
        else
        {
            textView = convertView.findViewById(R.id.tag_name);
            Utils.setVisible(convertView.findViewById(R.id.sub_tag), item.isSubTag);
        }
        textView.setText(item.label.endsWith("์") ? item.label + " " : item.label);
        textView.setTextSize(Pantip.textSize);
        return convertView;
    }

    private View getView(MenuItemType type, View view, ViewGroup parent)
    {
        if (type == MenuItemType.Header)
        {
            if (view == null || view.findViewById(R.id.menu_section) == null) view = inflater.inflate(R.layout.drawer_section, parent, false);
        }
        else
        {
            if (view == null || view.findViewById(R.id.tag_name) == null) view = inflater.inflate(R.layout.drawer_tag_item, parent, false);
        }
        return view;
    }
}
