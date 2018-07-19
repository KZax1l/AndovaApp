package com.andova.face.detector;

import com.andova.face.ICameraCheckListener;
import com.andova.face.IFaceDetector;

/**
 * Created by Administrator on 2018-07-19.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public interface ICameraPreview<CAMERA> {
    /**
     * 打开相机
     */
    void openCamera();

    /**
     * 设置人脸检测类，默认实现为原生检测类，可以替换成第三方库检测类
     */
    ICameraPreview setFaceDetector(IFaceDetector mFaceDetector);

    /**
     * 设置相机检查监听
     */
    ICameraPreview setCheckListener(ICameraCheckListener mCheckListener);

    /**
     * 设置相机预览为前置还是后置摄像头
     */
    ICameraPreview setCameraId(int cameraId);

    /**
     * 设置像素最低要求
     */
    ICameraPreview setMinCameraPixels(long minCameraPixels);

    /**
     * 关闭相机
     */
    void closeCamera();

    /**
     * 获取相机ID
     */
    int getCameraId();

    /**
     * 释放资源
     */
    void release();

    /**
     * 获取相机对象
     */
    CAMERA getCamera();
}
