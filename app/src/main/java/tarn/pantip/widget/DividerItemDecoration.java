package tarn.pantip.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Tarn on 30 August 2016
 */
class DividerItemDecoration extends RecyclerView.ItemDecoration
{
    private static final int VERTICAL = LinearLayout.VERTICAL;
    private final Drawable divider;
    private final int orientation;
    private final Rect outBounds = new Rect();

    DividerItemDecoration(Context context)
    {
        this(context, VERTICAL);
    }

    private DividerItemDecoration(Context context, int orientation)
    {
        TypedArray a = context.obtainStyledAttributes(new int[] { android.R.attr.listDivider });
        divider = a.getDrawable(0);
        a.recycle();
        this.orientation = orientation;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
    {
        canvas.save();
        if (orientation == VERTICAL)
        {
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();
            canvas.clipRect(left, parent.getPaddingTop(), right, parent.getHeight() - parent.getPaddingBottom());

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++)
            {
                View child = parent.getChildAt(i);
                parent.getDecoratedBoundsWithMargins(child, outBounds);
                final int bottom = outBounds.bottom + Math.round(child.getTranslationY());
                final int top = bottom - 1;//divider.getIntrinsicHeight();
                divider.setBounds(left, top, right, bottom);
                divider.draw(canvas);
            }
        }
        else
        {
            final int top = parent.getPaddingTop();
            final int bottom = parent.getHeight() - parent.getPaddingBottom();
            canvas.clipRect(parent.getPaddingLeft(), top, parent.getWidth() - parent.getPaddingRight(), bottom);

            RecyclerView.LayoutManager layout = parent.getLayoutManager();
            if (layout != null)
            {
                final int childCount = parent.getChildCount();
                for (int i = 0; i < childCount; i++)
                {
                    View child = parent.getChildAt(i);
                    layout.getDecoratedBoundsWithMargins(child, outBounds);
                    final int right = outBounds.right + Math.round(child.getTranslationX());
                    final int left = right - 1;//outBounds.getIntrinsicWidth();
                    divider.setBounds(left, top, right, bottom);
                    divider.draw(canvas);
                }
            }
        }
        canvas.restore();
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
    {
        if (orientation == VERTICAL)
            outRect.set(0, 0, 0, 1/*divider.getIntrinsicHeight()*/);
        else
            outRect.set(0, 0, 1/*divider.getIntrinsicWidth()*/, 0);
    }
}