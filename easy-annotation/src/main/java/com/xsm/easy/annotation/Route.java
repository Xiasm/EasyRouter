package com.xsm.easy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author: 夏胜明
 * Date: 2018/3/29 0029
 * Email: xiasem@163.com
 * Description:
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Route {
    /**
     * 路由的路径
     * @return
     */
    String path();

    /**
     * 将路由节点进行分组，可以实现动态加载
     * @return
     */
    String group() default "";

}
