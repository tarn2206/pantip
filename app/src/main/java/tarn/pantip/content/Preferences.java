package tarn.pantip.content;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * User: Tarn
 * Date: 9/13/13 4:12 PM
 * This is a mix save and restore instance state.
 * for temporary save in onSaveInstanceState and onRestoreInstanceState (restore in onCreate)
 * for persist in onPause and restore in onCreate
 */
public class Preferences
{
    private Intent intent;
    private Bundle bundle;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    /**
     * Use private {@link SharedPreferences} object to save preferences that are private to this activity.
     */
    public Preferences(AppCompatActivity activity)
    {
        this.preferences = activity.getPreferences(Context.MODE_PRIVATE);
    }

    /**
     * Use Bundle instance to save instance state.
     */
    public Preferences(Bundle bundle)
    {
        this.bundle = bundle;
    }

    /**
     * Use to restored instance state.
     */
    public Preferences(AppCompatActivity activity, Bundle bundle)
    {
        this.bundle = bundle;
        if (bundle == null) preferences = activity.getPreferences(Context.MODE_PRIVATE);
    }

    public int getInt(String key)
    {
        return getInt(key, 0);
    }

    private int getInt(String key, int defaultValue)
    {
        if (intent != null) return intent.getIntExtra(key, defaultValue);
        if (bundle != null) return bundle.getInt(key, defaultValue);
        if (preferences != null) return preferences.getInt(key, defaultValue);
        return defaultValue;
    }

    public long getLong(String key)
    {
        return getLong(key, 0);
    }

    private long getLong(String key, long defaultValue)
    {
        if (intent != null) return intent.getLongExtra(key, defaultValue);
        if (bundle != null) return bundle.getLong(key, defaultValue);
        if (preferences != null) return preferences.getLong(key, defaultValue);
        return defaultValue;
    }

    public <T> T getObject(String key, Class<T> clazz)
    {
        String s = getString(key, null);
        return s == null ? null : Json.fromJson(s, clazz);
    }

    public String getString(String key)
    {
        return getString(key, null);
    }

    private String getString(String key, String defaultValue)
    {
        if (intent != null) return intent.getStringExtra(key);
        if (bundle != null) return bundle.getString(key, defaultValue);
        if (preferences != null) return preferences.getString(key, defaultValue);
        return defaultValue;
    }

    private synchronized SharedPreferences.Editor getEditor()
    {
        if (editor == null) editor = preferences.edit();
        return editor;
    }

    public Preferences putBoolean(String key, boolean value)
    {
        if (bundle == null) getEditor().putBoolean(key, value);
        else bundle.putBoolean(key, value);
        return this;
    }

    public Preferences putInt(String key, int value)
    {
        if (bundle == null) getEditor().putInt(key, value);
        else bundle.putInt(key, value);
        return this;
    }

    public Preferences putLong(String key, long value)
    {
        if (bundle == null) getEditor().putLong(key, value);
        else bundle.putLong(key, value);
        return this;
    }

    public Preferences putObject(String key, Object value)
    {
        String json = Json.toJson(value);
        if (json != null)
        {
            if (bundle == null) getEditor().putString(key, json);
            else bundle.putString(key, json);
        }
        return this;
    }

    public Preferences putString(String key, String value)
    {
        if (bundle == null) getEditor().putString(key, value);
        else bundle.putString(key, value);
        return this;
    }

    public Preferences remove(String key)
    {
        if (bundle == null) getEditor().remove(key);
        else bundle.remove(key);
        return this;
    }

    public void commit()
    {
        if (editor != null) editor.apply();
    }
}