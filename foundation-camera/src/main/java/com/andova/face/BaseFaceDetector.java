package com.andova.face;

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

    protected DetectorData mDetectorData;
    protected CameraProvider mCamera;
    protected int mCameraId;
    protected float mZoomRatio;//缩放比例
    protected int mCameraWidth;
    protected int mCameraHeight;
    protected int mPreviewWidth;
    protected int mPreviewHeight;
    protected int mOrientationOfCamera;
    protected int mMaxFacesCount;
    protected boolean mOpenCamera = false;

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
            detectionFaces();

            if (mDataListener == null || mDetectorData.getFaceRectList() == null
                    || mDetectorData.getFaceRectList().length == 0) continue;
            mDataListener.onDetectorData(mDetectorData);
        }
    }

    protected abstract void detectionFaces();

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

    /**
     * 设置相机高度
     *
     * @param cameraHeight 相机高度
     */
    @Override
    public void setCameraHeight(int cameraHeight) {
        this.mCameraHeight = cameraHeight;
    }

    /**
     * 设置相机宽度
     */
    @Override
    public void setCameraWidth(int cameraWidth) {
        this.mCameraWidth = cameraWidth;
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

    /**
     * 设置缩放比例
     */
    @Override
    public void setZoomRatio(float zoomRatio) {
        this.mZoomRatio = zoomRatio;
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

    /**
     * 设置预览高度
     */
    @Override
    public void setPreviewHeight(int previewHeight) {
        this.mPreviewHeight = previewHeight;
    }

    /**
     * 设置预览宽度
     */
    @Override
    public void setPreviewWidth(int previewWidth) {
        this.mPreviewWidth = previewWidth;
    }
}
