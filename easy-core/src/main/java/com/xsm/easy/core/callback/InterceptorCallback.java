package com.xsm.easy.core.callback;

import com.xsm.easy.core.Postcard;

/**
 * @author: luoxiaohui
 * @date: 2019-05-23 20:41
 * @desc: 拦截器回调
 */
public interface InterceptorCallback {

    /**
     * 未拦截，走正常流程
     * @author luoxiaohui
     * @createTime 2019-05-23 20:50
     */
    void onNext(Postcard postcard);

    /**
     * 拦截器拦截成功，中断流程
     * @author luoxiaohui
     * @createTime 2019-05-23 20:42
     */
    void onInterrupt(String interruptMsg);
}
