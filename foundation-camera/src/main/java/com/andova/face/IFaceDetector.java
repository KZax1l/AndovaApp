package com.andova.face;

import com.andova.face.detector.CameraProvider;
import com.google.android.cameraview.CameraView;

/**
 * Created by Administrator on 2018-03-09.
 * <p>识别接口</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public interface IFaceDetector {
    void detector();

    void release();

    void setDataListener(IDataListener dataListener);

    void setCameraPreviewData(byte[] data, CameraProvider camera);

    void setMaxFacesCount(int maxFacesCount);

    void setCameraHeight(int cameraHeight);

    void setCameraWidth(int cameraWidth);

    void setPreviewHeight(int previewHeight);

    void setPreviewWidth(int previewWidth);

    void setCameraId(@CameraView.Facing int cameraId);

    void setOrientationOfCamera(int orientationOfCamera);

    void setZoomRatio(float zoomRatio);

    void setOpenCamera(boolean isOpenCamera);
}
