package com.andova.face;

import android.graphics.Color;

import com.andova.face.detector.IPreview;
import com.google.android.cameraview.CameraView;

import static com.google.android.cameraview.CameraView.FACING_BACK;
import static com.google.android.cameraview.Constants.FACING_FRONT;

/**
 * Created by Administrator on 2018-03-09.
 * <p>识别代理类</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class DetectorProxy {
    private IPreview mCameraPreview;
    private FaceRectView mFaceRectView;
    private IFaceDetector mFaceDetector;
    private boolean mDrawFaceRect;

    /**
     * 构造函数，需传入自定义相机预览界面
     *
     * @param cameraPreview 相机预览界面
     */
    private DetectorProxy(IPreview cameraPreview) {
        this.mCameraPreview = cameraPreview;
    }

    /**
     * 设置绘制人脸检测框界面
     */
    public void setFaceRectView(FaceRectView faceRectView) {
        this.mFaceRectView = faceRectView;
    }

    /**
     * 设置人脸检测类，默认实现为原生检测类，可以替换成第三方库检测类
     *
     * @param faceDetector 人脸检测类
     */
    public void setFaceDetector(IFaceDetector faceDetector) {
        if (faceDetector != null) {
            this.mFaceDetector = faceDetector;
        }
        if (mCameraPreview != null) {
            mCameraPreview.setFaceDetector(this.mFaceDetector);
        }
    }

    /**
     * 设置相机检查监听
     */
    public void setCheckListener(ICameraCheckListener checkListener) {
        if (mCameraPreview != null) {
            mCameraPreview.setCheckListener(checkListener);
        }
    }

    /**
     * 设置检测监听
     */
    public void setDataListener(final IDataListener dataListener) {
        if (mFaceDetector != null) {
            mFaceDetector.setDataListener(new IDataListener() {
                @Override
                public void onDetectorData(DetectorData detectorData) {
                    if (mDrawFaceRect && mFaceRectView != null && detectorData != null
                            && detectorData.getFaceRectList() != null) {
                        mFaceRectView.drawFaceRect(detectorData);
                    }
                    if (dataListener != null) {
                        dataListener.onDetectorData(detectorData);
                    }
                }
            });
        }
    }

    /**
     * 设置相机预览为前置还是后置摄像头
     */
    public void setCameraId(@CameraView.Facing int cameraId) {
        if (cameraId == FACING_BACK || cameraId == FACING_FRONT) {
            if (mCameraPreview != null) {
                mCameraPreview.setCameraId(cameraId);
            }
        }
    }

    /**
     * 设置像素最低要求
     */
    public void setMinCameraPixels(long minCameraPixels) {
        if (mCameraPreview != null) {
            mCameraPreview.setMinCameraPixels(minCameraPixels);
        }
    }

    /**
     * 设置检测最大人脸数量
     */
    public void setMaxFacesCount(int maxFacesCount) {
        if (mFaceDetector != null) {
            mFaceDetector.setMaxFacesCount(maxFacesCount);
        }
    }

    /**
     * 设置是否绘制人脸检测框
     */
    public void setDrawFaceRect(boolean drawFaceRect) {
        this.mDrawFaceRect = drawFaceRect;
    }

    /**
     * 设置人脸检测框是否是矩形
     */
    public void setFaceIsRect(boolean faceIsRect) {
        if (mFaceRectView != null) {
            mFaceRectView.setFaceIsRect(faceIsRect);
        }
    }

    /**
     * 设置人脸检测框颜色
     */
    public void setFaceRectColor(int rectColor) {
        if (mFaceRectView != null) {
            mFaceRectView.setRectColor(rectColor);
        }
    }

    /**
     * 开启检测
     */
    public void detector() {
        if (mFaceDetector != null) {
            mFaceDetector.detector();
        }
    }

    /**
     * 打开相机
     */
    public void openCamera() {
        if (mCameraPreview != null) {
            mCameraPreview.openCamera();
        }
    }

    /**
     * 关闭相机
     */
    public void closeCamera() {
        if (mCameraPreview != null) {
            mCameraPreview.closeCamera();
        }
    }

    /**
     * 获取相机ID
     */
    @CameraView.Facing
    public int getCameraId() {
        if (mCameraPreview != null) {
            return mCameraPreview.getCameraId();
        }
        return FACING_BACK;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mCameraPreview != null) {
            mCameraPreview.release();
        }
    }

    public static class Builder {
        private static final int MIN_CAMERA_PIXELS = 5000000;
        private static final int MAX_DETECTOR_FACES = 5;

        private IPreview mCameraPreview;
        private FaceRectView mFaceRectView;
        private ICameraCheckListener mCheckListener;
        private IDataListener mDataListener;
        private IFaceDetector mFaceDetector = new SystemFaceDetector();
        private int mCameraId = FACING_BACK;
        private long mMinCameraPixels = MIN_CAMERA_PIXELS;
        private int mMaxFacesCount = MAX_DETECTOR_FACES;
        private int mFaceRectColor = Color.rgb(255, 203, 15);
        private boolean mDrawFaceRect = false;
        private boolean mFaceIsRect = false;

        public Builder(IPreview cameraPreview) {
            this.mCameraPreview = cameraPreview;
        }

        public Builder setFaceRectView(FaceRectView faceRectView) {
            this.mFaceRectView = faceRectView;
            return this;
        }

        public Builder setFaceDetector(IFaceDetector faceDetector) {
            this.mFaceDetector = faceDetector;
            return this;
        }

        public Builder setCameraId(@CameraView.Facing int cameraId) {
            this.mCameraId = cameraId;
            return this;
        }

        public Builder setMinCameraPixels(long minCameraPixels) {
            this.mMinCameraPixels = minCameraPixels;
            return this;
        }

        public Builder setCheckListener(ICameraCheckListener checkListener) {
            this.mCheckListener = checkListener;
            return this;
        }

        public Builder setDataListener(IDataListener dataListener) {
            this.mDataListener = dataListener;
            return this;
        }

        public Builder setMaxFacesCount(int maxFacesCount) {
            this.mMaxFacesCount = maxFacesCount;
            return this;
        }

        public Builder setDrawFaceRect(boolean drawFaceRect) {
            this.mDrawFaceRect = drawFaceRect;
            return this;
        }

        public Builder setFaceIsRect(boolean faceIsRect) {
            this.mFaceIsRect = faceIsRect;
            return this;
        }

        public Builder setFaceRectColor(int faceRectColor) {
            this.mFaceRectColor = faceRectColor;
            return this;
        }

        public DetectorProxy build() {
            DetectorProxy detectorProxy = new DetectorProxy(mCameraPreview);
            detectorProxy.setFaceDetector(mFaceDetector);
            detectorProxy.setCheckListener(mCheckListener);
            detectorProxy.setDataListener(mDataListener);
            detectorProxy.setMaxFacesCount(mMaxFacesCount);
            detectorProxy.setMinCameraPixels(mMinCameraPixels);
            if (mFaceRectView != null && mDrawFaceRect) {
                detectorProxy.setFaceRectView(mFaceRectView);
                detectorProxy.setDrawFaceRect(mDrawFaceRect);
                detectorProxy.setFaceRectColor(mFaceRectColor);
                detectorProxy.setFaceIsRect(mFaceIsRect);
            }
            detectorProxy.setCameraId(mCameraId);
            return detectorProxy;
        }
    }
}
