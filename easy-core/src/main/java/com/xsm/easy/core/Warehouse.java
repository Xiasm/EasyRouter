package com.xsm.easy.core;

import com.xsm.easy.annotation.modle.RouteMeta;
import com.xsm.easy.core.template.IInterceptor;
import com.xsm.easy.core.template.IRouteGroup;
import com.xsm.easy.core.template.IService;
import com.xsm.easy.core.utils.UniqueKeyTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: 夏胜明
 * Date: 2018/4/24 0024
 * Email: xiasem@163.com
 * Description:
 */

public class Warehouse {

    // root 映射表 保存分组信息
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();

    // group 映射表 保存组中的所有数据
    static Map<String, RouteMeta> routes = new HashMap<>();

    // group 映射表 保存组中的所有数据
    static Map<Class, IService> services = new HashMap<>();
    // TestServiceImpl.class , TestServiceImpl 没有再反射

    /**
     * 以键值对优先级的方式保存拦截器对象
     */
    public static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new UniqueKeyTreeMap<>();
    /**
     * 以集合的方式保存所有拦截器对象
     */
    public static List<IInterceptor> interceptors = new ArrayList<>();
}
