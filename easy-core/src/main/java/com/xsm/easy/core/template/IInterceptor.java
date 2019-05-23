package com.xsm.easy.core.template;

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
}
