package com.andova.face;

/**
 * Created by Administrator on 2018-03-09.
 * <p>相机检查回调</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public interface ICameraCheckListener {
    void checkPermission(boolean isAllow);

    void checkPixels(long pixels, boolean isSupport);
}
