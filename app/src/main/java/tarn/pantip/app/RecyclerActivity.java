package tarn.pantip.app;

import android.os.Bundle;
import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;

import tarn.pantip.L;
import tarn.pantip.R;
import tarn.pantip.widget.SwipeRefreshRecyclerView;

/**
 * Created by Tarn on 20 December 2016
 */

public abstract class RecyclerActivity<T> extends BaseActivity implements SwipeRefreshRecyclerView.Listener
{
    SwipeRefreshRecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler);

        recycler = findViewById(R.id.recycler);
        recycler.setListener(this);
        recycler.showProgress();
        if (onLoad(savedInstanceState)) recycler.hideProgress();
    }

    void setAdapter(RecyclerView.Adapter adapter)
    {
        recycler.setAdapter(adapter);
    }

    void complete(T result)
    {
        boolean hasMore = onFinish(recycler.isRefreshing(), result);
        recycler.loaded(hasMore);
    }

    protected abstract boolean onFinish(boolean isRefreshing, T result);

    void error(Throwable tr)
    {
        L.e(tr);
        String message = tr.getMessage();
        if (StringUtils.isBlank(message)) message = "Something went wrong!";
        recycler.showText(message);
    }

    void setRefreshing()
    {
        recycler.setRefreshing(true);
    }

    void showRecycler()
    {
        recycler.show();
    }

    @Override
    protected boolean onDoubleTapToolbar(MotionEvent e)
    {
        recycler.smoothScrollToTop();
        return true;
    }

    boolean onLoad(Bundle savedInstanceState)
    {
        return false;
    }

    @Override
    public void onRefresh()
    {}

    @Override
    public void onLoadMore()
    {}
}