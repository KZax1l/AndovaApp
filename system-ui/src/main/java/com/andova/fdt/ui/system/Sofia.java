package com.andova.fdt.ui.system;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * Created by Administrator on 2018-03-13.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class Sofia {

    private Sofia() {
    }

    public static Bar with(Activity activity) {
        Window window = activity.getWindow();
        ViewGroup contentLayout = window.getDecorView().findViewById(Window.ID_ANDROID_CONTENT);
        if (contentLayout.getChildCount() > 0) {
            View contentView = contentLayout.getChildAt(0);
            if (contentView instanceof Bar) {
                return (Bar) contentView;
            }
        }
        return new HostLayout(activity);
    }
}