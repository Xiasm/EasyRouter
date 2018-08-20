package com.xsm.easy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author: 夏胜明
 * Date: 2018/8/20 0020
 * Email: xiasem@163.com
 * Description:
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface Extra {
    String name() default "";
}
