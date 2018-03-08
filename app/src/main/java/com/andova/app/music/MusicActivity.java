package com.andova.app.music;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;

import com.andova.app.Constants;
import com.andova.app.R;
import com.andova.app.BaseActivity;
import com.andova.app.music.fragment.MainFragment;
import com.andova.app.music.player.MusicTracker;

import static com.andova.app.music.player.MusicTracker.sService;

/**
 * Created by Administrator on 2018-02-23.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MusicActivity extends BaseActivity implements ServiceConnection {
    private MusicTracker.ServiceToken mToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToken = MusicTracker.bindToService(this, this);
        setContentView(R.layout.ac_module_music);

        pageToMain((NavigationView) findViewById(R.id.nav_list));
    }

    private void pageToMain(@NonNull NavigationView nav) {
        nav.getMenu().findItem(R.id.nav_music).setChecked(true);
        Fragment fragment = MainFragment.newInstance(Constants.NAVIGATE_ALL_SONG);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_container, fragment).commitAllowingStateLoss();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        sService = IMusicServiceAPI.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        sService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mToken != null) {
            MusicTracker.unbindFromService(mToken);
            mToken = null;
        }
    }
}
