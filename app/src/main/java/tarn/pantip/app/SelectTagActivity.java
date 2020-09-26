package tarn.pantip.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.SelectTagAdapter;

/**
 * User: Tarn
 * Date: 8/18/13 3:59 PM
 */
public class SelectTagActivity extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener
{
    private final SparseArray<Room> roomIdMap = new SparseArray<>();
    private final Map<String, Room> roomNameMap = new HashMap<>();
    private ListView list1;
    private ListView list2;
    private ViewPager viewPager;
    private TagPagerAdapter pageAdapter;
    private int roomId;
    private Room currentRoom;
    public ArrayList<String> selectedTags;
    private FlexboxLayout flowLayout;
    private SelectTagAdapter adapter;
    private View done;
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        flatToolbar = true;
        setContentView(R.layout.activity_tags_selector);
        title = getTitle().toString();

        flowLayout = findViewById(R.id.selected_tags);
        list1 = getListView(R.id.list1);
        list2 = getListView(R.id.list2);
        viewPager = findViewById(R.id.pager);
        pageAdapter = new TagPagerAdapter();
        pageAdapter.items.add(list1);
        viewPager.setAdapter(pageAdapter);
        viewPager.setPageMargin(toPixel(10));
        viewPager.setPageMarginDrawable(Pantip.pageMarginDrawable);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
        {
            @Override
            public void onPageSelected(int position)
            {
                toolbar.setNavigationIcon(position == 0 ? R.drawable.ic_close_white_24dp : R.drawable.abc_ic_ab_back_material);
                setTitle(position == 0 ? title : currentRoom.name);
            }
        });

        String[] tags;
        if (savedInstanceState == null)
        {
            roomId = getIntent().getIntExtra("room_id", 0);
            tags = getIntent().getStringArrayExtra("tags");
        }
        else
        {
            roomId = savedInstanceState.getInt("roomId");
            tags = savedInstanceState.getStringArray("selectedTags");
        }
        if (tags == null) tags = new String[0];

        selectedTags = new ArrayList<>();
        for (String tag : tags)
        {
            selectedTags.add(tag);
            addTagView(tag);
        }
        Utils.setVisible(findViewById(R.id.tags_group), selectedTags.size() > 0);
        findViewById(R.id.remove_all).setOnClickListener(this);
        done = findViewById(R.id.done);
        done.setOnClickListener(this);
        //done.setEnabled(selectedTags.size() > 0);

        List<String> roomList = new ArrayList<>();
        try
        {
            String json = IOUtils.toString(getAssets().open("tags.json"), StandardCharsets.UTF_8);
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            JsonArray rooms = jsonObject.get("rooms").getAsJsonArray();
            for (JsonElement e : rooms)
            {
                JsonObject room = e.getAsJsonObject();
                Room o = new Room(room.get("room_id").getAsInt(), room.get("room_name").getAsString());
                roomIdMap.put(o.id, o);
                roomNameMap.put(o.name, o);
                roomList.add(o.name);
            }
            JsonArray jtag = jsonObject.get("tags").getAsJsonArray();
            for (JsonElement e : jtag)
            {
                JsonObject tag = e.getAsJsonObject();
                int roomId = tag.get("room_id").getAsInt();
                String name = tag.get("tag_name").getAsString();
                List<String> list = roomIdMap.get(roomId).tags;
                if (!list.contains(name)) list.add(name);
            }

            Collator collator = Collator.getInstance(new Locale("th", "TH"));
            collator.setStrength(Collator.PRIMARY);
            Collections.sort(roomList, collator);
            list1.setAdapter(new ArrayAdapter<>(this, R.layout.simple_list_item, roomList));
            list1.setOnItemClickListener(this);
            list2.setOnItemClickListener(this);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt("roomId", roomId);
        outState.putStringArray("selectedTags", selectedTags.toArray(new String[0]));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        if (parent == list1)
        {
            TextView textView = (TextView)view;
            String text = textView.getText().toString();
            currentRoom = roomNameMap.get(text);
            Collections.sort(currentRoom.tags);
            list2.setAdapter(adapter = new SelectTagAdapter(this, currentRoom.tags));
            if (pageAdapter.getCount() == 1)
            {
                pageAdapter.items.add(list2);
                pageAdapter.notifyDataSetChanged();
            }
            viewPager.setCurrentItem(1, true);
        }
        else if (parent == list2)
        {
            TextView textView = view.findViewById(R.id.text);
            String text = textView.getText().toString();
            if (selectedTags.remove(text))
            {
                done.setEnabled(true);
                adapter.notifyDataSetChanged();
                int n = flowLayout.getChildCount();
                for (int i = n - 1; i >= 0; i--)
                {
                    TextView child = (TextView)flowLayout.getChildAt(i);
                    if (text.equals(child.getText().toString())) flowLayout.removeViewAt(i);
                }
                if (selectedTags.size() == 0)
                {
                    roomId = 0;
                    Utils.setVisible(findViewById(R.id.tags_group), false);
                    //done.setEnabled(false);
                }
            }
            else if (selectedTags.size() < 5)
            {
                done.setEnabled(true);
                if (roomId == 0) roomId = currentRoom.id;
                selectedTags.add(text);
                adapter.notifyDataSetChanged();
                addTagView(text);
                Utils.setVisible(findViewById(R.id.tags_group), true);
                //done.setEnabled(true);
            }
            else Utils.showToast(SelectTagActivity.this, "คุณสามารถเลือกแท็กได้ไม่เกิน 5 แท็ก");
        }
    }

    private void addTagView(String text)
    {
        TextView textView = (TextView)inflate(R.layout.static_tag, flowLayout);
        textView.setText(text);
        flowLayout.addView(textView);
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.remove_all) removeAll();
        else if (v.getId() == R.id.done) done();
    }

    private void removeAll()
    {
        roomId = 0;
        selectedTags.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        Utils.setVisible(findViewById(R.id.tags_group), false);
        //done.setEnabled(false);
        flowLayout.removeAllViews();
        done.setEnabled(true);
    }

    private void done()
    {
        Intent intent = new Intent();
        intent.putExtra("room_id", roomId);
        intent.putExtra("tags", selectedTags.toArray(new String[0]));
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            if (viewPager.getCurrentItem() == 1) viewPager.setCurrentItem(0, true);
            else finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        if (viewPager.getCurrentItem() == 1)
            viewPager.setCurrentItem(0, true);
        else super.onBackPressed();
    }

    private ListView getListView(int id)
    {
        ListView listView = findViewById(id);
        listView.getLayoutParams().width = Pantip.displayWidth;
        ((ViewGroup)listView.getParent()).removeView(listView);
        return listView;
    }

    static class Tag
    {
        public final int id;
        public final String name;

        Tag(int id, String name)
        {
            this.id = id;
            this.name = name;
        }
    }

    static class Room extends Tag
    {
        public final List<String> tags;

        Room(int id, String name)
        {
            super(id, name);
            tags = new ArrayList<>();
        }
    }

    static class TagPagerAdapter extends PagerAdapter
    {
        final List<ListView> items;

        TagPagerAdapter()
        {
            this.items = new ArrayList<>();
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            View page = items.get(position);
            container.addView(page);
            return page;
        }

        @Override
        public int getCount()
        {
            return items.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object)
        {
            return view.equals(object);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object)
        {
            container.removeView((View)object);
        }
    }
}