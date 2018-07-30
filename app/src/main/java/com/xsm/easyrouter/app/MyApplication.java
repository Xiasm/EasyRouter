package com.xsm.easyrouter.app;

import android.app.Application;

import com.xsm.easy.core.EasyRouter;

/**
 * Author: 夏胜明
 * Date: 2018/7/30 0030
 * Email: xiasem@163.com
 * Description:
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            EasyRouter.init(this);
        } catch (Exception e) {

        }
    }
}
