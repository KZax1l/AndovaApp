package com.andova.app;

import android.app.Application;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

/**
 * Created by Administrator on 2018-02-28.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class AndovaApplication extends Application {
    private RefWatcher refWatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader();
        refWatcher = setupLeakCanary();
    }

    private void initImageLoader() {
        ImageLoaderConfiguration localImageLoaderConfiguration = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(localImageLoaderConfiguration);
    }

    private RefWatcher setupLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return RefWatcher.DISABLED;
        }
        return LeakCanary.install(this);
    }

    /**
     * 调用{@link RefWatcher#watch(Object)}来监听可能内存泄漏的对象（如：Fragment对象）
     * <p>
     * LeakCanary在调用{@link LeakCanary#install(Application)}方法时会启动一个ActivityRefWatcher类，
     * 它用于自动监控Activity执行onDestroy方法之后是否发生内存泄露
     */
    public RefWatcher getRefWatcher() {
        return refWatcher;
    }
}
