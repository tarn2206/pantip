package tarn.pantip.app;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Preferences;
import tarn.pantip.content.Tags;
import tarn.pantip.model.ForumMenuItem;
import tarn.pantip.model.MenuItemType;
import tarn.pantip.model.Tag;
import tarn.pantip.model.TagMenuItem;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Search;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.ContentLoadingProgressBar;
import tarn.pantip.widget.ForumAdapter;
import tarn.pantip.widget.ResetFavoriteClickListener;
import tarn.pantip.widget.SearchTagAdapter;
import tarn.pantip.widget.TagAdapter;

/**
 * Date: 13 January 2013
 */
public class MainActivity extends BaseActivity implements MainFragment.MainListener, BottomNavigationView.OnNavigationItemSelectedListener, PopupMenu.OnMenuItemClickListener
{
    static final int RC_LOGIN = 1;
    private static final int RC_PREFERENCES = 2;
    private static final String FORUM = "FORUM";
    public static final String TAG = "TAG";
    private SlidingMenu slidingMenu;
    private ForumAdapter forumAdapter;
    private TagAdapter tagAdapter;
    private SearchTagAdapter searchTagAdapter;
    private EditText searchTag;
    private ContentLoadingProgressBar searchTagProgress;
    private TextView empty;
    private ListView tagListView;
    private State state;
    private MenuItem searchMenu;
    //private MenuItem optionsMenu; // smart search options
    private MenuItem account;
    private Fragment currentFragment;
    private HomeFragment home;
    private MainFragment fragment;
    private int nightMode;
    //private DrawerArrowDrawable arrow;
    private AppBarLayout.LayoutParams toolbarLayout;
    private TabLayout homeTab;
    private boolean homeTabInitialized;
    private AppBarLayout appBar;
    private MenuItem menuNotification;
    private int scrollFlags;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        flatToolbar = true;
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        appBar = findViewById(R.id.app_bar);
        //arrow = new DrawerArrowDrawable(toolbar.getContext());
        homeTab = findViewById(R.id.home_tab);
        toolbarLayout = (AppBarLayout.LayoutParams)toolbar.getLayoutParams();
        scrollFlags = toolbarLayout.getScrollFlags();

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(this);
        Menu bottom = bottomNavigation.getMenu();
        setTooltipText(bottom, R.id.action_forums);
        setTooltipText(bottom, R.id.action_tags);
        setTooltipText(bottom, R.id.action_home).setChecked(true);
        menuNotification = setTooltipText(bottom, R.id.action_notification);
        setTooltipText(bottom, R.id.action_settings);

