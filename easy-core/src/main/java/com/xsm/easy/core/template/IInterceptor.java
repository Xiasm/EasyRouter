package com.xsm.easy.core.template;

import android.content.Context;

import com.xsm.easy.core.Postcard;
import com.xsm.easy.core.callback.InterceptorCallback;

/**
 * @author: luoxiaohui
 * @date: 2019-05-23 20:52
 * @desc:
 */
public interface IInterceptor {
    
    /**
     * 拦截器流程
     * @author luoxiaohui
     * @createTime 2019-05-23 20:53
     */
    void process(Postcard postcard, InterceptorCallback callback);

    /**
     * 在调用EasyRouter.init()初始化时，会调用到此方法
     * @author luoxiaohui
     * @createTime 2019-06-18 10:39
     */
    void init(Context context);
}
