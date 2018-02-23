package com.andova.app.ui.music.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andova.app.Constants;

/**
 * Created by Administrator on 2018-02-23.
 * <p>专辑页面</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class AlbumFragment extends Fragment {
    private String mAction;

    public static AlbumFragment newInstance(String action) {
        Bundle args = new Bundle();
        switch (action) {
            case Constants.NAVIGATE_ALL_SONG:
                args.putString(Constants.PLAYLIST_TYPE, action);
                break;
            case Constants.NAVIGATE_PLAYLIST_RECENT_ADD:
                args.putString(Constants.PLAYLIST_TYPE, action);
                break;
            case Constants.NAVIGATE_PLAYLIST_RECENT_PLAY:
                args.putString(Constants.PLAYLIST_TYPE, action);
                break;
            case Constants.NAVIGATE_PLAYLIST_FAVORITE:
                args.putString(Constants.PLAYLIST_TYPE, action);
                break;
            default:
                throw new RuntimeException("wrong action type");
        }
        AlbumFragment fragment = new AlbumFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
