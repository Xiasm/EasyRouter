package com.xsm.easy.core.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @author: luoxiaohui
 * @date: 2019-06-18 14:21
 * @desc: 判断集合，Map等是否为空
 */
public class Utils {

    public static boolean isEmpty(String str){
        return str == null || str.equals("") || str.isEmpty();
    }

    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
}
