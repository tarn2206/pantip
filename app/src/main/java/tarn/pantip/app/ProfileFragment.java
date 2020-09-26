package tarn.pantip.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.content.MyTopic;
import tarn.pantip.util.Optional;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.MyTopicAdapter;
import tarn.pantip.widget.SwipeRefreshRecyclerView;

public class ProfileFragment extends Fragment implements SwipeRefreshRecyclerView.Listener
{
    public static final int POST_TOPIC = 0;
    public static final int REPLY_TOPIC = 1;
    public static final int FAVORITES_TOPIC = 2;
    private SwipeRefreshRecyclerView recycler;
    private MyTopic x;
    private int index;
    private MyTopicAdapter adapter;
    private int mid;
    private String filename;

    public static ProfileFragment newInstance(int mid, int index)
    {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt("mid", mid);
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

        mid = args.getInt("mid");
        index = args.getInt("index");
        if (index == POST_TOPIC) filename = "topics";
        else if (index == REPLY_TOPIC) filename = "comments";
        else if (index == FAVORITES_TOPIC) filename = "favorites";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        recycler = new SwipeRefreshRecyclerView(getContext());
        recycler.setListener(this);
        recycler.setAdapter(adapter = new MyTopicAdapter((BaseActivity)getActivity(), index == FAVORITES_TOPIC));
        recycler.showProgress();

        MyTopic.load(mid, filename).subscribe(this::localComplete, this::error);
        return recycler;
    }

    public void localComplete(Optional<MyTopic> result)
    {
        if (result.isPresent())
        {
            x = result.get();
            if (x.items != null && x.items.length > 0)
            {
                if (x.page == 0) x.page = 1;
                boolean hasMore = x.page < x.max_page;
                adapter.append(x.items, hasMore);
                recycler.loaded(hasMore);
                if (!Utils.needUpdate(x.time)) return;
            }
        }

        x = new MyTopic();
        loadData(x.page = 1);
    }

    @Override
    public void onRefresh()
    {
        loadData(1);
    }

    @Override
    public void onLoadMore()
    {
        loadData(x.page + 1);
    }

    private void loadData(int page)
    {
        Observable<MyTopic> observable = null;
        if (index == FAVORITES_TOPIC) observable = MyTopic.bookmarks(mid, page, x.first_id, x.last_id);
        if (index == POST_TOPIC) observable = MyTopic.topic(mid, page, x.first_id, x.last_id);
        if (index == REPLY_TOPIC) observable = MyTopic.comment(mid, page, x.first_id, x.last_id);
        observable.subscribe(this::remoteComplete, this::error);
    }

    public void remoteComplete(MyTopic result)
    {
        boolean hasMore = x.page < result.max_page;
        if (result.page == 1)
        {
            x.page = 1;
            adapter.update(result.items, hasMore);
            recycler.scrollToTop();
        }
        else adapter.append(result.items, hasMore);
        recycler.loaded(hasMore);

        if (result.page < x.page) result.page = x.page;
        if (result.max_page < x.max_page) result.max_page = x.max_page;
        x = result;
        x.items = adapter.getItems();

        MyTopic.save(mid, filename, x);
    }

    public void error(Throwable tr)
    {
        L.e(tr);
        recycler.showText(tr.getMessage());
    }

    public boolean smoothScrollToTop()
    {
        return recycler.smoothScrollToTop();
    }
}
