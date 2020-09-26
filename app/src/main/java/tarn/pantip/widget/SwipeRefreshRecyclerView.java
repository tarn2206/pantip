package tarn.pantip.widget;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import tarn.pantip.L;
import tarn.pantip.R;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 28 December 2016
 */
public class SwipeRefreshRecyclerView extends FrameLayout
{
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private ContentLoadingProgressBar progressBar;
    private TextView textView;
    private boolean recyclerShown;
    private boolean addScrollListener;
    private Listener listener;
    public String emptyText = "ไม่มีข้อมูล";
    private boolean loadingMore;

    public SwipeRefreshRecyclerView(Context context)
    {
        super(context);
        initialize(context);
    }

    public SwipeRefreshRecyclerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public SwipeRefreshRecyclerView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context)
    {
        inflate(context, R.layout.swipe_refresh_recycler, this);
        //if (isInEditMode()) return;

        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.swipe_refresh_background);
        swipeRefresh.setColorSchemeResources(R.color.swipe_refresh_color1, R.color.swipe_refresh_color2, R.color.swipe_refresh_color3, R.color.swipe_refresh_color4);
        swipeRefresh.setDistanceToTriggerSync(Utils.getDisplaySize().y / 3);
        swipeRefresh.setOnRefreshListener(refreshListener);
        swipeRefresh.setVisibility(GONE);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context));
        DefaultItemAnimator animator = (DefaultItemAnimator)recyclerView.getItemAnimator();
        if (animator != null) animator.setSupportsChangeAnimations(false);

        textView = findViewById(R.id.text);
        textView.setVisibility(GONE);

        progressBar = findViewById(android.R.id.progress);
        //progressBar.setVisibility(View.GONE);
    }

    public RecyclerView.Adapter getAdapter()
    {
        return recyclerView.getAdapter();
    }

    public void setAdapter(RecyclerView.Adapter adapter)
    {
        recyclerView.setAdapter(adapter);
    }

    public void setListener(Listener listener)
    {
        this.listener = listener;
    }

    private final SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener()
    {
        @Override
        public void onRefresh()
        {
            Utils.playSound(getContext(), R.raw.psst2);
            if (listener != null) listener.onRefresh();
        }
    };

    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener()
    {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState)
        {
            if (loadingMore || listener == null || newState != RecyclerView.SCROLL_STATE_IDLE
                || isRefreshing() || layoutManager.findLastVisibleItemPosition() != layoutManager.getItemCount() - 1) return;

            View last = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
            if (last == null) return;
            int x = (last.getBottom() - last.getTop()) / 2;
            if (last.getBottom() > recyclerView.getBottom() + x) return;
            loadingMore = true;
            postDelayed(() -> {
                try
                {
                    listener.onLoadMore();
                }
                catch (Exception e)
                {
                    L.e(e);
                }
                loadingMore = false;
            }, 100);
        }
    };

    public void showProgress()
    {
        hideText();
        progressBar.show();
    }

    public void hideProgress()
    {
        progressBar.hide();
    }

    public void setRefreshable(boolean refreshable)
    {
        swipeRefresh.setEnabled(refreshable);
    }

    public void setRefreshing(final boolean refreshing)
    {
        swipeRefresh.post(() -> {
            swipeRefresh.setRefreshing(refreshing);
            swipeRefresh.setVisibility(VISIBLE);
        });
    }

    public boolean isRefreshing()
    {
        return swipeRefresh.isRefreshing();
    }

    public void loaded(boolean hasMore)
    {
        loadingMore = false;
        progressBar.hide();
        if (isRefreshing())
        {
            Utils.playSound(getContext(), R.raw.pop2);
            swipeRefresh.setRefreshing(false);
        }

        if (!show()) return;

        if (hasMore && !addScrollListener)
        {
            addScrollListener = true;
            recyclerView.addOnScrollListener(scrollListener);
        }
        else if (!hasMore && addScrollListener)
        {
            addScrollListener = false;
            recyclerView.removeOnScrollListener(scrollListener);
        }
    }

    public boolean show()
    {
        if (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0)
        {
            if (textView.getVisibility() == GONE) showText(emptyText);
            return false;
        }
        if (!recyclerShown)
        {
            recyclerShown = true;
            textView.setVisibility(GONE);
            Utils.fadeIn(swipeRefresh);
        }
        recyclerView.requestFocus();
        return true;
    }

    public void showText(final String s)
    {
        progressBar.hide();
        swipeRefresh.setRefreshing(false);
        if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0)
        {
            postDelayed(() -> {
                swipeRefresh.setRefreshing(false);
                Utils.showToast(getContext(), s);
            }, 500);
        }
        else
        {
            recyclerShown = false;
            swipeRefresh.setVisibility(GONE);
            textView.setText(s);
            Utils.fadeIn(textView);
        }
    }

    public void hideText()
    {
        textView.setVisibility(GONE);
    }

    public boolean smoothScrollToTop()
    {
        return smoothScrollToTop(20);
    }

    public boolean smoothScrollToTop(int n)
    {
        int first = layoutManager.findFirstVisibleItemPosition();
        if (first == 0) return false;

        if (first > n) recyclerView.scrollToPosition(n);
        recyclerView.smoothScrollToPosition(0);
        return true;
    }

    public void scrollToTop()
    {
        recyclerView.scrollToPosition(0);
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.firstPosition = layoutManager.findFirstVisibleItemPosition();
        View first = getViewAt(0);
        state.offset = first == null ? 0 : first.getTop();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (state instanceof SavedState)
        {
            SavedState ss = (SavedState)state;
            super.onRestoreInstanceState(ss.getSuperState());
            recyclerView.scrollToPosition(ss.firstPosition);
            recyclerView.scrollBy(0, -ss.offset);
        }
        else super.onRestoreInstanceState(state);
    }

    public static class SavedState extends AbsSavedState
    {
        int firstPosition;
        int offset;

        SavedState(Parcelable superState)
        {
            super(superState);
        }
    }

    public interface Listener
    {
        void onRefresh();
        void onLoadMore();
    }

    public int[] getFirstVisibleItemPosition()
    {
        int pos = layoutManager.findFirstVisibleItemPosition();
        View first = getViewAt(0);
        int offset = first != null ? first.getTop() : 0;
        return new int[] { pos, offset };
    }

    public View getViewAt(int index)
    {
        return layoutManager.getChildCount() > index ? layoutManager.getChildAt(index) : null;
    }

    public void scrollToPositionWithOffset(int position, int offset)
    {
        layoutManager.scrollToPositionWithOffset(position, offset);
    }
}