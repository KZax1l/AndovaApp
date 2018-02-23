package com.andova.app.ui.music;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.andova.app.R;
import com.andova.app.ui.BaseActivity;

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
    }
}
