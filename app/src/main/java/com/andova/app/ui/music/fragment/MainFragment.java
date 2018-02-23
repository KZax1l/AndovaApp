package com.andova.app.ui.music.fragment;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andova.app.Constants;
import com.andova.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018-02-23.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MainFragment extends Fragment {
    private String mAction;

    public static MainFragment newInstance(String action) {
        MainFragment mainFragment = new MainFragment();
        Bundle bundle = new Bundle();
        switch (action) {
            case Constants.NAVIGATE_ALL_SONG:
                bundle.putString(Constants.PLAYLIST_TYPE, action);
                break;
            case Constants.NAVIGATE_PLAYLIST_RECENT_ADD:
                bundle.putString(Constants.PLAYLIST_TYPE, action);
                break;
            case Constants.NAVIGATE_PLAYLIST_RECENT_PLAY:
                bundle.putString(Constants.PLAYLIST_TYPE, action);
                break;
            case Constants.NAVIGATE_PLAYLIST_FAVORITE:
                bundle.putString(Constants.PLAYLIST_TYPE, action);
                break;
            default:
                throw new RuntimeException("wrong action type");
        }
        mainFragment.setArguments(bundle);
        return mainFragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mAction = getArguments().getString(Constants.PLAYLIST_TYPE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fr_module_music_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = view.findViewById(R.id.toolbar);

        switch (mAction) {
            case Constants.NAVIGATE_ALL_SONG:
                toolbar.setTitle(R.string.music_my_music);
                break;
            case Constants.NAVIGATE_PLAYLIST_RECENT_ADD:
                toolbar.setTitle(R.string.music_recent_add);
                break;
            case Constants.NAVIGATE_PLAYLIST_RECENT_PLAY:
                toolbar.setTitle(R.string.music_recent_play);
                break;
            case Constants.NAVIGATE_PLAYLIST_FAVORITE:
                toolbar.setTitle(R.string.music_favorite);
                break;
        }

        TabLayout tabLayout = view.findViewById(R.id.tab_page);
        ViewPager viewPager = view.findViewById(R.id.vp_container);
        tabLayout.setupWithViewPager(viewPager);

        if (viewPager != null) {
            setupViewPager(viewPager);
            viewPager.setOffscreenPageLimit(2);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getChildFragmentManager());
        adapter.addFragment(SongFragment.newInstance(mAction), this.getString(R.string.music_song));
        adapter.addFragment(ArtistFragment.newInstance(mAction), this.getString(R.string.music_artist));
        adapter.addFragment(AlbumFragment.newInstance(mAction), this.getString(R.string.music_album));
        viewPager.setAdapter(adapter);
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        Adapter(FragmentManager fm) {
            super(fm);
        }

        void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
