package com.andova.fdt.ui.system;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Field;

/**
 * Created by Administrator on 2018-03-13.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MeasureView extends View {

    private static boolean isInitialize = false;

    protected static int sStatusBarHeight = 0;
    protected static int sNavigationBarHeight = 0;

    private static void measureScreenSize(Context context) {
        if (isInitialize) return;
        isInitialize = true;

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1)
            display.getRealMetrics(displayMetrics);
        else display.getMetrics(displayMetrics);

        sNavigationBarHeight = displayMetrics.heightPixels - getDisplayHeight(display);

        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Field field = clazz.getField("status_bar_height");
            int x = Integer.parseInt(field.get(clazz.newInstance()).toString());
            sStatusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Throwable ignored) {
        }
    }

    private static int getDisplayHeight(Display display) {
        Point point = new Point();
        display.getSize(point);
        return point.y;
    }

    public MeasureView(Context context) {
        this(context, null, 0);
    }

    public MeasureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MeasureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        measureScreenSize(context);
    }
}