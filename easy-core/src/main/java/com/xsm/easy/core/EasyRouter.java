package com.xsm.easy.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;

import com.xsm.easy.annotation.modle.RouteMeta;
import com.xsm.easy.core.callback.InterceptorCallback;
import com.xsm.easy.core.callback.NavigationCallback;
import com.xsm.easy.core.exception.NoRouteFoundException;
import com.xsm.easy.core.implments.InterceptorImpl;
import com.xsm.easy.core.template.IInterceptor;
import com.xsm.easy.core.template.IInterceptorGroup;
import com.xsm.easy.core.template.IRouteGroup;
import com.xsm.easy.core.template.IRouteRoot;
import com.xsm.easy.core.template.IService;
import com.xsm.easy.core.utils.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

/**
 * Author: 夏胜明
 * Date: 2018/4/3 0003
 * Email: xiasem@163.com
 * Description:
 */

public class EasyRouter {
    private static final String TAG = "EasyRouter";
    private static final String ROUTE_ROOT_PAKCAGE = "com.xsm.easyrouter.routes";
    private static final String SDK_NAME = "EaseRouter";
    private static final String SEPARATOR = "_";
    private static final String SUFFIX_ROOT = "Root";
    private static final String SUFFIX_INTERCEPTOR = "Interceptor";

    private static EasyRouter sInstance;
    private static Application mContext;
    private Handler mHandler;

    private EasyRouter() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static EasyRouter getsInstance() {
        if (sInstance == null) {
            synchronized (EasyRouter.class) {
                if (sInstance == null) {
                    sInstance = new EasyRouter();
                }
            }
        }
        return sInstance;
    }

    public static void init(Application application) {
        mContext = application;
        try {
            loadInfo();
            InterceptorImpl.init(application.getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "初始化失败!", e);
        }
    }


    /**
     * 分组表制作
     */
    private static void loadInfo() throws PackageManager.NameNotFoundException, InterruptedException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        //获得所有 apt生成的路由类的全类名 (路由表)
        Set<String> routerMap = ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE);
        for (String className : routerMap) {
            if (className.startsWith(ROUTE_ROOT_PAKCAGE + "." + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {
                //root中注册的是分组信息 将分组信息加入仓库中
                ((IRouteRoot) Class.forName(className).getConstructor().newInstance()).loadInto(Warehouse.groupsIndex);
            } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + "." + SDK_NAME + SEPARATOR + SUFFIX_INTERCEPTOR)) {

                ((IInterceptorGroup) Class.forName(className).getConstructor().newInstance()).loadInto(Warehouse.interceptorsIndex);
            }
        }
        for (Map.Entry<String, Class<? extends IRouteGroup>> stringClassEntry : Warehouse.groupsIndex.entrySet()) {
            Log.d(TAG, "Root映射表[ " + stringClassEntry.getKey() + " : " + stringClassEntry.getValue() + "]");
        }

    }

    public Postcard build(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("路由地址无效!");
        } else {
            return build(path, extractGroup(path));
        }
    }

    public Postcard build(String path, String group) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(group)) {
            throw new RuntimeException("路由地址无效!");
        } else {
            return new Postcard(path, group);
        }
    }

    /**
     * 获得组别
     *
     * @param path
     * @return
     */
    private String extractGroup(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new RuntimeException(path + " : 不能提取group.");
        }
        try {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (TextUtils.isEmpty(defaultGroup)) {
                throw new RuntimeException(path + " : 不能提取group.");
            } else {
                return defaultGroup;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected Object navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {

        if (callback != null) {

            InterceptorImpl.onInterceptions(postcard, new InterceptorCallback() {
                @Override
                public void onNext(Postcard postcard) {
                    _navigation(context, postcard, requestCode, callback);
                }

                @Override
                public void onInterrupt(String interruptMsg) {

                    callback.onInterrupt(new Throwable(interruptMsg));
                }
            });
        }else{

            return _navigation(context, postcard, requestCode, callback);
        }

        return null;
    }

    protected Object _navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        try {
            prepareCard(postcard);
        } catch (NoRouteFoundException e) {
            e.printStackTrace();
            //没找到
            if (null != callback) {
                callback.onLost(postcard);
            }
            return null;
        }
        if (null != callback) {
            callback.onFound(postcard);
        }

        switch (postcard.getType()) {
            case ACTIVITY:
                final Context currentContext = null == context ? mContext : context;
                final Intent intent = new Intent(currentContext, postcard.getDestination());
                intent.putExtras(postcard.getExtras());
                int flags = postcard.getFlags();
                if (-1 != flags) {
                    intent.setFlags(flags);
                } else if (!(currentContext instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //可能需要返回码
                        if (requestCode > 0) {
                            ActivityCompat.startActivityForResult((Activity) currentContext, intent,
                                    requestCode, postcard.getOptionsBundle());
                        } else {
                            ActivityCompat.startActivity(currentContext, intent, postcard
                                    .getOptionsBundle());
                        }

                        if ((0 != postcard.getEnterAnim() || 0 != postcard.getExitAnim()) &&
                                currentContext instanceof Activity) {
                            //老版本
                            ((Activity) currentContext).overridePendingTransition(postcard
                                            .getEnterAnim()
                                    , postcard.getExitAnim());
                        }
                        //跳转完成
                        if (null != callback) {
                            callback.onArrival(postcard);
                        }
                    }
                });
                break;
            case ISERVICE:
                return postcard.getService();
            default:
                break;
        }
        return null;
    }

    /**
     * 准备卡片
     *
     * @param card
     */
    private void prepareCard(Postcard card) {
        RouteMeta routeMeta = Warehouse.routes.get(card.getPath());
        if (null == routeMeta) {
            Class<? extends IRouteGroup> groupMeta = Warehouse.groupsIndex.get(card.getGroup());
            if (null == groupMeta) {
                throw new NoRouteFoundException("没找到对应路由：分组=" + card.getGroup() + "   路径=" + card.getPath());
            }
            IRouteGroup iGroupInstance;
            try {
                iGroupInstance = groupMeta.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("路由分组映射表记录失败.", e);
            }
            iGroupInstance.loadInto(Warehouse.routes);
            //已经准备过了就可以移除了 (不会一直存在内存中)
            Warehouse.groupsIndex.remove(card.getGroup());
            //再次进入 else
            prepareCard(card);
        } else {
            //类 要跳转的activity 或IService实现类
            card.setDestination(routeMeta.getDestination());
            card.setType(routeMeta.getType());
            switch (routeMeta.getType()) {
                case ISERVICE:
                    Class<?> destination = routeMeta.getDestination();
                    IService service = Warehouse.services.get(destination);
                    if (null == service) {
                        try {
                            service = (IService) destination.getConstructor().newInstance();
                            Warehouse.services.put(destination, service);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    card.setService(service);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 注入
     *
     * @param instance
     */
    public void inject(Activity instance) {
        ExtraManager.getInstance().loadExtras(instance);
    }
}
