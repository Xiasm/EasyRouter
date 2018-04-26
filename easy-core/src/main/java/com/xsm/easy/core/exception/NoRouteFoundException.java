package com.xsm.easy.core.exception;

/**
 * Author: 夏胜明
 * Date: 2018/4/25 0025
 * Email: xiasem@163.com
 * Description:
 */

public class NoRouteFoundException extends RuntimeException {

    public NoRouteFoundException(String detailMessage) {
        super(detailMessage);
    }
}
