package com.andova.app.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 2018-07-19.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class ImageUtil {
    /**
     * 将相机采集到的图像数据转为Bitmap
     */
    public static Bitmap decodeToBitmap(byte[] data, Camera camera) {
        if (data == null || camera == null) return null;
        ByteArrayOutputStream stream = null;
        Camera.Size size = camera.getParameters().getPreviewSize();
        try {
            YuvImage image = new YuvImage(rotateYUVDegree270AndMirror(data, size.width, size.height), ImageFormat.NV21, size.height, size.width, null);
            stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, size.height, size.width), 100, stream);
            return BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 一般前置摄像头有270度的旋转，所以要对YUV数据进行一定旋转操作,同时对于前置摄像头的数据还要进行镜像翻转的操作
     */
    private static byte[] rotateYUVDegree270AndMirror(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate and mirror the Y luma
        int i = 0;
        int maxY;
        for (int x = imageWidth - 1; x >= 0; x--) {
            maxY = imageWidth * (imageHeight - 1) + x * 2;
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[maxY - (y * imageWidth + x)];
                i++;
            }
        }
        // Rotate and mirror the U and V color components
        int uvSize = imageWidth * imageHeight;
        i = uvSize;
        int maxUV;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            maxUV = imageWidth * (imageHeight / 2 - 1) + x * 2 + uvSize;
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[maxUV - 2 - (y * imageWidth + x - 1)];
                i++;
                yuv[i] = data[maxUV - (y * imageWidth + x)];
                i++;
            }
        }
        return yuv;
    }
}
