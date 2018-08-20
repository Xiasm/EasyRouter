package com.xsm.module1;

import com.xsm.base.providers.module1.Module1Providers;
import com.xsm.easy.annotation.Route;

/**
 * Author: 夏胜明
 * Date: 2018/8/20 0020
 * Email: xiasem@163.com
 * Description:
 */
@Route(path = "/module1/providers")
public class Module1ProvidersImpl implements Module1Providers {

    @Override
    public int add(int a, int b) {
        return a + b;
    }

}
