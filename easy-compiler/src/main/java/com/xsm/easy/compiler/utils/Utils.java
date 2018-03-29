package com.xsm.easy.compiler.utils;

import java.util.Collection;
import java.util.Map;

/**
 * Author: 夏胜明
 * Date: 2018/3/29 0029
 * Email: xiasem@163.com
 * Description:
 */

public class Utils {
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
}