        try
        {
            updateAccount();
            setupSlidingMenu();

            //Utils.setVisible(fab, Pantip.loggedOn);
            /*if (overlay)
            {
                actionBar.setBackgroundDrawable(new ColorDrawable(0xf05e5b8b));
                StyledAttributes styled = new StyledAttributes(this, PantipApplication.themeId, R.attr.actionBarSize);
                topicListFragment.setPaddingTop(styled.getDimensionPixel());
            }*/

            Intent intent = getIntent();
            if (Intent.ACTION_VIEW.equals(intent.getAction()))
            {
                displayTopics(intent);
            }
            else if (!loadInstanceState(savedInstanceState))
            {
                displayHome();
            }
        }
        catch (Throwable e)
        {
            Pantip.handleException(this, e);
        }
    }

    private MenuItem setTooltipText(Menu menu, @IdRes int id)
    {
        MenuItem item = menu.findItem(id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            item.setTooltipText(item.getTitle());
        }
        return item;
    }

    private void displayHome()
    {
        if (home == null)
        {
            home = new HomeFragment();
            home.tabLayout = homeTab;
        }
        if (currentFragment == home)
        {
            appBar.setExpanded(true);
            home.smoothScrollToTop();
        }
        else
        {
            setTitle(R.string.app_label);
            final Preferences preferences = new Preferences(this);
            preferences.remove("main:mode").remove("main:forum").remove("main:label").remove("main:tag").commit();
            state = null;

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, home)
                       .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            if (fragment != null)
            {
                transaction.remove(fragment);
                fragment = null;
            }
            transaction.commit();
            currentFragment = home;
            if (!homeTabInitialized)
            {
                homeTabInitialized = true;
                homeTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
                {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab)
                    { }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab)
                    { }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab)
                    {
                        home.smoothScrollToTop();
                    }
                });
            }
            toolbarLayout.setScrollFlags(scrollFlags);
        }
    }

    private void createFragment()
    {
        if (fragment == null)
        {
            fragment = new MainFragment();
            fragment.setMainListener(this);
            fragment.setOnItemClickListener(topic -> collapseSearchView());
        }
        if (currentFragment != fragment)
        {
            Utils.setVisible(homeTab, false);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            if (home != null)
            {
                transaction.remove(home);
                home = null;
            }
            transaction.commitNow();
            currentFragment = fragment;
            toolbarLayout.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        }
    }

    void showHomeTab(boolean visible)
    {
        if (homeTab != null) homeTab.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void displayTopics(Intent intent) throws IOException
    {
        String mode = intent.getStringExtra("view");
        if (FORUM.equals(mode))
        {
            String forum = intent.getStringExtra("value");
            String label = Pantip.getDataStore().getForumLabel(forum);
            loadForum(forum, label, false);
        }
        else
        {
            String tag = intent.getStringExtra("value");
            String label = intent.getStringExtra("label");
            if (StringUtils.isBlank(label)) label = Pantip.getDataStore().getTagLabel(tag);
            loadTag(tag, label, null, false);
            if (label == null || label.contains("_"))
            {
                final String finalTag = tag;
                postDelayed(() -> {
                    String label1 = Pantip.getDataStore().getTagLabel(finalTag);
                    if (StringUtils.isNotBlank(label1)) setTitle(label1);
                }, 3000);
            }
        }
    }

    private void loadForum(String forum, String label, boolean resetType)
    {
        if (resetType && fragment != null) fragment.resetTopicType();

        setTitle(label == null ? forum : label);
        collapseSearchView();
        if (slidingMenu.isMenuShowing()) slidingMenu.toggle();
        createFragment();

        state = new State(FORUM, forum, label, null);
        fragment.loadData(true, forum, null);
    }

    private void loadTag(String tag, String label, String forum, boolean resetType)
    {
        if (resetType && fragment != null) fragment.resetTopicType();
        createFragment();

        setTitle(label == null ? tag : label);
        collapseSearchView();
        if (slidingMenu.isMenuShowing()) slidingMenu.toggle();

        state = new State(TAG, forum, label, tag);
        fragment.loadData(false, forum, tag);
        Pantip.getDataStore().updateTagFavorite(forum, label == null ? "" : label, tag, true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Pantip.main = this;
        if (tagAdapter != null) tagAdapter.updateFavorites();
    }

    /*@Override
    protected void requestWindowFeature()
    {
        overlay = supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
    }*/

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        saveInstanceState(outState);
    }

    private boolean loadInstanceState(Bundle savedInstanceState)
    {
        final Preferences preferences = new Preferences(this, savedInstanceState);
        state = new State(preferences.getString("main:mode"), preferences.getString("main:forum"), preferences.getString("main:label"), preferences.getString("main:tag"));
        if (tagAdapter != null) tagAdapter.loadInstanceState(preferences, state.forum, state.tag);
        if (state.label != null)
        {
            setTitle(state.label);
            createFragment();
            postDelayed(() -> {
                fragment.loadData(state.mode.equals(FORUM), state.forum, state.tag);
            }, 500);
            return true;
        }
        return false;
    }

    private void saveInstanceState(Bundle outState)
    {
        Preferences preferences = new Preferences(this, outState);
        if (state != null)
        {
            preferences.putString("main:mode", state.mode);
            preferences.putString("main:forum", state.forum);
            preferences.putString("main:label", state.label);
            preferences.putString("main:tag", state.tag);
        }
        if (tagAdapter != null) tagAdapter.saveInstanceState(preferences);
        preferences.commit();
    }

    private void setupSlidingMenu()
    {
        slidingMenu = new SlidingMenu(this);
        slidingMenu.setBackgroundResource(R.color.colorPrimaryDark);
        slidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        slidingMenu.setTouchmodeMarginThreshold(Pantip.displayWidth / 4);
        slidingMenu.setMenu(R.layout.drawer_forum_listview);
        slidingMenu.setSecondaryMenu(R.layout.drawer_tag_listview);
        slidingMenu.setBehindScrollScale(0);
        slidingMenu.setFadeDegree(0);
        slidingMenu.setShadowDrawable(R.drawable.drawer_left_shadow);
        slidingMenu.setSecondaryShadowDrawable(R.drawable.drawer_right_shadow);
        slidingMenu.setShadowWidthRes(R.dimen.menu_shadow_width);
        slidingMenu.setOnOpenListener(hideSoftInput);
        slidingMenu.setSecondaryOnOpenListner(hideSoftInput);
        slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
        //slidingMenu.setProgressListener(this);
        updateMenuWidth();

        ListView listView = findViewById(R.id.menu_listview);
        forumAdapter = new ForumAdapter(this);
        listView.setAdapter(forumAdapter);
        listView.setOnItemClickListener(forumClickListener);
        listView.setOnItemLongClickListener(forumLongClickListener);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, Pantip.spacer);
        View footer = new View(this);
        footer.setLayoutParams(params);
        listView.addFooterView(footer);

        searchTagProgress = findViewById(R.id.search_tag_progress);
        searchTagProgress.setVisibility(View.GONE);
        empty = findViewById(android.R.id.empty);

        searchTag = findViewById(R.id.search_tag);
        searchTag.setTextSize(Pantip.textSize);
        searchTag.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;

            final String query = searchTag.getText().toString().trim();
            if (query.length() == 0) return false;

            searchTagProgress.show();
            if (searchTagAdapter == null)
            {
                searchTagAdapter = new SearchTagAdapter(MainActivity.this, null);
                tagListView.setAdapter(searchTagAdapter);
                tagListView.setVerticalScrollBarEnabled(true);
            }
            else
            {
                searchTagAdapter.list.clear();
                searchTagAdapter.notifyDataSetChanged();
            }
            Tags.search(query)
                .subscribe(tags -> {
                    searchTagProgress.hide();
                    searchTagAdapter.list.addAll(tags);
                    if (searchTagAdapter.list.size() > 0)
                    {
                        Utils.hideKeyboard(MainActivity.this);
                        searchTagAdapter.notifyDataSetChanged();
                        Utils.setVisible(empty, false);
                    }
                    else
                    {
                        String text = "ไม่พบแท็ก " + query;
                        empty.setText(text);
                        Utils.setVisible(empty, true);
                    }
                }, tr -> {
                    searchTagProgress.hide();
                    Utils.showToast(MainActivity.this, tr.getMessage());
                });
            return true;
        });
        findViewById(R.id.clear).setOnClickListener(v -> restoreTagAdapter());
        tagAdapter = new TagAdapter(this);
        tagListView = findViewById(R.id.tag_listview);
        tagListView.setAdapter(tagAdapter);
        tagListView.setVerticalScrollBarEnabled(false);
        tagListView.setOnItemClickListener(tagClickListener);
        tagListView.setOnItemLongClickListener(tagLongClickListener);
        params = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, Pantip.spacer);
        footer = new View(this);
        footer.setLayoutParams(params);
        tagListView.addFooterView(footer);
    }

    private final SlidingMenu.OnOpenListener hideSoftInput = this::collapseSearchView;

    private void restoreTagAdapter()
    {
        Utils.hideKeyboard(this);
        searchTag.setText("");
        searchTagProgress.hide();
        Utils.setVisible(empty, false);
        if (searchTagAdapter != null)
        {
            searchTagAdapter = null;
            tagListView.setAdapter(tagAdapter);
            tagListView.setVerticalScrollBarEnabled(false);
        }
    }

    private final AdapterView.OnItemClickListener forumClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(final AdapterView<?> parent, View view, int position, long id)
        {
            try
            {
                ForumMenuItem item = (ForumMenuItem)forumAdapter.getItem(position);
                if (item.type == MenuItemType.Item)
                {
                    loadForum(item.url, item.label, true);
                    Pantip.getDataStore().increaseForumFavorite(item.label);
                    postDelayed(() -> {
                        parent.setSelection(0);
                        forumAdapter.loadData();
                    }, 1000);
                }
            }
            catch (Exception e)
            {
                Pantip.handleException(MainActivity.this, e);
            }
        }
    };

    private final AdapterView.OnItemLongClickListener forumLongClickListener = new AdapterView.OnItemLongClickListener()
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
        {
            ForumMenuItem item = (ForumMenuItem)forumAdapter.getItem(position);
            if (item.type != MenuItemType.Item || item.hitCount == 0) return false;

            SpannableString message = new SpannableString("ลบห้อง " + item.label + " ออกจากรายการโปรดไหม?");
            message.setSpan(new StyleSpan(Typeface.BOLD), 7, 7 + item.label.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            ConfirmDialog.delete(MainActivity.this, message, new ResetFavoriteClickListener(MainActivity.this, item.label));
            return true;
        }
    };

    private final AdapterView.OnItemClickListener tagClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(final AdapterView<?> parent, View view, int position, long id)
        {
            Object item = parent.getAdapter().getItem(position);
            if (item instanceof TagMenuItem)
            {
                TagMenuItem tag = (TagMenuItem) item;
                loadTag(tag.url, tag.label, tag.forum, true);
            }
            else
            {
                Tag tag = (Tag) item;
                if (tag != null)
                {
                    loadTag(tag.url, tag.label, null, true);
                    restoreTagAdapter();
                }
            }
            postDelayed(() -> {
                parent.setSelection(0);
                tagAdapter.notifyDataSetChanged();
            }, 1000);
        }
    };

    private final AdapterView.OnItemLongClickListener tagLongClickListener = (parent, view, position, id) -> {
        Object item = parent.getAdapter().getItem(position);
        if (!(item instanceof TagMenuItem)) return false;

        TagMenuItem tag = (TagMenuItem)item;
        if (tag.hitCount == 0) return false;

        SpannableString message = new SpannableString("ลบแท็ก " + tag.label + " ออกจากรายการโปรดไหม?");
        message.setSpan(new StyleSpan(Typeface.BOLD), 7, 7 + tag.label.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        ConfirmDialog.delete(MainActivity.this, message, new ResetFavoriteClickListener(MainActivity.this, tag.forum, tag.label, tag.url));
        return true;
    };

    public void favoriteForumChanged()
    {
        forumAdapter.loadData();
    }

    public void favoriteTagChanged()
    {
        if (state != null) tagAdapter.reload(state.forum, state.tag);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        searchMenu = Search.setup(this, menu);
        //optionsMenu = menu.findItem(R.id.action_options);
        account = menu.findItem(R.id.action_account);
        account.getActionView().findViewById(R.id.avatar)
               .setOnClickListener(v -> onOptionsItemSelected(account));
        updateAccount();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu)
    {
        if ((featureId & Window.FEATURE_ACTION_BAR) == Window.FEATURE_ACTION_BAR) searchMenu.collapseActionView();
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_account)
        {
            if (Pantip.loggedOn)
            {
                collapseSearchView();
                PopupMenu popup = new PopupMenu(this, findViewById(R.id.action_account));
                popup.setOnMenuItemClickListener(this);
                popup.inflate(R.menu.account);
                popup.show();
            }
            else startActivityForResult(new Intent(this, LoginActivity.class), RC_LOGIN);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_home:
                displayHome();
                return false;
            case R.id.action_forums:
                slidingMenu.showMenu();
                return false;
            case R.id.action_tags:
                slidingMenu.showSecondaryMenu();
                return false;
            case R.id.action_notification:
                startActivity(new Intent(this, NotificationActivity.class));
                return false;
            case R.id.action_settings:
                nightMode = Pantip.nightMode;
                startActivityForResult(new Intent(this, SettingsActivity.class), RC_PREFERENCES);
                return false;
        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_my:
                startActivity(new Intent(this, MyTopicActivity.class));
                return true;
            case R.id.action_post:
                startActivity(new Intent(this, PostActivity.class));
                return true;
            case R.id.action_open:
                OpenTopicDialog.show(this);
                break;
            case R.id.action_logout:
                logout();
                return true;
        }
        return false;
    }

    private void collapseSearchView()
    {
        if (searchMenu == null || !searchMenu.isActionViewExpanded()) return;
        postDelayed(() -> searchMenu.collapseActionView(), 100);
    }

    @Override
    protected void onOrientationChanged(int orientation)
    {
        invalidateOptionsMenu();
        if (slidingMenu.isMenuShowing())
        {
            slidingMenu.toggle(false);
            //onProgress(0);
        }
        updateMenuWidth();
    }

    private void updateMenuWidth()
    {
        Point point = Utils.getDisplaySize();
        int w = (int)getResources().getDimension(R.dimen.menu_width);
        if (point.x - w > Pantip.actionBarSize)
            slidingMenu.setBehindWidthRes(R.dimen.menu_width);
        else
            slidingMenu.setBehindOffset(Pantip.actionBarSize);
    }

    @Override
    protected boolean onDoubleTapToolbar(MotionEvent e)
    {
        if (fragment != null)
        {
            fragment.smoothScrollToTop();
            return true;
        }
        return false;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        saveInstanceState(null);
    }

    @Override
    public void onBackPressed()
    {
        if (slidingMenu != null && slidingMenu.isMenuShowing())
        {
            slidingMenu.toggle();
            return;
        }

        if (home != null && home.smoothScrollToTop()) return;
        if (fragment != null && fragment.smoothScrollToTop()) return;

        super.onBackPressed();
    }

    private void updateAccount()
    {
        menuNotification.setEnabled(Pantip.loggedOn);
        if (account == null) return;
        //Utils.setVisible(fab, Pantip.loggedOn);
        //forumAdapter.loadData();
        ImageView view = account.getActionView().findViewById(R.id.avatar);
        if (Pantip.loggedOn)
        {
            GlideApp.with(this).load(Pantip.currentUser.avatar)
                    .placeholder(R.drawable.circle)
                    .transform(new CircleCrop()).into(view);
        }
        else view.setImageResource(R.drawable.ic_account_circle_white_24dp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) view.setTooltipText(Pantip.loggedOn ? "" : "เข้าสู่ระบบ");
    }

    private void logout()
    {
        LogoutDialog.show(this, new LogoutDialog.LogoutCallback()
        {
            @Override
            public void complete()
            {
                updateAccount();
            }

            @Override
            public void error(Throwable tr)
            {
                updateAccount();
                Pantip.handleException(MainActivity.this, tr);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        try
        {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode)
            {
                case RC_LOGIN:
                    if (resultCode == RESULT_OK)
                    {
                        updateAccount();
                    }
                    break;

                case RC_PREFERENCES:
                    Pantip.loadPreferences();
                    if (Pantip.nightMode != nightMode) recreate();
                    else if (fragment != null) fragment.update();
                    else if (home != null) home.update();
                    updateAccount();
                    break;
            }
        }
        catch (Exception e)
        {
            Pantip.handleException(this, e);
        }
    }

    @Override
    public void onLoad(int page)
    {
        //slidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
        if (state == null) return;
        if (page == 1)
        {
            tagAdapter.reload(state.forum, state.tag);
        }
        saveInstanceState(null);
    }

    @Override
    public void onError(Throwable tr)
    {
        L.e(tr);
    }

    public void reloadTag()
    {
        if (tagAdapter != null && state != null) tagAdapter.reload(state.forum, state.tag);
    }

    /*@Override
    public void onProgress(float progress)
    {
        if (progress == 1f) arrow.setVerticalMirror(true);
        else if (progress == 0f) arrow.setVerticalMirror(false);
        arrow.setProgress(progress);
    }*/

    static class State
    {
        public final String mode;
        public final String forum;
        public final String tag;
        public final String label;

        State(String mode, String forum, String label, String tag)
        {
            this.mode = mode;
            this.forum = forum;
            this.label = label;
            this.tag = tag;
        }

        @NonNull
        @Override
        public String toString()
        {
            return "View: " + mode + ", Forum: " + forum + ", Label: " + label + ", Tag: " + tag;
        }
    }
}
