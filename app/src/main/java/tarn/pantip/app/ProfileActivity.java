package tarn.pantip.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.tabs.TabLayout;

import org.apache.commons.lang3.StringUtils;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Utils;

public class ProfileActivity extends BaseActivity
{
    private final ProfileFragment[] fragments = new ProfileFragment[3];
    private int mid;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        flatToolbar = true;
        setContentView(R.layout.activity_profile);

        Intent intent = getIntent();
        mid = intent.getIntExtra("mid", 0);
        setTitle(intent.getStringExtra("name"));
        setAvatar(intent.getStringExtra("avatar"));

        ViewPager viewPager = findViewById(R.id.viewPager);
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
                ProfileFragment fragment = fragments[tab.getPosition()];
                if (fragment != null) fragment.smoothScrollToTop();
            }
        });
    }

    private void setAvatar(String url)
    {
        if (StringUtils.isBlank(url)) return;

        ImageView avatar = new ImageView(this);
        toolbar.addView(avatar);
        Toolbar.LayoutParams params = (Toolbar.LayoutParams)avatar.getLayoutParams();
        params.width = params.height = Utils.toPixels(44);
        params.setMarginEnd(Utils.toPixels(8));
        params.gravity = Gravity.END;
        GlideApp.with(this).load(url).transform(new CircleCrop()).into(avatar);
    }

    class PagerAdapter extends FragmentPagerAdapter
    {
        private final String[] titles = new String[] { "กระทู้ที่ตั้ง", "กระทู้ที่ตอบ", "กระทู้โปรด" };

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
        public ProfileFragment getItem(int position)
        {
            int index;
            switch (position)
            {
                case 1:
                    index = ProfileFragment.REPLY_TOPIC;
                    break;
                case 2:
                    index = ProfileFragment.FAVORITES_TOPIC;
                    break;
                default:
                    index = ProfileFragment.POST_TOPIC;
                    break;
            }
            return fragments[position] = ProfileFragment.newInstance(mid, index);
        }
    }
}
