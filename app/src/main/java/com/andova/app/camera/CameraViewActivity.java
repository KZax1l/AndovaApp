package com.andova.app.camera;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.andova.app.R;
import com.google.android.cameraview.CameraView;

/**
 * Created by Administrator on 2018-07-20.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class CameraViewActivity extends AppCompatActivity {
    private CameraView mCameraView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_camera);
//        mCameraView = findViewById(R.id.camera_view);
//        mCameraView.setFacing(CameraView.FACING_FRONT);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mCameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mCameraView.stop();
    }
}
