package com.andova.face.detector;

import com.andova.face.ICameraCheckListener;
import com.andova.face.IFaceDetector;
import com.google.android.cameraview.CameraView;

/**
 * Created by Administrator on 2018-07-19.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public interface IPreview {
    /**
     * 打开相机
     */
    void openCamera();

    /**
     * 设置人脸检测类，默认实现为原生检测类，可以替换成第三方库检测类
     */
    IPreview setFaceDetector(IFaceDetector faceDetector);

    /**
     * 设置相机检查监听
     */
    IPreview setCheckListener(ICameraCheckListener checkListener);

    /**
     * 设置相机预览为前置还是后置摄像头
     */
    IPreview setCameraId(@CameraView.Facing int cameraId);

    /**
     * 设置像素最低要求
     */
    IPreview setMinCameraPixels(long minCameraPixels);

    /**
     * 关闭相机
     */
    void closeCamera();

    /**
     * 获取相机ID
     */
    @CameraView.Facing
    int getCameraId();

    /**
     * 释放资源
     */
    void release();
}
