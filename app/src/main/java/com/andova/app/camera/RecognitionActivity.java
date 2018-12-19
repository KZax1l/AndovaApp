package com.andova.app.camera;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.andova.app.BaseActivity;
import com.andova.app.R;
import com.andova.face.DetectorData;
import com.andova.face.DetectorProxy;
import com.andova.face.FaceRectView;
import com.andova.face.ICameraCheckListener;
import com.andova.face.IDataListener;
import com.andova.face.SystemFaceDetector;
import com.andova.face.preview.CameraUndefinePreview;
import com.google.android.cameraview.CameraView;

/**
 * Created by Administrator on 2018-03-12.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class RecognitionActivity extends BaseActivity {
    //    private CameraPreview mCameraPreview;
//    private CameraView mCameraView;
    private DetectorProxy mDetectorProxy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_module_camera);
//        mCameraPreview = findViewById(R.id.camera_preview);
//        mCameraView = findViewById(R.id.camera_view);
        initFaceDetect();
//        new AlertDialog.Builder(this).setMessage("message").setTitle("title").create().show();
    }

    private void initFaceDetect() {
        IDataListener mDataListener = new IDataListener() {
            @Override
            public void onDetectorData(final DetectorData detectorData) {
                if (detectorData.getFaceRectList().length <= 0) return;
//                Bitmap bitmap = ImageUtil.decodeToBitmap(detectorData.getFaceData(), mCameraPreview.getCamera());
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
        FaceRectView faceRectView = (FaceRectView) findViewById(R.id.face_rect_view);
        faceRectView.setZOrderOnTop(true);
        faceRectView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        //创建代理类，必须传入相机预览界面
        mDetectorProxy = new DetectorProxy.Builder((CameraUndefinePreview) findViewById(R.id.camera_preview))
                //设置人脸检测实现
                .setFaceDetector(new SystemFaceDetector())
                .setMinCameraPixels(2000000)
                //设置检测数据回调监听
                .setDataListener(mDataListener)
                //设置权限检查监听
                .setCheckListener(mCameraCheckListener)
                //设置绘制人脸识别框界面
                .setFaceRectView(faceRectView)
                //设置是否绘制人脸检测框
                .setDrawFaceRect(true)
                //设置预览相机的相机ID
                .setCameraId(CameraView.FACING_FRONT)
                //设置可检测的最大人脸数
                .setMaxFacesCount(5)
                //设置人脸识别框是否为完整矩形
                .setFaceIsRect(false)
                //设置人脸识别框的RGB颜色
                .setFaceRectColor(Color.rgb(255, 203, 15))
                //创建代理类
                .build();
        if (mDetectorProxy != null) mDetectorProxy.detector();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (mDetectorProxy != null) {
//            mDetectorProxy.detector();
////            mDetectorProxy.openCamera();
//        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mDetectorProxy != null) mDetectorProxy.release();
    }
}
