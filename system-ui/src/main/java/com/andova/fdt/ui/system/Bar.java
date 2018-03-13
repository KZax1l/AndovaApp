package com.andova.fdt.ui.system;

import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Created by Administrator on 2018-03-13.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public interface Bar {

    Bar statusBarDarkFont();

    Bar statusBarLightFont();

    Bar statusBarBackground(int statusBarColor);

    Bar statusBarBackground(Drawable drawable);

    Bar statusBarBackgroundAlpha(int alpha);

    Bar navigationBarBackground(int navigationBarColor);

    Bar navigationBarBackground(Drawable drawable);

    Bar navigationBarBackgroundAlpha(int alpha);

    Bar invasionStatusBar();

    Bar invasionNavigationBar();

    Bar fitsSystemWindowView(int viewId);

    Bar fitsSystemWindowView(View view);
}