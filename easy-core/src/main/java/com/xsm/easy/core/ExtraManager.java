package com.xsm.easy.core;

import android.app.Activity;
import android.util.LruCache;

import com.xsm.easy.core.template.IExtra;

/**
 * Author: 夏胜明
 * Date: 2018/4/25 0025
 * Email: xiasem@163.com
 * Description:
 */

public class ExtraManager {
    public static final String SUFFIX_AUTOWIRED = "_Extra";
    private static ExtraManager instance;
    private LruCache<String, IExtra> classCache;

    public static ExtraManager getInstance() {
        if (instance == null) {
            synchronized (ExtraManager.class) {
                if (instance == null) {
                    instance = new ExtraManager();
                }
            }
        }
        return instance;
    }


    public ExtraManager() {
        classCache = new LruCache<>(66);
    }


    /**
     * 注入
     *
     * @param instance
     */
    public void loadExtras(Activity instance) {
        //查找对应activity的缓存
        String className = instance.getClass().getName();
        IExtra iExtra = classCache.get(className);
        try {
            if (null == iExtra) {
                iExtra = (IExtra) Class.forName(instance.getClass().getName() +
                        SUFFIX_AUTOWIRED).getConstructor().newInstance();
            }
            iExtra.loadExtra(instance);
            classCache.put(className, iExtra);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
