package com.andova.app.ui.music;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;

import com.andova.app.Constants;
import com.andova.app.R;
import com.andova.app.ui.BaseActivity;
import com.andova.app.ui.music.fragment.MainFragment;

/**
 * Created by Administrator on 2018-02-23.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MusicActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_module_music);

        pageToMain((NavigationView) findViewById(R.id.nav_list));
    }

    private void pageToMain(@NonNull NavigationView nav) {
        nav.getMenu().findItem(R.id.nav_music).setChecked(true);
        Fragment fragment = MainFragment.newInstance(Constants.NAVIGATE_ALL_SONG);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_container, fragment).commitAllowingStateLoss();
    }
}
