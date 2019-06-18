package com.xsm.module1;

import android.content.Context;
import android.util.Log;

import com.xsm.easy.annotation.Interceptor;
import com.xsm.easy.core.Postcard;
import com.xsm.easy.core.callback.InterceptorCallback;
import com.xsm.easy.core.template.IInterceptor;

/**
 * @author: luoxiaohui
 * @date: 2019-05-30 20:35
 * @desc:
 */
@Interceptor(priority = 1, name = "login")
public class LoginInterceptor implements IInterceptor {

    private static final String TAG = "LoginInterceptor";

    /**
     * 拦截器流程
     *
     * @param postcard
     * @param callback
     * @author luoxiaohui
     * @createTime 2019-05-23 20:53
     */
    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {

        Log.e(TAG, "process()...");
        callback.onNext(postcard);
    }

    /**
     * 在调用EasyRouter.init()初始化时，会调用到此方法
     *
     * @param context
     * @author luoxiaohui
     * @createTime 2019-06-18 10:39
     */
    @Override
    public void init(Context context) {

        Log.e(TAG, "init()...");
    }
}
