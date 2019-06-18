package com.xsm.easy.core.utils;

import java.util.concurrent.CountDownLatch;

/**
 * @author: luoxiaohui
 * @date: 2019-06-18 14:46
 * @desc:
 */
public class CancelableCountDownLatch extends CountDownLatch {

    private String msg = "";

    public CancelableCountDownLatch(int count) {
        super(count);
    }

    /**
     * 当遇到特殊情况时，需要将计步器清0
     *
     * @author luoxiaohui
     * @createTime 2019-06-18 14:47
     */
    public void cancel(String msg) {
        this.msg = msg;
        while (getCount() > 0) {
            countDown();
        }
    }

    public String getMsg(){
        return msg;
    }
}
