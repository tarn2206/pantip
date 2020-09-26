package tarn.pantip.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.StringUtils;

import io.reactivex.rxjava3.internal.functions.Functions;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.content.LocalObject;
import tarn.pantip.content.PantipClient;
import tarn.pantip.model.Topic;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.SwipeRefreshRecyclerView;
import tarn.pantip.widget.TopicAdapter;
import tarn.pantip.widget.TopicFilter;

/**
 * User: Tarn
 * Date: 5/6/13 5:00 PM
 */
public class MainFragment extends Fragment implements SwipeRefreshRecyclerView.Listener, TopicFilter.OnItemClickListener
{
    private MainActivity activity;
    private SwipeRefreshRecyclerView recycler;
    private TopicAdapter adapter;
    private TopicFilter filter;
    private boolean isForum;
    private String forum;
    private String tag;
    private int page = 1;
    private MainListener listener;
    private TopicAdapter.OnItemClickListener onItemClickListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        if (recycler != null) return recycler;
        recycler = new SwipeRefreshRecyclerView(getContext());
        recycler.setListener(this);
        return recycler;
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        activity = (MainActivity)context;
        activity.showHomeTab(false);
    }

    void loadData(boolean isForum, String forum, String tag)
    {
        if (recycler == null) onCreateView(activity.getLayoutInflater(), null, null);
        recycler.showProgress();
        if (adapter != null) adapter.clear();
        this.isForum = isForum;
        this.forum = forum;
        this.tag = tag;

        if (filter == null)
        {
            filter = new TopicFilter(activity, forum, tag);
            filter.setOnItemClickListener(this);
        }
        else filter.setForum(forum, tag);

        page = 1;
        //recycler.showProgress();
        adapter = null;

        Topic.load(isForum ? forum : tag, filter.getType(), isForum)
                .subscribe(result -> {
                    if (result.items != null && result.items.length > 0)
                        complete(result);
                    else
                        loadTopic(1);
                }, tr -> Pantip.handleException(getContext(), tr));
    }

    @Override
    public void onItemClick(TopicType type)
    {
        loadData(isForum, forum, tag);
    }

    public void resetTopicType()
    {
        if (filter != null) filter.reset();
    }

    @Override
    public void onRefresh()
    {
        loadTopic(page = 1);
    }

    @Override
    public void onLoadMore()
    {
        // page does matter ??
        loadTopic(page + 1);
    }

    private void loadTopic(int page)
    {
        long lastId = 0;
        if (adapter != null)
        {
            Topic last = null;
            int n = adapter.getItemCount() - 1;
            while (last == null && n >= 0)
            {
                last = adapter.getItem(n--);
            }
            if (last != null) lastId = last.id;
        }
        PantipClient.loadTopics(isForum, isForum ? forum : tag, filter.getType().getValue(), page, lastId)
                .subscribe(list -> {
                    LocalObject<Topic> result = new LocalObject<>();
                    result.items = list.toArray(new Topic[0]);
                    complete(result);
                    if (page == 1)
                    {
                        activity.reloadTag();
                    }
                }, this::error);
    }

    private void complete(LocalObject<Topic> result)
    {
        if (StringUtils.isNotBlank(result.label)) activity.setTitle(result.label);
        if (result.items.length == 0) return;

        int n = result.items.length;
        while (n > 0 && result.items[n - 1] == null) n--;
        boolean hasMore = result.lastModified > 0 || n % 50 == 0;
        int[] pos = null;
        Topic data = null;
        if (page == 1)
        {
            if (adapter == null)
            {
                recycler.setAdapter(adapter = new TopicAdapter(activity));
                adapter.append(filter.getView(), result.items, hasMore);
                adapter.setOnItemClickListener(onItemClickListener);
            }
            else
            {
                pos = getFirstVisibleItemPosition();
                data = pos[0] >= 0 && pos[0] < adapter.getItemCount() ? adapter.getItem(pos[0]) : null;
                adapter.update(result.items, hasMore);
            }
        }
        else
        {
            pos = getFirstVisibleItemPosition();
            data = pos[0] >= 0 && pos[0] < adapter.getItemCount() ? adapter.getItem(pos[0]) : null;
            adapter.append(result.items, hasMore);
        }
        recycler.loaded(hasMore);
        if (data != null)
        {
            pos[0] = adapter.getPosition(data);
            recycler.scrollToPositionWithOffset(pos[0], pos[1]);
        }

        listener.onLoad(page);
        if (hasMore) page++;

        if (result.lastModified == 0)
        {
            Topic.save(isForum ? forum : tag, filter.getType(), isForum, adapter.getItems())
                    .subscribe(Functions.emptyConsumer(), L::e);
        }
        else if (Utils.needUpdate(result.lastModified))
        {
            recycler.postDelayed(() -> {
                recycler.setRefreshing(true);
                onRefresh();
            }, 800);
        }
        else adapter.notifyDataSetChanged();
    }

    private int[] getFirstVisibleItemPosition()
    {
        int[] pos = recycler.getFirstVisibleItemPosition();
        if (pos[0] == 0)
        {
            pos[0] = 1;
            View first = recycler.getViewAt(1);
            if (first != null) pos[1] = first.getTop();
        }
        return pos;
    }

    private void error(Throwable tr)
    {
        recycler.showText(tr.getMessage());
        L.e(tr);
        listener.onError(tr);
    }

    public boolean smoothScrollToTop()
    {
        return recycler.smoothScrollToTop();
    }

    public void setOnItemClickListener(TopicAdapter.OnItemClickListener listener)
    {
        if (adapter == null) this.onItemClickListener = listener;
        else adapter.setOnItemClickListener(listener);
    }

    public void setMainListener(MainListener listener)
    {
        this.listener = listener;
    }

    public interface MainListener
    {
        void onLoad(int page);
        void onError(Throwable tr);
    }

    public void update()
    {
        recycler.setAdapter(adapter);
    }
}