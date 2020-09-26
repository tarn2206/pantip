package tarn.pantip.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.io.FileUtils;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.SearchSuggestionProvider;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 14 September 2016
 */
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        SharedPreferences preferences = Pantip.getSharedPreferences();
        Preference account = findPreference("account");
        if (account != null)
        {
            account.setOnPreferenceClickListener(p -> {
                p.setEnabled(false);
                if (Pantip.loggedOn) logOut(p);
                else startActivityForResult(new Intent(getActivity(), LoginActivity.class), MainActivity.RC_LOGIN);
                return true;
            });
            updatePreferenceAccount(account);
        }

        ListPreference listPreference = (ListPreference)findPreference("font_size");
        if (listPreference != null)
        {
            listPreference.setOnPreferenceChangeListener(this);
            String fontSize = preferences.getString("font_size", "16");
            setSummary(listPreference, fontSize);
        }

        listPreference = (ListPreference)findPreference("night_mode2");
        if (listPreference != null)
        {
            listPreference.setOnPreferenceChangeListener(this);
            String nightMode = preferences.getString("night_mode2", "2");
            setSummary(listPreference, nightMode);
        }

        Preference preference = findPreference("clear_search");
        if (preference != null)
        {
            preference.setOnPreferenceClickListener(p -> {
                Utils.createDialog(getActivity())
                        .setTitle(p.getTitle())
                        .setMessage(p.getSummary() + "?")
                        .setPositiveButton("ตกลง", new ClearSearchListener(p))
                        .setNegativeButton("ยกเลิก", null)
                        .setCancelable(true)
                        .show();
                return true;
            });
        }

        final Preference clearCache = findPreference("clear_cache");
        calcCacheSize(clearCache);

        preference = findPreference("open_source_licenses");
        if (preference != null)
        {
            preference.setOnPreferenceClickListener(p -> {
                Utils.showOpenSourceLicense(getActivity());
                return true;
            });
        }

        preference = findPreference("app_version");
        if (preference != null)
        {
            preference.setTitle("เวอร์ชั่น " + SettingsActivity.version());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        RecyclerView listView = getListView();
        if (listView != null) listView.setVerticalScrollBarEnabled(false);
    }

    private void setSummary(ListPreference preference, String value)
    {
        int i = preference.findIndexOfValue(value);
        preference.setSummary(i == -1 ? value : preference.getEntries()[i]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        if (preference instanceof ListPreference)
        {
            setSummary((ListPreference)preference, (String)newValue);
            if (preference.getKey().equals("night_mode2"))
            {
                Pantip.nightMode = Integer.parseInt((String)newValue);
                Pantip.initTheme(false);
                if (getActivity() != null) getActivity().recreate();
            }
        }
        return true;
    }

    private void calcCacheSize(final Preference preference)
    {
        RxUtils.observe(() -> {
            Context context = preference.getContext();
            return Utils.getDirectorySize(context.getCacheDir())
                    + Utils.getDirectorySize(context.getExternalCacheDir())
                    + Utils.getDirectorySize(Utils.getFileDir(context));
        }).subscribe(size -> {
            if (size == 0)
            {
                preference.setSummary("ไม่มีข้อมูลในแคช");
                preference.setEnabled(false);
                return;
            }

            preference.setSummary("ลบข้อมูลในแคช " + SettingsActivity.formatSize(size));
            preference.setEnabled(true);
            preference.setOnPreferenceClickListener(p -> {
                Context context = p.getContext();
                if (context instanceof ContextThemeWrapper)
                {
                    context = ((ContextThemeWrapper)context).getBaseContext();
                }
                if (context instanceof AppCompatActivity)
                {
                    Utils.createDialog((AppCompatActivity)context)
                            .setTitle(p.getTitle())
                            .setMessage(p.getSummary() + "?")
                            .setPositiveButton("ตกลง", (dialog, which) -> clearDiskCache(p))
                            .setNegativeButton("ยกเลิก", null)
                            .setCancelable(true)
                            .show();
                }
                else clearDiskCache(p);
                return true;
            });
        }, L::e);
    }

    private void clearDiskCache(final Preference preference)
    {
        RxUtils.observe(emitter -> {
            Context context = preference.getContext();
            FileUtils.deleteQuietly(context.getCacheDir());
            FileUtils.deleteQuietly(context.getExternalCacheDir());
            FileUtils.deleteQuietly(Utils.getFileDir(context));
            GlideApp.get(context).clearDiskCache();
            GlideApp.get(preference.getContext()).clearMemory();
            emitter.onComplete();
        }).subscribe(empty -> {
            preference.setSummary("ลบข้อมูลในแคช");
            Utils.showToast(preference.getContext(), "ลบข้อมูลในแคชเรียบร้อยแล้ว!");
        }, tr -> {
            preference.setEnabled(true);
            Pantip.handleException(preference.getContext(), tr);
        });
    }

    private static class ClearSearchListener implements DialogInterface.OnClickListener
    {
        private final Preference preference;

        ClearSearchListener(Preference preference)
        {
            this.preference = preference;
        }

        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            SearchSuggestionProvider.clearHistory(preference.getContext());
            preference.setEnabled(false);
        }
    }

    // PreferenceCategory bugs, remove in future support library
    @Override
    @SuppressLint("RestrictedApi")
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen)
    {
        return new PreferenceGroupAdapter(preferenceScreen)
        {
            @Override
            public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position)
            {
                super.onBindViewHolder(holder, position);
                Preference preference = getItem(position);
                if (preference instanceof PreferenceCategory && holder.itemView instanceof ViewGroup)
                {
                    ViewGroup group = (ViewGroup)holder.itemView;
                    for (int i = 0; i < group.getChildCount(); i++)
                    {
                        View view = group.getChildAt(i);
                        view.setPadding(0, 0, 0, 0);
                    }
                }
            }
        };
    }

    private void updatePreferenceAccount(Preference preference)
    {
        preference.setEnabled(true);
        if (Pantip.loggedOn)
        {
            preference.setTitle(Pantip.currentUser.name);
            preference.setSummary("คลิกเพื่อออกจากระบบ");
        }
        else
        {
            preference.setTitle("Login");
            preference.setSummary("คลิกเพื่อเข้าสู่ระบบ");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.RC_LOGIN && resultCode == Activity.RESULT_OK)
        {
            updatePreferenceAccount(findPreference("account"));
        }
    }

    private void logOut(final Preference preference)
    {
        LogoutDialog.show(getActivity(), new LogoutDialog.LogoutCallback()
        {
            @Override
            public void cancel()
            {
                preference.setEnabled(true);
            }

            @Override
            public void complete()
            {
                updatePreferenceAccount(preference);
            }

            @Override
            public void error(Throwable tr)
            {
                updatePreferenceAccount(preference);
                Pantip.handleException(getActivity(), tr);
            }
        });
    }}