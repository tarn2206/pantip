package tarn.pantip.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;

import java.lang.reflect.Field;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.util.ApiAware;

/**
 * User: Tarn
 * Date: 5/4/13 1:31 AM
 */
public abstract class BaseActivity extends AppCompatActivity
{
    Toolbar toolbar;
    boolean flatToolbar;
    private final Handler handler = new Handler();
    private int orientation;
    private int currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setTheme(currentTheme = Pantip.currentTheme);
        super.onCreate(savedInstanceState);
        orientation = getResources().getConfiguration().orientation;
        ApiAware.setTaskDescription(this, null);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void onContentChanged()
    {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null)
        {
            if (!flatToolbar)
            {
                toolbar.setElevation(toPixel(3));
            }
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

            GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
            {
                @Override
                public boolean onDoubleTap(MotionEvent e)
                {
                    return onDoubleTapToolbar(e);
                }
            });
            View.OnTouchListener onTouchListener = (v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            };
            toolbar.setOnTouchListener(onTouchListener);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Pantip.initTheme(true);
        if (currentTheme != Pantip.currentTheme)
        {
            recreate();
        }
    }

    @Override
    public void onActionModeStarted(ActionMode mode)
    {
        if (Pantip.isNightMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            try
            {
                Object mFloatingToolbar = getDeclaredField(mode, "mFloatingToolbar");
                if (mFloatingToolbar != null)
                {
                    Object mPopup = getDeclaredField(mFloatingToolbar, "mPopup");
                    if (mPopup != null)
                    {
                        ViewGroup mContentContainer = (ViewGroup)getDeclaredField(mPopup, "mContentContainer");
                        if (mContentContainer != null)
                        {
                            mContentContainer.setBackgroundResource(R.color.actionModeBackground);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                L.e(e);
            }
        }
        super.onActionModeStarted(mode);
    }

    private Object getDeclaredField(Object obj, String name) throws NoSuchFieldException, IllegalAccessException
    {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }

    public int getToolbarHeight()
    {
        Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        return rect.top + toolbar.getHeight();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (orientation != newConfig.orientation)
        {
            orientation = newConfig.orientation;
            onOrientationChanged(orientation);
        }
    }

    void onOrientationChanged(int orientation)
    { }

    boolean onDoubleTapToolbar(MotionEvent e)
    {
        return false;
    }

    SharedPreferences getPreferences()
    {
        return getPreferences(Context.MODE_PRIVATE);
    }

    View inflate(@LayoutRes int id, ViewGroup parent)
    {
        return LayoutInflater.from(this).inflate(id, parent, false);
    }

    int toPixel(float value)
    {
        return toPixel(TypedValue.COMPLEX_UNIT_DIP, value);
    }

    int toPixel(int unit, float value)
    {
        return (int)TypedValue.applyDimension(unit, value, getResources().getDisplayMetrics());
    }

    void postDelayed(final Runnable r, long delayMillis)
    {
        handler.postDelayed(() -> {
            try
            {
                r.run();
            }
            catch (Exception e)
            {
                L.e(e);
            }
        }, delayMillis);
    }

    @Override
    public void setTitle(CharSequence title)
    {
        super.setTitle(title + " ");
    }

    void setHomeIcon(@DrawableRes int resId)
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setHomeAsUpIndicator(resId);
    }

    StateListDrawable getMenuIcon(@DrawableRes int iconRes)
    {
        return getMenuIcon(iconRes, Color.WHITE);
    }

    StateListDrawable getMenuIcon(@DrawableRes int iconRes, int color)
    {
        StateListDrawable stateList = new StateListDrawable();
        stateList.addState(new int[] { -android.R.attr.state_enabled }, getIcon(iconRes, 0x66FFFFFF));
        stateList.addState(StateSet.WILD_CARD, getIcon(iconRes, color));
        return stateList;
    }

    private Drawable getIcon(@DrawableRes int iconRes, int tint)
    {
        Drawable icon = AppCompatResources.getDrawable(this, iconRes);
        if (icon == null || tint == 0) return icon;
        Drawable wrappedIcon = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(wrappedIcon, tint);
        return wrappedIcon;
    }
}