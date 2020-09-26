package tarn.pantip.app;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import org.apache.commons.io.IOUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.internal.functions.Functions;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Bookmark;
import tarn.pantip.content.CommentData;
import tarn.pantip.content.HttpException;
import tarn.pantip.content.Json;
import tarn.pantip.content.LocalObject;
import tarn.pantip.content.ObjectStore;
import tarn.pantip.content.Preferences;
import tarn.pantip.model.Comment;
import tarn.pantip.model.Topic;
import tarn.pantip.model.TopicEx;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.CommentAdapter;
import tarn.pantip.widget.ContentLoadingProgressBar;

/**
 * User: Tarn
 * Date: 5/4/13 12:52 AM
 */
public class TopicActivity extends BaseActivity
{
    public static final int RC_REPLY = 1;
    public static final int RC_EDIT = 2;
    private TopicEx topic;
    private MenuItem menuRefresh;
    private MenuItem menuReply;
    private MenuItem menuNotify;
    private MenuItem menuFavorite;
    private MenuItem menuShare;
    private Drawable notifyOff;
    private Drawable notifyOn;
    private Drawable favoriteOff;
    private Drawable favoriteOn;
    private RecyclerView recycler;
    private LinearLayoutManager layoutManager;
    private CommentAdapter adapter;
    private ContentLoadingProgressBar progressBar;
    private TextView errorView;
    private WebView webView;
    private boolean processGoTo;
    private String goTo;
    private boolean loaded;
    private boolean silentUpdate = true;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);

        recycler = findViewById(R.id.recycler_view);
        recycler.setLayoutManager(layoutManager = new LinearLayoutManager(this));
        SimpleItemAnimator animator = (SimpleItemAnimator)recycler.getItemAnimator();
        if (animator != null) animator.setSupportsChangeAnimations(false);
        recycler.setAdapter(new CommentAdapter(this, topic)); // prevent no adapter error

        progressBar = findViewById(android.R.id.progress);
        progressBar.setVisibility(View.GONE);
        errorView = findViewById(R.id.error_text);
        errorView.setVisibility(View.GONE);
        errorView.setTextSize(Pantip.textSize);
        int p = Utils.toPixels(14);
        errorView.setPadding(p, p, p, p);
        webView = findViewById(R.id.webview);
        webView.setVisibility(View.GONE);

        if (savedInstanceState == null)
        {
            Intent intent = getIntent();
            if (intent.hasExtra("id"))
            {
                long id = intent.getLongExtra("id", 0);
                topic = new TopicEx();
                topic.id = id;
                topic.title = intent.getStringExtra("title");
                goTo = intent.getStringExtra("goTo");
            }
            else if (intent.hasExtra("url"))
            {
                getRedirectUrl(intent.getStringExtra("url"))
                        .subscribe(location -> {
                            if (location == null)
                            {
                                Utils.showToast(TopicActivity.this, "Topic not found");
                                finish();
                                return;
                            }
                            int i = location.lastIndexOf('/');
                            topic = new TopicEx();
                            topic.id = Long.parseLong(location.substring(i + 1));
                        }, tr -> {
                            Utils.showToast(TopicActivity.this, "Topic not found");
                            finish();
                        });
                return;
            }
            else if (intent.hasExtra("topic"))
            {
                String s = intent.getStringExtra("topic");
                Topic t = Json.fromJson(s, Topic.class);
                topic = t == null ? null : new TopicEx(t);
            }
        }
        else
        {
            Preferences preferences = new Preferences(savedInstanceState);
            topic = preferences.getObject("topic", TopicEx.class);
        }

        if (topic == null)
        {
            Utils.showToast(this, "No topic info");
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.topic, menu);
        menuRefresh = menu.findItem(R.id.action_refresh);
        menuRefresh.setIcon(getMenuIcon(R.drawable.ic_refresh_black_24dp));
        menuReply = menu.findItem(R.id.action_reply);
        menuReply.setIcon(getMenuIcon(R.drawable.ic_comment_24dp));
        menuNotify = menu.findItem(R.id.action_notification);
        menuFavorite = menu.findItem(R.id.action_favorites);
        menuShare = menu.findItem(R.id.action_share);
        menuShare.setIcon(getMenuIcon(R.drawable.ic_share_white_24dp));

        notifyOff = getMenuIcon(R.drawable.ic_notifications_none_black_24dp);
        notifyOn = getMenuIcon(R.drawable.ic_notifications_black_24dp, 0xfffcb414);
        favoriteOff = getMenuIcon(R.drawable.ic_favorite_border_black_24dp);
        favoriteOn = getMenuIcon(R.drawable.ic_favorite_black_24dp, 0xfffcb414);
        updateNotifyMenu();
        updateFavoriteMenu();

        menuReply.setVisible(Pantip.loggedOn);
        menuReply.setEnabled(false);
        menuRefresh.setEnabled(false);
        menuNotify.setVisible(Pantip.loggedOn);
        menuNotify.setEnabled(false);
        menuFavorite.setVisible(Pantip.loggedOn);
        menuFavorite.setEnabled(false);
        menuShare.setEnabled(false);

        setTitle(String.valueOf(topic.id));
        if (topic.error != null || topic.deleteMessage != null)
        {
            menuRefresh.setVisible(false);
            menuReply.setVisible(false);
            menuShare.setVisible(false);
            menuNotify.setEnabled(topic.follow);
            menuNotify.setVisible(topic.follow);
            menuFavorite.setEnabled(topic.favorite);
            menuFavorite.setVisible(topic.favorite);
            progressBar.hide();
            recycler.setVisibility(View.GONE);
            if (topic.error != null)
            {
                webView.loadData(topic.error, "text/html", "UTF-8");
                webView.setVisibility(View.VISIBLE);
            }
            else
            {
                errorView.setText(topic.deleteMessage);
                errorView.setVisibility(View.VISIBLE);
            }
        }
        else if (loaded)
        {
            menuReply.setEnabled(true);
            menuRefresh.setEnabled(true);
            menuShare.setEnabled(true);
            menuNotify.setEnabled(true);
            menuFavorite.setEnabled(true);
        }
        else
        {
            loaded = true;
            postDelayed(this::loadData, 100);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void loadData()
    {
        progressBar.show();
        Comment.load(topic.id).subscribe(result -> {
            boolean needUpdate = true;
            if (result.items != null && result.items.length > 0)
            {
                needUpdate = Utils.needUpdate(result.lastModified);
                processGoTo = !needUpdate;
                complete(result);
                if (result.items[0].deleteMessage != null) return;
            }

            if (needUpdate)
            {
                processGoTo = true;
                onRefresh(true);
            }
        }, tr -> Pantip.handleException(this, tr));
    }

    public void onRefresh(boolean silentUpdate)
    {
        this.silentUpdate = silentUpdate;
        if (menuRefresh != null) menuRefresh.setEnabled(false);
        if (menuNotify != null) menuNotify.setEnabled(false);
        if (menuFavorite != null) menuFavorite.setEnabled(false);

        CommentData.loadComments(topic)
                .subscribe(items -> {
                    LocalObject<Comment> result = new LocalObject<>();
                    result.items = items.toArray(new Comment[0]);
                    this.complete(result);
                }, this::error);
    }

    private void complete(LocalObject<Comment> result)
    {
        progressBar.hide();

        if ((adapter == null || adapter.getItemCount() == 0) && result.items[0] instanceof TopicEx)
        {
            topic = (TopicEx)result.items[0];
            updateNotifyMenu();
            updateFavoriteMenu();
        }

        if (topic.error != null || topic.deleteMessage != null)
        {
            menuRefresh.setVisible(false);
            menuReply.setVisible(false);
            menuShare.setVisible(false);
            menuNotify.setEnabled(topic.follow);
            menuNotify.setVisible(topic.follow);
            menuFavorite.setEnabled(topic.favorite);
            menuFavorite.setVisible(topic.favorite);
            progressBar.hide();
            recycler.setVisibility(View.GONE);
            if (topic.error != null)
            {
                webView.loadData(topic.error, "text/html", "UTF-8");
                webView.setVisibility(View.VISIBLE);
            }
            else
            {
                errorView.setText(topic.deleteMessage);
                errorView.setVisibility(View.VISIBLE);
            }
            return;
        }

        menuReply.setEnabled(true);
        menuRefresh.setEnabled(true);
        menuShare.setEnabled(true);
        menuNotify.setEnabled(true);
        menuFavorite.setEnabled(true);
        updateNotifyMenu();
        updateFavoriteMenu();
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);

        if (adapter == null)
        {
            recycler.setAdapter(adapter = new CommentAdapter(this, topic));
            Pantip.getDataStore().markAsRead(topic.id);
        }

        if (result.lastModified == 0)
        {
            if (adapter.getItemCount() > 0)
            {
                int count = result.items.length - adapter.getItemCount();
                if (count > 0) Utils.showToast(this, "มี " + count + " ความคิดเห็นใหม่");
                else if (!silentUpdate)
                {
                    silentUpdate = true;
                    Utils.showToast(this, "ไม่มีความคิดเห็นใหม่");
                }
            }
            adapter.update(result.items);
        }
        else adapter.append(result.items);

        if (result.lastModified == 0) saveComments();

        if (processGoTo && goTo != null) scrollTo();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        saveComments(); // save story.thumbSize
    }

    private void error(Throwable tr)
    {
        progressBar.hide();
        recycler.setVisibility(View.GONE);

        String message = tr instanceof HttpException ? ((HttpException)tr).text() : tr.getMessage();
        errorView.setText(Utils.fromHtml(message));
        errorView.setVisibility(View.VISIBLE);
        L.e(tr);
        menuRefresh.setEnabled(true);
        menuShare.setEnabled(false);
    }

    private void scrollTo()
    {
        String[] a = goTo.split("-");
        int cNo = Integer.parseInt(a[0]);
        int rNo = a.length > 1 ? Integer.parseInt(a[1]) : 0;

        final int position = adapter.getPosition(cNo, rNo);
        if (position != -1)
        {
            goTo = null;
            postDelayed(() -> layoutManager.scrollToPositionWithOffset(position, 0), 100);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        new Preferences(this, outState).putObject("topic", topic).commit();
    }

    public void saveComments()
    {
        if (adapter != null)
            Comment.save(adapter.getItems())
                    .subscribe(Functions.emptyConsumer(), tr -> Pantip.handleException(this, tr));
    }

    @Override
    public void onBackPressed()
    {
        close(false);
    }

    private void close(boolean success)
    {
        if (topic != null)
        {
            Intent data = new Intent();
            data.putExtra("id", topic.id);
            data.putExtra("favorite", topic.favorite);
            data.putExtra("follow", topic.follow);
            setResult(RESULT_OK, data);
        }
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;
        try
        {
            switch (item.getItemId())
            {
                case android.R.id.home:
                    close(false);
                    return true;

                case R.id.action_refresh:
                    onRefresh(false);
                    return true;

                case R.id.action_reply:
                    intent = new Intent(this, ReplyActivity.class);
                    intent.putExtra("topic", ObjectStore.put(topic));
                    startActivityForResult(intent, RC_REPLY);
                    return true;

                case R.id.action_notification:
                    menuNotify.setEnabled(false);
                    notification();
                    break;

                case R.id.action_favorites:
                    menuFavorite.setEnabled(false);
                    if (topic.deleteMessage == null) favorites();
                    else deleteFavorites();
                    return true;

                case R.id.action_share:
                    intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, topic.title);
                    intent.putExtra(Intent.EXTRA_TEXT, topic.title + "\n" + "https://pantip.com/topic/" + topic.id);
                    startActivity(Intent.createChooser(intent, "Share"));
                    return true;

                case R.id.action_browser:
                    if (topic != null) Utils.openBrowser(this, "https://m.pantip.com/topic/" + topic.id);
                    return true;
            }
        }
        catch (Exception e)
        {
            Pantip.handleException(this, e);
        }
        return super.onOptionsItemSelected(item);
    }

    private void notification()
    {
        Observable<String> observable = topic.follow ? Bookmark.unfollow(topic.id) : Bookmark.follow(topic.id);
        observable.subscribe(result -> {
            topic.follow = !topic.follow;
            menuNotify.setEnabled(true);
            updateNotifyMenu();
            saveComments();
        }, tr -> {
            menuNotify.setEnabled(true);
            Utils.showToast(TopicActivity.this, tr.getMessage());
        });
    }

    private void favorites()
    {
        Observable<Boolean> observable = topic.favorite ? Bookmark.remove(topic.id) : Bookmark.add(topic.id);
        observable.subscribe(success -> {
            menuFavorite.setEnabled(true);
            if (success)
            {
                topic.favorite = !topic.favorite;
                updateFavoriteMenu();
                saveComments();
            }
            else Utils.showToast(TopicActivity.this, topic.favorite ? "ไม่สามารถเก็บเป็นกระทู้โปรดได้" : "ไม่สามารถนำออกจากกระทู้โปรดได้");
        }, this::bookmarkError);
    }

    private void deleteFavorites()
    {
        Bookmark.removeMy(topic.id)
                .subscribe(success -> {
                    if (success)
                    {
                        Utils.showToast(TopicActivity.this, "นำออกจากกระทู้โปรดแล้ว");
                    }
                    close(true);
                }, this::bookmarkError);
    }

    private void bookmarkError(Throwable tr)
    {
        menuFavorite.setEnabled(true);
        Utils.showToast(TopicActivity.this, tr.getMessage());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (resultCode == RESULT_CANCELED) return;
        if (requestCode == RC_REPLY)
        {
            //commentNo = intent.getIntExtra("commentNo", -1);
            onRefresh(true);
        }
        else if (requestCode == RC_EDIT)
        {
            //editNo = intent.getStringExtra("no");
            onRefresh(true);
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected boolean onDoubleTapToolbar(MotionEvent e)
    {
        LinearLayoutManager layout = (LinearLayoutManager)recycler.getLayoutManager();
        if (layout == null) return false;

        int first = layout.findFirstVisibleItemPosition();
        if (first == 0) return false;

        if (first > 10) recycler.scrollToPosition(10);
        recycler.smoothScrollToPosition(0);

        return true;
    }

    @Override
    protected void onOrientationChanged(int orientation)
    {
        invalidateOptionsMenu();
    }

    private void updateNotifyMenu()
    {
        if (topic == null) return;
        menuNotify.setTitle(topic.follow ? R.string.menu_unfollow : R.string.menu_follow);
        menuNotify.setIcon(topic.follow ? notifyOn : notifyOff);
    }

    private void updateFavoriteMenu()
    {
        if (topic == null) return;
        menuFavorite.setTitle(topic.favorite ? R.string.menu_remove_favorite : R.string.menu_add_favorite);
        menuFavorite.setIcon(topic.favorite ? favoriteOn : favoriteOff);
    }

    private Observable<String> getRedirectUrl(String url)
    {
        return RxUtils.observe(() -> {
            HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            boolean success = status >= 200 && status < 400;
            if (!success)
            {
                String content = IOUtils.toString(connection.getErrorStream(), StandardCharsets.UTF_8);
                throw new HttpException("GET", url, status, connection.getResponseMessage(), connection.getHeaderFields(), content);
            }
            return connection.getHeaderField("Location");
        });
    }
}
