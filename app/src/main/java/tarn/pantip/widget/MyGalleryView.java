package tarn.pantip.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.internal.functions.Functions;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Gallery;
import tarn.pantip.util.Utils;

public class MyGalleryView extends RecyclerView
{
    private GridLayoutManager gridLayout;
    private LinearLayoutManager linearLayout;
    private MyGalleryAdapter adapter;
    private int itemHeight;
    private int loadingPage;
    public boolean loaded;
    private ContentLoadingProgressBar progressBar;
    private boolean autoLayout;

    public MyGalleryView(Context context)
    {
        this(context, null, 0);
    }

    public MyGalleryView(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public MyGalleryView(Context context, @Nullable AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context, attrs, defStyle);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyle)
    {
        try
        {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyGalleryView, defStyle, 0);
            autoLayout = a.getBoolean(R.styleable.MyGalleryView_autoLayout, false);
            a.recycle();
        }
        catch (Exception e)
        {
            L.e(e);
        }
        setHasFixedSize(true);

        addItemDecoration(new ItemDecoration());
        gridLayout = new GridLayoutManager(context, 1);
        linearLayout = new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false);

        Configuration config = context.getResources().getConfiguration();
        calcItemHeight(config);
        setLayoutManager(config.orientation == Configuration.ORIENTATION_PORTRAIT || !autoLayout ? gridLayout : linearLayout);

        adapter = new MyGalleryAdapter(itemHeight);
        setAdapter(adapter);
    }

    public void load(final ContentLoadingProgressBar progressBar)
    {
        loaded = true;
        this.progressBar = progressBar;
        if (progressBar != null)
        {
            progressBar.show();
        }

        Gallery.load().subscribe(result -> {
            if (result != null && result.hasItems())
            {
                if (progressBar != null) progressBar.hide();
                if (adapter.getItemCount() == 0) adapter.addAll(result.items);
                else adapter.update(Arrays.asList(result.items));
            }

            boolean needUpdate = result == null || !result.hasItems() || Utils.needUpdate(result.lastModified);
            if (needUpdate) reload();
        }, tr -> {
            L.e(tr);
            reload();
        });
    }

    interface LocalGalleryListener
    {
        void complete(Gallery[] result);
        void loadNetwork();
    }

    public void reload()
    {
        loadingPage = 1;
        getPicture();
    }

    private void getPicture()
    {
        Gallery.getPicture(loadingPage)
                .subscribe(this::complete, this::error);
    }

    private void complete(JsonObject json)
    {
        if (progressBar != null)
        {
            progressBar.hide();
            progressBar = null;
        }
        JsonArray images = json.get("images").getAsJsonArray();
        List<Gallery> list = new ArrayList<>();
        for (JsonElement e : images)
        {
            JsonObject o = e.getAsJsonObject();
            list.add(new Gallery(o.get("_id").getAsLong(), o.get("url_pic").getAsString()));
        }

        int size = adapter.getItemCount();
        if (loadingPage == 1 && size > 0)
        {
            adapter.update(list);
            scrollToPosition(0);
        }
        else adapter.addAll(list.toArray(new Gallery[0]));

        if (loadingPage < json.get("total_pages").getAsInt())
        {
            loadingPage++;
            getPicture();
        }
        else
        {
            adapter.save().subscribe(Functions.emptyConsumer(), this::error);
        }
    }

    private void error(Throwable tr)
    {
        L.e(tr);
        Utils.showToast(getContext(), tr.getMessage());
        if (progressBar != null)
        {
            progressBar.hide();
            progressBar = null;
        }
    }

    private static class ItemDecoration extends RecyclerView.ItemDecoration
    {
        private final int spacer = Utils.toPixels(1);

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state)
        {
            int position = ((RecyclerView.LayoutParams)view.getLayoutParams()).getViewLayoutPosition();

            int rows, cols = 0, row, col;
            LayoutManager lm = parent.getLayoutManager();
            if (lm instanceof GridLayoutManager)
            {
                GridLayoutManager gridLayout = (GridLayoutManager)lm;
                int span = gridLayout.getSpanCount();
                rows = gridLayout.getItemCount() / span;
                if (gridLayout.getItemCount() % span > 0) rows++;
                cols = span;
                row = position / span;
                col = position % span;
            }
            else
            {
                rows = 1;
                if (parent.getAdapter() != null)
                {
                    cols = parent.getAdapter().getItemCount();
                }
                row = 0;
                col = position;
            }

            if (row > 0) outRect.top = spacer;
            if (row < rows - 1) outRect.bottom = spacer;
            if (col > 0) outRect.left = spacer;
            if (col < cols - 1) outRect.right = spacer;
        }
    }

    public interface OnSelectListener
    {
        void onSelectChanged(int n);
    }

    public void setOnSelectListener(OnSelectListener listener)
    {
        adapter.setOnSelectListener(listener);
    }

    public int calcItemHeight(Configuration config)
    {
        int n;
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT)
            n = Pantip.xLarge ? 4 : 3;
        else n = Pantip.xLarge ? 6 : 5;

        gridLayout.setSpanCount(n);
        itemHeight = Utils.toPixels(config.screenWidthDp) / n;
        return itemHeight;
    }

    void updateLayoutManager(int orientation)
    {
        setLayoutManager(orientation == Configuration.ORIENTATION_PORTRAIT ? gridLayout : linearLayout);
    }

    @Override
    public MyGalleryAdapter getAdapter()
    {
        return adapter;
    }

    public List<Gallery> getSelectedItems()
    {
        return adapter.selected;
    }

    public void clearSelected()
    {
        for (Gallery o : adapter.selected)
        {
            o.selected = false;
            int i = adapter.list.indexOf(o);
            if (i > -1) adapter.notifyItemChanged(i);
        }
        adapter.selected.clear();
    }

    public void scrollToTop()
    {
        smoothScrollToPosition(0);
    }
}
