package com.andova.face;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;

import com.andova.face.detector.CameraProvider;
import com.andova.face.detector.IPreview;
import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.android.cameraview.CameraView.FACING_BACK;
import static com.google.android.cameraview.Constants.FACING_FRONT;

/**
 * Created by Administrator on 2018-07-19.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class CompatCameraPreview implements IPreview {
    private final String TAG = CompatCameraPreview.class.getSimpleName();
    @CameraView.Facing
    private int mCameraId;
    private long mMinCameraPixels;
    private CameraView mCamera;
    private IFaceDetector mFaceDetector;
    private ICameraCheckListener mCheckListener;
    private CameraProvider mCameraProvider = new CameraProvider();

    private int mCameraWidth;
    private int mCameraHeight;

    public CompatCameraPreview(@NonNull CameraView cameraView) {
        mCamera = cameraView;

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) cameraView.getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mCameraWidth = metrics.widthPixels;
        mCameraHeight = metrics.heightPixels;
    }

    @Override
    public void openCamera() {
        if (mCamera.isCameraOpened()) return;

        if (!FaceUtil.checkCameraPermission(mCamera.getContext())) {
            Log.i(TAG, "摄像头权限未打开，请打开后再试");
            if (mCheckListener != null) {
                mCheckListener.checkPermission(false);
            }
            return;
        }

        // 只有一个摄相头，打开后置
        if (mCamera.getNumberOfCameras() == 1) {
            mCameraId = FACING_BACK;
        }

        if (mFaceDetector != null) {
            mFaceDetector.setCameraId(mCameraId);
        }

        try {
            mCamera.setFacing(mCameraId);
            if (FACING_FRONT == mCameraId) {
                Log.i(TAG, "使用前置摄像头进行预览");
            } else {
                Log.i(TAG, "使用后置摄像头进行预览");
            }
            //开始预览
            mCamera.start();
        } catch (Exception e) {
            e.printStackTrace();

            //回调权限判定结果
            if (mCheckListener != null) {
                mCheckListener.checkPermission(false);
            }

            //关闭相机
            closeCamera();
            return;
        }

        //回调权限判定结果
        if (mCheckListener != null) {
            mCheckListener.checkPermission(true);
        }

        long pixels = 0;
        try {
            Size maxPictureSize = findMaxCameraSize(mCamera.getSupportedPictureSizes());
            if (maxPictureSize != null) {
                pixels = maxPictureSize.getWidth() * maxPictureSize.getHeight();
            }
            Log.i(TAG, "camera max support pixels: " + pixels);
            //回调该手机像素值
            if (mCheckListener != null && mMinCameraPixels > 0) {
                if (pixels >= mMinCameraPixels) {
                    mCheckListener.checkPixels(pixels, true);
                } else {
                    closeCamera();
                    mCheckListener.checkPixels(pixels, false);
                    return;
                }
            }

            //设置预览回调
            mCamera.addCallback(new CameraView.Callback() {
                @Override
                public void onPreviewFrame(CameraView cameraView, byte[] data) {
                    if (mFaceDetector != null) {
                        mCameraProvider.previewWidth = mCamera.getPreviewWidth();
                        mCameraProvider.previewHeight = mCamera.getPreviewHeight();
                        mFaceDetector.setCameraPreviewData(data, mCameraProvider);
                        mFaceDetector.setOpenCamera(true);
                        mFaceDetector.setPreviewWidth(mCamera.getPreviewWidth());
                        mFaceDetector.setPreviewHeight(mCamera.getPreviewHeight());
                    }
                }
            });
            Log.i(TAG, "camera size width:" + mCamera.getCameraWidth() + ",height:" + mCamera.getCameraHeight());
            if (mFaceDetector != null) {
                mFaceDetector.setCameraWidth(mCameraWidth);
                mFaceDetector.setCameraHeight(mCameraHeight);
            }
            mFaceDetector.setZoomRatio(5f);
            mFaceDetector.setOrientationOfCamera(mCamera.getOrientationOfCamera());
        } catch (Exception e) {
            closeCamera();
            mCheckListener.checkPixels(pixels, false);
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * 找出最大像素组合
     */
    private Size findMaxCameraSize(List<Size> cameraSizes) {
        // 按照分辨率从大到小排序
        List<Size> supportedResolutions = new ArrayList<>(cameraSizes);
        Collections.sort(supportedResolutions, new Comparator<Size>() {
            @Override
            public int compare(Size a, Size b) {
                int aPixels = a.getHeight() * a.getWidth();
                int bPixels = b.getHeight() * b.getWidth();
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });
        return supportedResolutions.get(0);
    }

    @Override
    public IPreview setFaceDetector(IFaceDetector faceDetector) {
        this.mFaceDetector = faceDetector;
        return this;
    }

    @Override
    public IPreview setCheckListener(ICameraCheckListener checkListener) {
        this.mCheckListener = checkListener;
        return this;
    }

    @Override
    public IPreview setCameraId(@CameraView.Facing int cameraId) {
        this.mCameraId = cameraId;
        return this;
    }

    @Override
    public IPreview setMinCameraPixels(long minCameraPixels) {
        this.mMinCameraPixels = minCameraPixels;
        return this;
    }

    @Override
    public void closeCamera() {
        if (mFaceDetector != null) mFaceDetector.setOpenCamera(false);
        if (mCamera == null) return;
        try {
            mCamera.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @CameraView.Facing
    public int getCameraId() {
        return mCameraId;
    }

    @Override
    public void release() {
        closeCamera();
        if (mFaceDetector != null) mFaceDetector.release();
    }
}
