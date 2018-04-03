package com.xsm.easy.core;

import android.app.Application;

/**
 * Author: 夏胜明
 * Date: 2018/4/3 0003
 * Email: xiasem@163.com
 * Description:
 */

public class EasyRouter {
    private static final String TAG = "EasyRouter";
    private static EasyRouter sInstance;
    private static Application mContext;

    public static EasyRouter getsInstance() {
        synchronized (EasyRouter.class) {
            if (sInstance == null) {
                sInstance = new EasyRouter();
            }
        }
        return sInstance;
    }

    public static void init(Application application) {
        mContext = application;
    }

    private static void loadInfo() {

    }
}
