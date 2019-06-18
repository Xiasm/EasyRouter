package com.xsm.easy.core.template;

import java.util.Map;

/**
 * @author: luoxiaohui
 * @date: 2019-05-23 20:36
 * @desc:
 */
public interface IInterceptorGroup {

    /**
     * key为拦截器的优先级，value为拦截器
     * @author luoxiaohui
     * @createTime 2019-05-23 20:54
     * @param map
     */
    void loadInto(Map<Integer, Class<? extends IInterceptor>> map);
}
