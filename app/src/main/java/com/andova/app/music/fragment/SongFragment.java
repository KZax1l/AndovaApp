package com.andova.app.music.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andova.app.Constants;
import com.andova.app.R;
import com.andova.app.music.adapter.SongListAdapter;
import com.andova.app.music.decoration.DividerDecoration;
import com.andova.app.music.loader.SongLoader;
import com.andova.app.music.model.Song;

import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2018-02-23.
 * <p>歌曲页面</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class SongFragment extends Fragment {
    private String mAction;
    private SongListAdapter mAdapter;

    private Subscription mSubscription;

    public static SongFragment newInstance(String action) {
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
        SongFragment fragment = new SongFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mAction = getArguments().getString(Constants.PLAYLIST_TYPE);
        }

        mAdapter = new SongListAdapter((AppCompatActivity) getActivity(), null, mAction, true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fr_module_music_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.rv_song);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerDecoration(getActivity(), LinearLayoutManager.VERTICAL, true));

        loadSongs();
    }

    private void loadSongs() {
        mSubscription = SongLoader.getAllSongs(getContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Song>>() {
                    @Override
                    public void call(List<Song> songList) {
                        if (songList == null || songList.size() == 0) return;
                        mAdapter.setSongList(songList);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSubscription != null) mSubscription.unsubscribe();
    }
}
