package com.andova.fdt.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Process;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.util.List;

/**
 * Created by Administrator on 2018-03-09.
 * <p>自定义相机</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private final String TAG = CameraPreview.class.getSimpleName();

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraId;
    private ICameraCheckListener mCheckListener;
    private IFaceDetector mFaceDetector;
    private int mDisplayOrientation;
    /**
     * 用来预览的宽度，如果是横屏，宽度值较高度值大，若是竖屏，宽度值较高度值小
     */
    private int mCameraWidth;
    /**
     * 用来预览的高度（即如果展示了系统状态栏、系统底部导航栏，则要减去这些显示部分的高度值）
     */
    private int mCameraHeight;

    public CameraPreview(Context context) {
        super(context);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mHolder = getHolder();
        mHolder.addCallback(this);

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mCameraWidth = metrics.widthPixels;
        mCameraHeight = metrics.heightPixels;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        openCamera();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        closeCamera();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }

    /**
     * 打开相机
     */
    public void openCamera() {
        if (null != mCamera) {
            return;
        }

        if (!checkCameraPermission()) {
            Log.i(TAG, "摄像头权限未打开，请打开后再试");
            return;
        }

        // 只有一个摄相头，打开后置
        if (Camera.getNumberOfCameras() == 1) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        if (mFaceDetector != null) {
            mFaceDetector.setCameraId(mCameraId);
        }

        try {
            mCamera = Camera.open(mCameraId);
            // setParameters 针对部分手机通过Camera.open()拿到的Camera对象不为null
            Camera.Parameters mParameters = mCamera.getParameters();
            mCamera.setParameters(mParameters);
            if (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId) {
                Log.i(TAG, "前置摄像头已开启");
            } else {
                Log.i(TAG, "后置摄像头已开启");
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

        try {
            //获取最大宽高，得出最大支持像素
            Camera.Parameters parameters = mCamera.getParameters();
            // 获取横屏模式下摄像头支持的PictureSize列表
            List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
            long pixels = 0L;
            for (Camera.Size size : pictureSizeList) {
                if (pixels >= size.width * size.height) continue;
                pixels = size.width * size.height;
            }
            // 回调该手机像素值
            if (mCheckListener != null) {
                if (pixels > 300 * 10000) {// 默认要求手机配置不低于300万像素
                    mCheckListener.checkPixels(pixels, true);
                } else {
                    mCheckListener.checkPixels(pixels, false);
                    closeCamera();
                    return;
                }
            }

            //设置预览回调
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (mFaceDetector != null) {
                        mFaceDetector.setCameraPreviewData(data, camera);
                        mFaceDetector.setOpenCamera(true);
                    }
                }
            });
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mCamera.setPreviewDisplay(mHolder);
            Log.i(TAG, "camera size width:" + mCameraWidth + ",height:" + mCameraHeight);
            if (mFaceDetector != null) {
                mFaceDetector.setCameraWidth(mCameraWidth);
                mFaceDetector.setCameraHeight(mCameraHeight);
            }
            //设置相机参数
            setCameraParams(mCamera, mCameraWidth, mCameraHeight);
            Log.i(TAG, "camera getPreviewSize width:" + mCamera.getParameters().getPreviewSize().width
                    + ",height:" + mCamera.getParameters().getPreviewSize().height);
            Log.i(TAG, "camera getPictureSize width:" + mCamera.getParameters().getPictureSize().width
                    + ",height:" + mCamera.getParameters().getPictureSize().height);
            //开始预览
            mCamera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * 关闭相机
     */
    public void closeCamera() {
        if (mFaceDetector != null) {
            mFaceDetector.setOpenCamera(false);
        }
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        closeCamera();
        if (mFaceDetector != null) {
            mFaceDetector.release();
            mFaceDetector = null;
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    public CameraPreview setFaceDetector(IFaceDetector mFaceDetector) {
        this.mFaceDetector = mFaceDetector;
        return this;
    }

    public CameraPreview setCheckListener(ICameraCheckListener mCheckListener) {
        this.mCheckListener = mCheckListener;
        return this;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public CameraPreview setCameraId(int mCameraId) {
        this.mCameraId = mCameraId;
        return this;
    }

    public int getCameraHeight() {
        return mCameraHeight;
    }

    public int getCameraWidth() {
        return mCameraWidth;
    }

    public int getDisplayOrientation() {
        return mDisplayOrientation;
    }

    /**
     * 检查相机权限
     */
    private boolean checkCameraPermission() {
        int status = getContext().checkPermission(Manifest.permission.CAMERA, Process.myPid(), Process.myUid());
        return PackageManager.PERMISSION_GRANTED == status;
    }

    /**
     * 在摄像头启动前设置参数
     *
     * @param width  see {@link #mCameraWidth}
     * @param height see {@link #mCameraHeight}
     */
    private void setCameraParams(Camera camera, int width, int height) {
        // 获取摄像头支持的pictureSize列表
        Camera.Parameters parameters = camera.getParameters();
        // 获取横屏模式下摄像头支持的PictureSize列表
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        // 从列表中选择合适的分辨率
        Point pictureSize = FaceUtil.findBestResolution(pictureSizeList, new Point(width, height), true, 0.15f);
        // 根据选出的PictureSize重新设置SurfaceView大小
        parameters.setPictureSize(pictureSize.x, pictureSize.y);

        // 获取横屏模式下摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Point preSize = FaceUtil.findBestResolution(previewSizeList, new Point(width, height), false, 0.15f);
        parameters.setPreviewSize(preSize.x, preSize.y);

        float w = preSize.x;
        float h = preSize.y;
        float scale = 1.0f;

        boolean isCandidatePortrait = w > h;// true代表横屏
        float maybeFlippedWidth = isCandidatePortrait ? h : w;// 竖屏所对应的宽度值，较小值
        float maybeFlippedHeight = isCandidatePortrait ? w : h;// 竖屏所对应的高度值，较大值

        /**
         * 由于{@link pictureSizeList}和{@link previewSizeList}获取的是系统返回的横屏模式下的数据，
         * 所以宽度值较高度值大
         */
        int tempW = (int) (height * (maybeFlippedWidth / maybeFlippedHeight));
        int tempH = (int) (width * (maybeFlippedHeight / maybeFlippedWidth));
        if (tempW >= width) {
            setLayoutParams(new FrameLayout.LayoutParams(tempW, height));
            scale = tempW / maybeFlippedWidth;
        } else if (tempH >= height) {
            setLayoutParams(new FrameLayout.LayoutParams(width, tempH));
            scale = tempH / maybeFlippedHeight;
        } else {
            setLayoutParams(new FrameLayout.LayoutParams(width, height));
        }
        if (mFaceDetector != null) {
            mFaceDetector.setZoomRatio(5f * scale);
            mFaceDetector.setPreviewWidth((int) maybeFlippedWidth);
            mFaceDetector.setPreviewHeight((int) maybeFlippedHeight);
        }

        parameters.setJpegQuality(100);
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            // 连续对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.cancelAutoFocus();
        setCameraDisplayOrientation();
        camera.setParameters(parameters);
    }

    /**
     * 设置相机显示方向
     */
    private void setCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }
        if (mFaceDetector != null) {
            mFaceDetector.setOrientionOfCamera(info.orientation);
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degree) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degree + 360) % 360;
        }
        mDisplayOrientation = result;
        mCamera.setDisplayOrientation(result);
    }
}
