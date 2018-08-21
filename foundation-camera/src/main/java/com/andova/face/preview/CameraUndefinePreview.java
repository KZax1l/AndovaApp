package com.andova.face.preview;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.andova.face.FaceUtil;
import com.andova.face.ICameraCheckListener;
import com.andova.face.IFaceDetector;
import com.andova.face.detector.CameraProvider;
import com.andova.face.detector.IPreview;
import com.google.android.cameraview.CameraView;

import static com.google.android.cameraview.CameraView.FACING_BACK;
import static com.google.android.cameraview.CameraView.FACING_FRONT;

/**
 * Created by Administrator on 2018-08-20.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class CameraUndefinePreview extends SurfaceView implements SurfaceHolder.Callback, IPreview {
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private IFaceDetector mFaceDetector;
    private ICameraCheckListener mCheckListener;

    @CameraView.Facing
    private int mCameraId;
    private long mMinCameraPixels;
    private CameraProvider mCameraProvider = new CameraProvider();

    public CameraUndefinePreview(Context context) {
        super(context);
        init(context);
    }

    public CameraUndefinePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraUndefinePreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mHolder = getHolder();
        mHolder.addCallback(this);

//        DisplayMetrics metrics = new DisplayMetrics();
//        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        mCameraWidth = metrics.widthPixels;
//        mCameraHeight = metrics.heightPixels;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        closeCamera();
    }

    @Override
    public void openCamera() {
        if (null != mCamera) {
            return;
        }

        if (!FaceUtil.checkCameraPermission(getContext())) {
            System.out.println("摄像头权限未打开，请打开后再试");
            if (mCheckListener != null) {
                mCheckListener.checkPermission(false);
            }
            return;
        }

        // 只有一个摄相头，打开后置
        if (Camera.getNumberOfCameras() == 1) {
            mCameraId = FACING_BACK;
        }

        if (mFaceDetector != null) {
            mFaceDetector.setCameraId(mCameraId);
        }

        try {
            mCamera = Camera.open(mCameraId);
            if (FACING_FRONT == mCameraId) {
                System.out.println("前置摄像头已开启");
            } else {
                System.out.println("后置摄像头已开启");
            }
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

        if (mCamera == null || mHolder == null || mHolder.getSurface() == null) {
            return;
        }

        //回调权限判定结果
        if (mCheckListener != null) {
            mCheckListener.checkPermission(true);
        }

        long pixels = 0;
        try {
            //获取最大宽高，得出最大支持像素
            Camera.Parameters parameters = mCamera.getParameters();
            // getSupportedPictureSizes：获取横屏模式下摄像头支持的PictureSize列表
            Camera.Size maxPictureSize = FaceUtil.findMaxCameraSize(parameters.getSupportedPictureSizes());
            if (maxPictureSize != null) {
                pixels = maxPictureSize.width * maxPictureSize.height;
            }
            System.out.println("camera max support pixels: " + pixels);
            //回调该手机像素值
            if (mCheckListener != null && mMinCameraPixels > 0) {
                if (pixels >= mMinCameraPixels) {
                    mCheckListener.checkPixels(pixels, true);
                } else {
                    System.out.println("camera support pixels is so small, close camera!");
                    closeCamera();
                    mCheckListener.checkPixels(pixels, false);
                    return;
                }
            }

            //设置预览回调
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (mFaceDetector != null) {
                        mCameraProvider.previewWidth = camera.getParameters().getPreviewSize().width;
                        mCameraProvider.previewHeight = camera.getParameters().getPreviewSize().height;
                        mFaceDetector.setCameraPreviewData(data, mCameraProvider);
                        mFaceDetector.setOpenCamera(true);
                    }
                }
            });
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mCamera.setPreviewDisplay(mHolder);
            if (mFaceDetector != null) {
//                mFaceDetector.setCameraWidth(mCameraWidth);
//                mFaceDetector.setCameraHeight(mCameraHeight);
            }
            //开始预览
            mCamera.startPreview();
        } catch (Exception e) {
            closeCamera();
            mCheckListener.checkPixels(pixels, false);
            System.out.println("Error starting camera preview: " + e.getMessage());
        }
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
    public IPreview setCameraId(int cameraId) {
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
        if (mFaceDetector != null) {
            mFaceDetector.setOpenCamera(false);
        }
        if (null != mCamera) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getCameraId() {
        return mCameraId;
    }

    @Override
    public void release() {
        closeCamera();
        if (mFaceDetector != null) {
            mFaceDetector.release();
        }
    }
}
