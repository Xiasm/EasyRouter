package com.xsm.easy.core.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: 夏胜明
 * Date: 2018/4/3 0003
 * Email: xiasem@163.com
 * Description:
 */

public class DefaultPoolExecutor {

    public static ThreadPoolExecutor executor;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "EasyRouter #" + mCount.getAndIncrement());
        }
    };

    //核心线程和最大线程都是cpu核心数+1
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAX_CORE_POOL_SIZE = CPU_COUNT + 1;
    //存活30秒 回收线程
    private static final long SURPLUS_THREAD_LIFE = 30L;

    public static ThreadPoolExecutor newDefaultPoolExecutor(int corePoolSize) {
        if (corePoolSize == 0) {
            return null;
        }
        corePoolSize = Math.min(corePoolSize, MAX_CORE_POOL_SIZE);
        executor = new ThreadPoolExecutor(corePoolSize,
                corePoolSize, SURPLUS_THREAD_LIFE, TimeUnit.SECONDS, new
                ArrayBlockingQueue<Runnable>(64), sThreadFactory);
        //核心线程也会被销毁
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }



}

























