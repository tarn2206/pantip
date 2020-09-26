package tarn.pantip.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.util.Utils;

/**
 * User: Tarn
 * Date: 10/11/13 12:33 PM
 */
public class RecommendActivity extends BaseActivity
{
    private String forum;
    private String tag;
    private RecommendFragment[] fragments = new RecommendFragment[2];

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        flatToolbar = true;
        setContentView(R.layout.activity_my);

        if (savedInstanceState == null)
        {
            forum = getIntent().getStringExtra("forum");
            tag = getIntent().getStringExtra("tag");
        }
        else
        {
            forum = savedInstanceState.getString("forum");
            tag = savedInstanceState.getString("tag");
        }

        if (tag != null) setTitle(Pantip.getDataStore().getTagLabel(tag));
        else setTitle(Pantip.getDataStore().getForumLabel(forum));

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
                RecommendFragment fragment = fragments[tab.getPosition()];
                if (fragment != null) fragment.smoothScrollToTop();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("forum", forum);
        outState.putString("tag", tag);
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
        PagerAdapter()
        {
            super(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount()
        {
            return tag == null ? 2 : 1;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0: return "กระทู้แนะนำโดยสมาชิก";
                case 1: return "Pantip Trend";
            }
            return null;
        }

        @NonNull
        @Override
        public RecommendFragment getItem(int position)
        {
            return fragments[position] = RecommendFragment.newInstance(forum, tag, position);
        }
    }
}