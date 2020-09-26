package tarn.pantip.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.tabs.TabLayout;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Preferences;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Utils;

/**
 * User: Tarn
 * Date: 9/14/13 6:46 PM
 */
public class MyTopicActivity extends BaseActivity
{
    private static final String KEY_CURRENT = "My:Current";
    private ViewPager viewPager;
    private final MyTopicFragment[] fragments = new MyTopicFragment[4];

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        flatToolbar = true;
        setContentView(R.layout.activity_my);

        addAvatar(this, toolbar);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new PagerAdapter());
        viewPager.setPageMargin(Utils.toPixels(10));
        viewPager.setPageMarginDrawable(Pantip.pageMarginDrawable);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
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
                MyTopicFragment fragment = fragments[tab.getPosition()];
                if (fragment != null) fragment.smoothScrollToTop();
            }
        });

        Preferences preferences = new Preferences(this, savedInstanceState);
        int item = preferences.getInt(KEY_CURRENT);
        viewPager.setCurrentItem(item);
    }

    static void addAvatar(BaseActivity context, Toolbar toolbar)
    {
        ImageView avatar = new ImageView(context);
        toolbar.addView(avatar);
        Toolbar.LayoutParams params = (Toolbar.LayoutParams)avatar.getLayoutParams();
        params.width = params.height = Utils.getDimension(R.dimen.avatar_size);
        params.setMarginEnd(Utils.toPixels(8));
        params.gravity = Gravity.END;
        GlideApp.with(context).load(Pantip.currentUser.avatar)
                .transform(new CircleCrop()).into(avatar);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        getPreferences().edit().putInt(KEY_CURRENT, viewPager.getCurrentItem()).apply();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT, viewPager.getCurrentItem());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : getSupportFragmentManager().getFragments())
        {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    class PagerAdapter extends FragmentPagerAdapter
    {
        private final String[] titles = new String[] { "กระทู้ที่ฉันเคยอ่าน", "กระทู้โปรดของฉัน", "กระทู้ที่ฉันตั้ง", "กระทู้ที่ฉันตอบ" };

        PagerAdapter()
        {
            super(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount()
        {
            return fragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return titles[position];
        }

        @NonNull
        @Override
        public MyTopicFragment getItem(int position)
        {
            int index;
            switch (position)
            {
                case 1:
                    index = MyTopicFragment.FAVORITES_TOPIC;
                    break;
                case 2:
                    index = MyTopicFragment.POST_TOPIC;
                    break;
                case 3:
                    index = MyTopicFragment.REPLY_TOPIC;
                    break;
                default:
                    index = MyTopicFragment.READ_TOPIC;
                    break;
            }

            return fragments[position] = MyTopicFragment.newInstance(index);
        }
    }
}