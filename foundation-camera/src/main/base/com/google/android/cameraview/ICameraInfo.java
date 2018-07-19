package com.google.android.cameraview;

import java.util.List;

/**
 * Created by Administrator on 2018-07-19.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public interface ICameraInfo {
    int getNumberOfCameras();

    List<Size> getSupportedPictureSizes();

    int getCameraWidth();

    int getCameraHeight();
}
