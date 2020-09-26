package tarn.pantip.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.HomeData;
import tarn.pantip.content.HomeObject;
import tarn.pantip.content.PantipNow;
import tarn.pantip.content.PantipPick;
import tarn.pantip.content.PantipRealtime;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.PantipHomeAdapter;
import tarn.pantip.widget.SwipeRefreshRecyclerView;

public class HomeFragment extends Fragment
{
    private ViewPager viewPager;
    private final SwipeRefreshRecyclerView[] views = new SwipeRefreshRecyclerView[3];
    private RealtimeLoader realtimeLoader;
    private PickLoader pickLoader;
    private NowLoader nowLoader;
    TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        viewPager = (ViewPager)inflater.inflate(R.layout.fragment_home, container, false);
        viewPager.setAdapter(new HomePagerAdapter());
        viewPager.setPageMargin(Utils.toPixels(10));
        viewPager.setPageMarginDrawable(Pantip.pageMarginDrawable);
        if (tabLayout != null) tabLayout.setupWithViewPager(viewPager);
        return viewPager;
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        ((MainActivity)context).showHomeTab(true);
    }

    public boolean smoothScrollToTop()
    {
        SwipeRefreshRecyclerView recycler = views[viewPager.getCurrentItem()];
        return recycler != null && recycler.smoothScrollToTop(12);
    }

    class HomePagerAdapter extends PagerAdapter
    {
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position)
        {
            SwipeRefreshRecyclerView recycler = views[position];
            if (recycler == null)
            {
                recycler = new SwipeRefreshRecyclerView(getContext());
                try
                {
                    switch (position)
                    {
                        case 0:
                            realtimeLoader = new RealtimeLoader(recycler);
                            break;
                        case 1:
                            pickLoader = new PickLoader(recycler);
                            break;
                        case 2:
                            nowLoader = new NowLoader(recycler);
                            break;
                    }
                }
                catch (Exception e)
                {
                    L.e(e);
                    Utils.showToast(e.getMessage());
                }
                views[position] = recycler;
            }
            container.addView(recycler);
            return recycler;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object)
        {
            container.removeView((View)object);
            views[position] = null;
        }

        @Override
        public int getCount()
        {
            return views.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object)
        {
            return view.equals(object);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0: return "Realtime";
                case 1: return "Pantip Pick";
                case 2: return "Hitz";
            }
            return null;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (realtimeLoader != null) realtimeLoader.adapter.onConfigurationChanged(newConfig);
        if (pickLoader != null) pickLoader.adapter.onConfigurationChanged(newConfig);
        if (nowLoader != null) nowLoader.adapter.onConfigurationChanged(newConfig);
    }

    private abstract class HomeLoader<T> implements SwipeRefreshRecyclerView.Listener
    {
        private final SwipeRefreshRecyclerView recycler;
        final PantipHomeAdapter adapter;
        HomeObject last;
        private final String fileName;
        private boolean updates;
        private boolean refreshing;

        HomeLoader(SwipeRefreshRecyclerView recycler, String fileName)
        {
            this.recycler = recycler;
            recycler.setListener(this);
            recycler.setAdapter(adapter = new PantipHomeAdapter(getContext()));

            AtomicBoolean refreshing = new AtomicBoolean(false);
            this.fileName = fileName;
            HomeObject.load(fileName).subscribe(obj -> {
                if (obj.items == null)
                    updates = true;
                else
                {
                    Collections.addAll(adapter.list, obj.items);
                    refreshing.set(true);
                    updates = obj.expired;
                }
                last = obj;
            }, tr -> {
                refreshing.set(true);
                updates = true;
            },() -> {
                if (adapter.list.size() > 0) recycler.loaded(true);
                if (updates)
                {
                    if (refreshing.get()) recycler.setRefreshing(true);
                    onRefresh();
                }
            });
        }

        abstract HomeObject getHomeObject(T result);

        void complete(T result)
        {
            last = getHomeObject(result);

            boolean save = updates || recycler.isRefreshing();
            int[] pos;
            HomeData data;
            if (refreshing || recycler.isRefreshing())
            {
                refreshing = false;
                pos = recycler.getFirstVisibleItemPosition();
                data = pos[0] >= 0 && pos[0] < adapter.getItemCount() ? adapter.getItem(pos[0]) : null;
                adapter.update(last.items, last.has_next);
            }
            else
            {
                pos = recycler.getFirstVisibleItemPosition();
                data = pos[0] >= 0 && pos[0] < adapter.getItemCount() ? adapter.getItem(pos[0]) : null;
                adapter.append(last.items, last.has_next);
            }
            if (data != null)
            {
                pos[0] = adapter.getPosition(data);
                recycler.scrollToPositionWithOffset(pos[0], pos[1]);
            }
            recycler.setRefreshing(false);
            recycler.loaded(last.has_next);

            if (save)
                HomeObject.save(last, adapter.list, fileName);
        }

        void error(Throwable tr)
        {
            L.e(tr);
            recycler.showText(tr.getMessage());
        }

        void update()
        {
            recycler.setAdapter(adapter);
        }
    }

    private class RealtimeLoader extends HomeLoader<PantipRealtime>
    {
        RealtimeLoader(SwipeRefreshRecyclerView recycler)
        {
            super(recycler, "realtime.json");
        }

        @Override
        HomeObject getHomeObject(PantipRealtime result)
        {
            HomeObject obj = new HomeObject();
            obj.next_id = result.next_id;
            obj.ranking_time = result.ranking_time;
            obj.items = result.data;
            obj.has_next = result.data != null && result.data.length == 20;
            return obj;
        }

        @Override
        public void onRefresh()
        {
            PantipRealtime.load().subscribe(this::complete, this::error);
        }

        @Override
        public void onLoadMore()
        {
            PantipRealtime.load(last).subscribe(this::complete, this::error);
        }
    }

    private class PickLoader extends HomeLoader<PantipPick>
    {
        PickLoader(SwipeRefreshRecyclerView recycler)
        {
            super(recycler, "pick.json");
        }

        @Override
        HomeObject getHomeObject(PantipPick result)
        {
            HomeObject obj = new HomeObject();
            obj.next_id = result.next_id;
            obj.has_next = result.has_next;
            obj.items = result.data;
            return obj;
        }

        @Override
        public void onRefresh()
        {
            PantipPick.load().subscribe(this::complete, this::error);
        }

        @Override
        public void onLoadMore()
        {
            PantipPick.load(last).subscribe(this::complete, this::error);
        }
    }

    private class NowLoader extends HomeLoader<PantipNow>
    {
        NowLoader(SwipeRefreshRecyclerView recycler)
        {
            super(recycler, "now.json");
        }

        @Override
        HomeObject getHomeObject(PantipNow result)
        {
            HomeObject obj = new HomeObject();
            obj.next_id = result.next_id;
            obj.has_next = result.has_next;
            obj.items = result.data;
            return obj;
        }

        @Override
        public void onRefresh()
        {
            PantipNow.load().subscribe(this::complete, this::error);
        }

        @Override
        public void onLoadMore()
        {
            PantipNow.load(last).subscribe(this::complete, this::error);
        }
    }

    public void update()
    {
        if (realtimeLoader != null) realtimeLoader.update();
        if (pickLoader != null) pickLoader.update();
        if (nowLoader != null) nowLoader.update();
    }
}
