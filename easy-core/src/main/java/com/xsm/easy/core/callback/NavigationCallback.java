package com.xsm.easy.core.callback;

import com.xsm.easy.core.Postcard;

/**
 * Author: 夏胜明
 * Date: 2018/4/25 0025
 * Email: xiasem@163.com
 * Description:
 */

public interface NavigationCallback {

    /**
     * 找到跳转页面
     * @param postcard
     */
    void onFound(Postcard postcard);

    /**
     * 未找到
     * @param postcard
     */
    void onLost(Postcard postcard);

    /**
     * 成功跳转
     * @param postcard
     */
    void onArrival(Postcard postcard);


}
