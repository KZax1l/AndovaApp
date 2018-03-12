package com.andova.app.camera;

import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.andova.app.BaseActivity;
import com.andova.app.R;
import com.andova.fdt.camera.CameraPreview;
import com.andova.fdt.camera.DetectorData;
import com.andova.fdt.camera.DetectorProxy;
import com.andova.fdt.camera.FaceRectView;
import com.andova.fdt.camera.ICameraCheckListener;
import com.andova.fdt.camera.IDataListener;
import com.andova.fdt.camera.NormalFaceDetector;

/**
 * Created by Administrator on 2018-03-12.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class RecognitionActivity extends BaseActivity {
    private DetectorProxy mDetectorProxy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_module_camera);
        initFaceDetect();
        if (mDetectorProxy != null) mDetectorProxy.detector();
    }

    private void initFaceDetect() {
        IDataListener mDataListener = new IDataListener() {
            @Override
            public void onDetectorData(final DetectorData detectorData) {
                if (detectorData.getFaceRectList().length <= 0) return;
                System.out.println("线程：" + Thread.currentThread().getName() + "，识别数据:" + detectorData);
            }
        };
        ICameraCheckListener mCameraCheckListener = new ICameraCheckListener() {
            @Override
            public void checkPermission(boolean isAllow) {
                //权限是否允许
                System.out.println("checkPermission:" + isAllow);
            }

            @Override
            public void checkPixels(long pixels, boolean isSupport) {
                //手机像素是否满足要求
                System.out.println("checkPixels:" + pixels);
            }
        };
        //创建代理类，必须传入相机预览界面
        mDetectorProxy = new DetectorProxy.Builder((CameraPreview) findViewById(R.id.camera_preview))
                //设置人脸检测实现
                .setFaceDetector(new NormalFaceDetector())
                //设置检测数据回调监听
                .setDataListener(mDataListener)
                //设置权限检查监听
                .setCheckListener(mCameraCheckListener)
                //设置绘制人脸识别框界面
                .setFaceRectView((FaceRectView) findViewById(R.id.face_rect_view))
                //设置是否绘制人脸检测框
                .setDrawFaceRect(true)
                //设置预览相机的相机ID
                .setCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT)
                //设置可检测的最大人脸数
                .setMaxFacesCount(5)
                //设置人脸识别框是否为完整矩形
                .setFaceIsRect(true)
                //设置人脸识别框的RGB颜色
                .setFaceRectColor(Color.rgb(255, 203, 15))
                //创建代理类
                .build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDetectorProxy != null) mDetectorProxy.release();
    }
}
