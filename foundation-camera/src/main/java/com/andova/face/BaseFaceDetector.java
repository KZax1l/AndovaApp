package com.andova.face;

import android.support.annotation.NonNull;

import com.andova.face.detector.CameraProvider;

/**
 * Created by Administrator on 2018-03-09.
 * <p>识别抽象类</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public abstract class BaseFaceDetector implements IFaceDetector, Runnable {
    private Thread mThread;
    private boolean mStopTrack;
    private IDataListener mDataListener;

    private DetectorData mDetectorData;
    private CameraProvider mCamera;
    private int mCameraId;
    private float mZoomRatio;//缩放比例
    private int mCameraWidth;
    private int mCameraHeight;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mOrientationOfCamera;
    private int mMaxFacesCount;
    private boolean mOpenCamera = false;

    public BaseFaceDetector() {
        mDetectorData = new DetectorData();
    }

    @Override
    public void run() {
        mStopTrack = false;
        while (!mStopTrack) {
            if (!mOpenCamera) {
                continue;
            }
            if (mCamera == null || mDetectorData.getFaceData() == null || mDetectorData.getFaceData().length == 0) {
                continue;
            }
            detectionFaces(mDetectorData.getFaceData(), mCamera);

            if (mDataListener == null || mDetectorData.getFaceRectList() == null
                    || mDetectorData.getFaceRectList().length == 0) continue;
            mDataListener.onDetectorData(mDetectorData);
        }
    }

    // TODO: 2018-12-19 设置直接返回DetectorData对象
    protected abstract void detectionFaces(@NonNull byte[] data, @NonNull CameraProvider camera);

    /**
     * 开启识别
     */
    @Override
    public void detector() {
        mThread = new Thread(this);
        mThread.start();
    }

    /**
     * 释放资源
     */
    @Override
    public void release() {
        if (mDetectorData != null) {
            mDetectorData.setFaceData(null);
        }
        mStopTrack = true;
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    @NonNull
    public DetectorData getDetectorData() {
        return mDetectorData;
    }

    /**
     * 设置检测监听
     */
    @Override
    public void setDataListener(IDataListener dataListener) {
        this.mDataListener = dataListener;
    }

    /**
     * 设置预览数据
     */
    @Override
    public void setCameraPreviewData(byte[] data, CameraProvider camera) {
        if (mDetectorData != null) {
            mDetectorData.setFaceData(data);
        }
        mCamera = camera;
    }

    /**
     * 设置识别最大人脸数量
     */
    @Override
    public void setMaxFacesCount(int maxFacesCount) {
        this.mMaxFacesCount = maxFacesCount;
    }

    public int getMaxFacesCount() {
        return mMaxFacesCount;
    }

    /**
     * 设置相机高度
     *
     * @param cameraHeight 相机高度
     */
    @Override
    public void setCameraHeight(int cameraHeight) {
        this.mCameraHeight = cameraHeight;
    }

    public int getCameraHeight() {
        return mCameraHeight;
    }

    /**
     * 设置相机宽度
     */
    @Override
    public void setCameraWidth(int cameraWidth) {
        this.mCameraWidth = cameraWidth;
    }

    public int getCameraWidth() {
        return mCameraWidth;
    }

    /**
     * 设置相机方向
     * o
     *
     * @param orientationOfCamera 相机方向
     */
    @Override
    public void setOrientationOfCamera(int orientationOfCamera) {
        this.mOrientationOfCamera = orientationOfCamera;
    }

    public int getOrientationOfCamera() {
        return mOrientationOfCamera;
    }

    /**
     * 设置缩放比例
     */
    @Override
    public void setZoomRatio(float zoomRatio) {
        this.mZoomRatio = zoomRatio;
    }

    public float getZoomRatio() {
        return mZoomRatio;
    }

    /**
     * 设置相机是否打开
     */
    @Override
    public void setOpenCamera(boolean isOpenCamera) {
        this.mOpenCamera = isOpenCamera;
    }

    /**
     * 设置相机ID
     */
    @Override
    public void setCameraId(int cameraId) {
        this.mCameraId = cameraId;
    }

    public int getCameraId() {
        return mCameraId;
    }

    /**
     * 设置预览高度
     */
    @Override
    public void setPreviewHeight(int previewHeight) {
        this.mPreviewHeight = previewHeight;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    /**
     * 设置预览宽度
     */
    @Override
    public void setPreviewWidth(int previewWidth) {
        this.mPreviewWidth = previewWidth;
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }
}
