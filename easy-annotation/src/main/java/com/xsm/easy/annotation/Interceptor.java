package com.xsm.easy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: luoxiaohui
 * @date: 2019-05-23 20:08
 * @desc:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Interceptor {

    /**
     * 拦截器优先级
     * @author luoxiaohui
     * @createTime 2019-06-04 20:51
     */
    int priority();
    /**
     * 拦截器的名称
     * @author luoxiaohui
     * @createTime 2019-05-23 20:33
     */
    String name() default "";
}
