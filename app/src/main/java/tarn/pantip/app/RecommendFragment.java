package tarn.pantip.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import tarn.pantip.L;
import tarn.pantip.content.Recommend;
import tarn.pantip.util.Optional;
import tarn.pantip.widget.SwipeRefreshRecyclerView;
import tarn.pantip.widget.TopicAdapter;

/**
 * Created by Tarn on 26 August 2017
 */
public class RecommendFragment extends Fragment implements SwipeRefreshRecyclerView.Listener
{
    private SwipeRefreshRecyclerView recycler;
    private String forum;
    private String tag;
    private int index;
    private TopicAdapter adapter;

    public static RecommendFragment newInstance(String forum, String tag, int index)
    {
        RecommendFragment fragment = new RecommendFragment();
        Bundle args = new Bundle();
        args.putString("forum", forum);
        args.putString("tag", tag);
        args.putInt("index", index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        forum = args.getString("forum");
        tag = args.getString("tag");
        index = args.getInt("index");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        recycler = new SwipeRefreshRecyclerView(getContext());
        recycler.setListener(this);
        Recommend.loadCache(forum, tag, index)
                .subscribe(this::checkCacheData, this::error);
        return recycler;
    }

    private void checkCacheData(Optional<Recommend> data)
    {
        if (data.isPresent())
        {
            Recommend recommend = data.get();
            if (!recommend.isEmpty())
            {
                setItems(recommend);
            }
            if (recommend.expired)
            {
                onRefresh();
            }
        }
        else onRefresh();
    }

    public void setItems(Recommend data)
    {
        if (data.expired) recycler.setRefreshing(true);
        if (adapter == null) recycler.setAdapter(adapter = new TopicAdapter((RecommendActivity)getActivity()));
        adapter.setItems(data.items);
        recycler.loaded(false);
    }

    public void error(Throwable tr)
    {
        L.e(tr);
        recycler.showText(tr.getMessage());
    }

    @Override
    public void onRefresh()
    {
        Recommend.loadFromNetwork(forum, tag, index)
                .subscribe(this::setItems, this::error);
    }

    @Override
    public void onLoadMore()
    {}

    public boolean smoothScrollToTop()
    {
        return recycler.smoothScrollToTop();
    }
}
