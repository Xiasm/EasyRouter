package com.xsm.easy.core.implments;

import android.content.Context;

import com.xsm.easy.core.Postcard;
import com.xsm.easy.core.Warehouse;
import com.xsm.easy.core.callback.InterceptorCallback;
import com.xsm.easy.core.template.IInterceptor;
import com.xsm.easy.core.thread.DefaultPoolExecutor;
import com.xsm.easy.core.utils.CancelableCountDownLatch;
import com.xsm.easy.core.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: luoxiaohui
 * @date: 2019-06-18 14:08
 * @desc: 拦截器实现，在初始化路由，以及调用路由时，都需要调用到此类
 */
public class InterceptorImpl {

    /**
     * 初始化路由时，需要轮询每个拦截器中的init()方法
     *
     * @author luoxiaohui
     * @createTime 2019-06-18 14:10
     */
    public static void init(final Context context) {

        DefaultPoolExecutor.executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!Utils.isEmpty(Warehouse.interceptorsIndex)) {
                    for (Map.Entry<Integer, Class<? extends IInterceptor>> entry : Warehouse.interceptorsIndex.entrySet()) {
                        Class<? extends IInterceptor> interceptorClass = entry.getValue();
                        try {
                            IInterceptor iInterceptor = interceptorClass.getConstructor().newInstance();
                            iInterceptor.init(context);
                            Warehouse.interceptors.add(iInterceptor);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    /**
     * 执行拦截逻辑
     *
     * @author luoxiaohui
     * @createTime 2019-06-18 14:56
     */
    public static void onInterceptions(final Postcard postcard, final InterceptorCallback callback) {

        if (Warehouse.interceptors.size() > 0) {
            DefaultPoolExecutor.executor.execute(new Runnable() {
                @Override
                public void run() {

                    CancelableCountDownLatch countDownLatch = new CancelableCountDownLatch(Warehouse.interceptors.size());
                    execute(0, countDownLatch, postcard);
                    try {
                        countDownLatch.await(300, TimeUnit.SECONDS);
                        if (countDownLatch.getCount() > 0){

                            callback.onInterrupt("拦截器处理超时");
                        }else if(!Utils.isEmpty(countDownLatch.getMsg())){

                            callback.onInterrupt(countDownLatch.getMsg());
                        }else {

                            callback.onNext(postcard);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {

            callback.onNext(postcard);
        }
    }


    /**
     * 以递归的方式走完所有拦截器的process()方法
     *
     * @author luoxiaohui
     * @createTime 2019-06-18 15:22
     */
    private static void execute(final int index, final CancelableCountDownLatch countDownLatch, final Postcard postcard) {
        if (index < Warehouse.interceptors.size()){

            IInterceptor iInterceptor = Warehouse.interceptors.get(index);
            iInterceptor.process(postcard, new InterceptorCallback() {
                @Override
                public void onNext(Postcard postcard) {

                    countDownLatch.countDown();
                    execute(index + 1, countDownLatch, postcard);
                }

                @Override
                public void onInterrupt(String msg) {

                    countDownLatch.cancel(msg);
                }
            });
        }
    }
}
































