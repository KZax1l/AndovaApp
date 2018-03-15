package com.andova.app.music.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.andova.app.Constants;
import com.andova.app.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import static com.andova.app.music.MusicActivity.EXTRA_INTENT_MUSIC_COVER_URI;
import static com.andova.app.music.MusicActivity.RECEIVER_ACTION_UPDATE_MUSIC_COVER;

/**
 * Created by Administrator on 2018-02-23.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MainFragment extends Fragment {
    private String mAction;

    private ImageView ivCover;

    private BroadcastReceiver mUpdateBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.isEmpty(intent.getAction())
                    || !intent.getAction().equals(RECEIVER_ACTION_UPDATE_MUSIC_COVER))
                return;
            String uri = intent.getStringExtra(EXTRA_INTENT_MUSIC_COVER_URI);
            Glide.with(ivCover).applyDefaultRequestOptions(new RequestOptions().placeholder(R.mipmap.ic_album_default)).load(uri).into(ivCover);
        }
    };

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fr_module_music_main, container, false);
        ivCover = view.findViewById(R.id.iv_cover);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
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

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() == null) return;
        getContext().registerReceiver(mUpdateBroadcast, new IntentFilter(RECEIVER_ACTION_UPDATE_MUSIC_COVER));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() == null) return;
        getContext().unregisterReceiver(mUpdateBroadcast);
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
