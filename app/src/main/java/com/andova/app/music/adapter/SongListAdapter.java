package com.andova.app.music.adapter;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andova.app.R;
import com.andova.app.music.model.Song;
import com.andova.app.music.player.MusicTracker;
import com.andova.app.util.DensityUtil;
import com.andova.app.util.MusicUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018-02-23.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class SongListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public int currentlyPlayingPosition;
    private List<Song> mSongList;
    private AppCompatActivity mContext;
    private long[] songIDs;
    private boolean withHeader;
    private float topPlayScore;
    private String action;

    public SongListAdapter(AppCompatActivity context, List<Song> songList, String action, boolean withHeader) {
        if (songList == null) {
            this.mSongList = new ArrayList<>();
        } else {
            this.mSongList = songList;
        }
        this.mContext = context;
        this.songIDs = getSongIds();
        this.withHeader = withHeader;
        this.action = action;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && withHeader) {
            return Type.TYPE_PLAY_SHUFFLE;
        } else {
            return Type.TYPE_SONG;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case Type.TYPE_PLAY_SHUFFLE:
                View playShuffle = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_module_music_play_shuffle, viewGroup, false);
                viewHolder = new PlayShuffleViewHolder(playShuffle);
                break;
            case Type.TYPE_SONG:
                View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_module_music_song, viewGroup, false);
                viewHolder = new ItemHolder(v);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case Type.TYPE_PLAY_SHUFFLE:
                break;
            case Type.TYPE_SONG:
                ItemHolder itemHolder = (ItemHolder) holder;
                Song localItem;
                localItem = withHeader ? mSongList.get(position - 1) : mSongList.get(position);

                itemHolder.title.setText(localItem.title);
                itemHolder.artist.setText(localItem.artistName);
                itemHolder.album.setText(localItem.albumName);

                Glide.with(holder.itemView.getContext()).load(MusicUtil.getAlbumArtUri(localItem.albumId).toString())
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .centerCrop()
                                .error(R.mipmap.ic_album_default)
                                .placeholder(R.mipmap.ic_album_default))
                        .into(itemHolder.albumArt);

                if (topPlayScore != 0) {
                    itemHolder.playscore.setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) itemHolder.playscore.getLayoutParams();
                    int full = DensityUtil.getScreenWidth(mContext);
                    layoutParams.width = (int) (full * (localItem.getPlayCountScore() / topPlayScore));
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        if (withHeader && mSongList.size() != 0) {
            return (null != mSongList ? mSongList.size() + 1 : 0);
        } else {
            return (null != mSongList ? mSongList.size() : 0);
        }
    }

    public long[] getSongIds() {
        int songNum = mSongList.size();
        long[] ret = new long[songNum];
        for (int i = 0; i < songNum; i++) {
            ret[i] = mSongList.get(i).id;
        }

        return ret;
    }

    public void setSongList(List<Song> list) {
        this.mSongList = list;
        this.songIDs = getSongIds();
        if (list.size() != 0) {
            this.topPlayScore = list.get(0).getPlayCountScore();
        }
        notifyDataSetChanged();
    }

    public static class Type {
        /**
         * 随机播放项
         */
        static final int TYPE_PLAY_SHUFFLE = 0;
        /**
         * 歌曲项
         */
        static final int TYPE_SONG = 1;
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView title;
        private TextView artist;
        private TextView album;
        private ImageView albumArt;
        private ImageView popupMenu;
        private View playscore;

        public ItemHolder(View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.text_item_title);
            this.artist = (TextView) view.findViewById(R.id.text_item_subtitle);
            this.album = (TextView) view.findViewById(R.id.text_item_subtitle_2);
            this.albumArt = (ImageView) view.findViewById(R.id.image);
            this.popupMenu = (ImageView) view.findViewById(R.id.popup_menu);
            this.playscore = view.findViewById(R.id.playscore);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MusicTracker.playAll(songIDs, getAdapterPosition() - 1, -1, false);
                }
            }, 100);
        }
    }

    public class PlayShuffleViewHolder extends RecyclerView.ViewHolder {
        PlayShuffleViewHolder(View view) {
            super(view);
        }
    }
}


