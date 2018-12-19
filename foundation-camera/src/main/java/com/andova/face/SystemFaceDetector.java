package com.andova.face;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.FaceDetector;
import android.support.annotation.NonNull;
import android.util.Log;

import com.andova.face.detector.CameraProvider;

import java.io.ByteArrayOutputStream;

import static com.google.android.cameraview.CameraView.FACING_FRONT;

/**
 * Created by Administrator on 2018-03-09.
 * <p>利用系统提供的FaceDetector实现人脸识别</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class SystemFaceDetector extends BaseFaceDetector {
    private final String TAG = SystemFaceDetector.class.getSimpleName();

    private FaceDetector.Face[] mFaces;
    private byte[] mPreviewBuffer;

    public SystemFaceDetector() {
        super();
    }

    @Override
    protected void detectionFaces(@NonNull byte[] data, @NonNull CameraProvider camera) {
        /**
         * 这里需要注意，回调出来的data不是我们直接意义上的RGB图 而是YUV图，因此我们需要
         * 将YUV转化为bitmap再进行相应的人脸检测，同时注意必须使用RGB_565，才能进行人脸检测，其余无效
         */
        try {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, camera.previewWidth, camera.previewHeight, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, camera.previewWidth, camera.previewHeight), 100, baos);
            mPreviewBuffer = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mPreviewBuffer == null || mPreviewBuffer.length == 0) {
            return;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测
        Bitmap bitmap = BitmapFactory.decodeByteArray(mPreviewBuffer, 0, mPreviewBuffer.length, options);
        if (bitmap == null) {
            return;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        //设置各个角度的相机，这样我们的检测效果才是最好
        switch (getOrientationOfCamera()) {
            case 0:
                matrix.postRotate(0.0f, width / 2, height / 2);
                break;
            case 90:
                matrix.postRotate(-270.0f, height / 2, width / 2);
                break;
            case 180:
                matrix.postRotate(-180.0f, width / 2, height / 2);
                break;
            case 270:
                matrix.postRotate(-90.0f, height / 2, width / 2);
                break;
        }
        matrix.postScale(0.2f, 0.2f);//为了减小内存压力，将图片缩放，但是也不能太小，否则检测不到人脸
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

        //初始化人脸检测
        FaceDetector detector = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), getMaxFacesCount());
        mFaces = new FaceDetector.Face[getMaxFacesCount()];
        //这里通过向findFaces中传递帧图转化后的bitmap和最大检测的人脸数face，返回检测后的人脸数
        getDetectorData().setFacesCount(detector.findFaces(bitmap, mFaces));
        //绘制识别后的人脸区域的类
        getFaceRect();
        bitmap.recycle();
    }

    /**
     * 计算识别框
     */
    private void getFaceRect() {
        Rect[] faceRectList = new Rect[getDetectorData().getFacesCount()];
        Rect rect = null;
        int index = 0;
        float distance = 0;
        for (int i = 0; i < getDetectorData().getFacesCount(); i++) {
            faceRectList[i] = new Rect();
            FaceDetector.Face face = mFaces[i];
            if (face != null) {
                float eyeDistance = face.eyesDistance();
                eyeDistance = eyeDistance * getZoomRatio();
                if (eyeDistance > distance) {
                    distance = eyeDistance;
                    rect = faceRectList[i];
                    index = i;
                }
                PointF midEyesPoint = new PointF();
                face.getMidPoint(midEyesPoint);
                midEyesPoint.x = midEyesPoint.x * getZoomRatio();
                midEyesPoint.y = midEyesPoint.y * getZoomRatio();
                Log.i(TAG, "eyeDistance:" + eyeDistance + ",midEyesPoint.x:" + midEyesPoint.x
                        + ",midEyesPoint.y:" + midEyesPoint.y);
                faceRectList[i].set((int) (midEyesPoint.x - eyeDistance),
                        (int) (midEyesPoint.y - eyeDistance),
                        (int) (midEyesPoint.x + eyeDistance),
                        (int) (midEyesPoint.y + eyeDistance));
                Log.i(TAG, "FaceRectList[" + i + "]:" + faceRectList[i]);
            }
        }
        int width = (int) (getPreviewHeight() * getZoomRatio() / 5);
        if (rect != null && getCameraId() == FACING_FRONT) {
            int left = rect.left;
            rect.left = width - rect.right;
            rect.right = width - left;
            faceRectList[index].left = rect.left;
            faceRectList[index].right = rect.right;
        }
        getDetectorData().setLightIntensity(FaceUtil.getYUVLight(getDetectorData().getFaceData(), rect, width));
        getDetectorData().setFaceRectList(faceRectList);
        if (getCameraWidth() > 0) {
            getDetectorData().setDistance(distance * 2.5f / getCameraWidth());
        }
    }
}
