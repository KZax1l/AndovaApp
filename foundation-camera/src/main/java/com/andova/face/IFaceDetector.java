package com.andova.face;

import android.hardware.Camera;

/**
 * Created by Administrator on 2018-03-09.
 * <p>识别接口</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public interface IFaceDetector<T> {
    void detector();

    void release();

    void setDataListener(IDataListener<T> mDataListener);

    void setCameraPreviewData(byte[] data, Camera camera);

    void setMaxFacesCount(int mMaxFacesCount);

    void setCameraHeight(int mCameraHeight);

    void setCameraWidth(int mCameraWidth);

    void setPreviewHeight(int mPreviewHeight);

    void setPreviewWidth(int mPreviewWidth);

    void setCameraId(int mCameraId);

    void setOrientationOfCamera(int orientationOfCamera);

    void setZoomRatio(float mZoomRatio);

    void setOpenCamera(boolean isOpenCamera);
}
