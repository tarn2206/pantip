package tarn.pantip.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.content.MyTopic;
import tarn.pantip.util.Optional;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.MyTopicAdapter;
import tarn.pantip.widget.SwipeRefreshRecyclerView;

/**
 * User: Tarn
 * Date: 9/14/13 8:13 PM
 */
public class MyTopicFragment extends Fragment implements SwipeRefreshRecyclerView.Listener
{
    public static final int RC_FAVORITES = 55;
    public static final int READ_TOPIC = 0;
    public static final int FAVORITES_TOPIC = 1;
    public static final int POST_TOPIC = 2;
    public static final int REPLY_TOPIC = 3;
    private SwipeRefreshRecyclerView recycler;
    private MyTopic x;
    private int index;
    private MyTopicAdapter adapter;
    private String type;

    public static MyTopicFragment newInstance(int index)
    {
        MyTopicFragment fragment = new MyTopicFragment();
        Bundle args = new Bundle();
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

        index = args.getInt("index");
        if (index == POST_TOPIC) type = "topics";
        else if (index == REPLY_TOPIC) type = "comments";
        else if (index == FAVORITES_TOPIC) type = "favorites";
        else if (index == READ_TOPIC) type = "history";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        recycler = new SwipeRefreshRecyclerView(getContext());
        recycler.setListener(this);
        recycler.setAdapter(adapter = new MyTopicAdapter((BaseActivity)getActivity(), index == FAVORITES_TOPIC));
        recycler.showProgress();

        MyTopic.load(Pantip.currentUser.id, type)
                .subscribe(this::localComplete, this::error);
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
        if (index == READ_TOPIC) observable = MyTopic.history(Pantip.currentUser.id, page, x.first_id, x.last_id);
        if (index == FAVORITES_TOPIC) observable = MyTopic.bookmarks(Pantip.currentUser.id, page, x.first_id, x.last_id);
        if (index == POST_TOPIC) observable = MyTopic.topic(Pantip.currentUser.id, page, x.first_id, x.last_id);
        if (index == REPLY_TOPIC) observable = MyTopic.comment(Pantip.currentUser.id, page, x.first_id, x.last_id);
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

        MyTopic.save(Pantip.currentUser.id, type, x);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_FAVORITES)
        {
            long topicId = data.getLongExtra("id", 0);
            if (topicId == 0) return;
            boolean favorite = data.getBooleanExtra("favorite", true);
            if (!favorite && adapter.remove(topicId))
            {
                x.items = adapter.getItems();
                MyTopic.save(Pantip.currentUser.id, type, x);
            }
        }
    }
}