package com.andova.face;

/**
 * Created by Administrator on 2018-03-09.
 * <p>识别数据监听</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public interface IDataListener<T> {
    void onDetectorData(DetectorData<T> detectorData);
}
