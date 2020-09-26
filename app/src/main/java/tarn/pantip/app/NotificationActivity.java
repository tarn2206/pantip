package tarn.pantip.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.util.Date;

import tarn.pantip.content.Json;
import tarn.pantip.content.Notify;
import tarn.pantip.content.Preferences;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.NotificationAdapter;

/**
 * Created by Tarn on 21/1/2561.
 */

public class NotificationActivity extends RecyclerActivity<Notify[]>
{
    public static final int FOLLOW = 1;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        MyTopicActivity.addAvatar(this, toolbar);
    }

    @Override
    protected boolean onLoad(Bundle savedInstanceState)
    {
        Preferences preferences = savedInstanceState == null ? new Preferences(this) : new Preferences(savedInstanceState);
        setAdapter(adapter = new NotificationAdapter(this));
        String data = preferences.getString("data");
        adapter.append(Json.toArray(data, Notify[].class));
        if (adapter.getItemCount() > 0) showRecycler();
        if (savedInstanceState == null)
        {
            long timestamp = preferences.getLong("timestamp");
            if (timestamp == 0 || Utils.needUpdate(timestamp))
            {
                if (timestamp > 0) setRefreshing();
                onRefresh();
            }
        }
        return adapter.getItemCount() > 0;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if (adapter != null) outState.putString("data", Json.toJson(adapter.getItems()));
    }

    @Override
    public void onRefresh()
    {
        Notify.load().subscribe(this::complete, this::error);
    }

    @Override
    protected boolean onFinish(boolean isRefreshing, Notify[] result)
    {
        if (isRefreshing)
        {
            adapter.update(result);
            recycler.scrollToTop();
        }
        else adapter.append(result);

        RxUtils.observe(emitter -> {
            Preferences preferences = new Preferences(this);
            preferences.putString("data", Json.toJson(adapter.getItems())).commit();
            if (isRefreshing)
            {
                preferences.putLong("timestamp", new Date().getTime()).commit();
            }
            emitter.onComplete();
        }).subscribe();
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FOLLOW && resultCode == RESULT_OK)
        {
            long topicId = data.getLongExtra("id", 0);
            if (topicId == 0) return;
            boolean follow = data.getBooleanExtra("follow", true);
            if (!follow && adapter.remove(topicId))
            {
                new Preferences(this).putString("data", Json.toJson(adapter.getItems())).commit();
            }
        }
    }
}
