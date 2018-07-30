### router路由原理解析及手动实现（二）------ 手动实现类似ARouter调用

&nbsp;&nbsp;&nbsp;&nbsp;通过上一章，我们知道了router路由的原理。看似很复杂的路由框架，其实原理很简单，我们可以理解为一个map(其实是两个map，一个保存group列表，一个保存group下的路由地址和activityClass关系)保存了路由地址和ActivityClass的映射关系，然后通过map.get("router address") 拿到AncivityClass，通过startActivity()调用就好了。但一个框架的设计要考虑的事情远远没有这么简单，而今天我们实现的路由框架，也只是一个最精简的router框架，但麻雀虽小，五脏俱全，我们就来一步步实现这个框架吧。<br/>

##### 第一节 拿到apt生成的类，进行框架初始化

&nbsp;&nbsp;&nbsp;&nbsp;要实现这么一个路由框架，首先我们需要在用户使用路由跳转之前把这些路由映射关系拿到手，拿到这些路由关系最好的时机就是应用程序初始化的时候，上一章第二节我贴了几行代码，是通过apt生成的路由映射关系文件，为了大家观看方便，我把这些文件重新粘贴到下面代码中（这几个类都是单独的文件，在各个模块下/build/generated/source/apt文件夹下面，为了演示方便我只贴出来了app模块下生成的类，其他模块如module1、module2下面的类跟app下面的没有什么区别），在程序启动的时候扫描这些生成的类文件，然后获取到映射关系信息，保存起来。
```
public class EaseRouter_Root_app implements IRouteRoot {
  @Override
  public void loadInto(Map<String, Class<? extends IRouteGroup>> routes) {
    routes.put("main", EaseRouter_Group_main.class);
    routes.put("show", EaseRouter_Group_show.class);
  }
}


public class EaseRouter_Group_main implements IRouteGroup {
  @Override
  public void loadInto(Map<String, RouteMeta> atlas) {
    atlas.put("/main/main",RouteMeta.build(RouteMeta.Type.ACTIVITY,Main2\Activity.class,"/main/main","main"));
    atlas.put("/main/main2",RouteMeta.build(RouteMeta.Type.ACTIVITY,Main2\Activity.class,"/main/main2","main"));
  }
}

public class EaseRouter_Group_show implements IRouteGroup {
  @Override
  public void loadInto(Map<String, RouteMeta> atlas) {
    atlas.put("/show/info",RouteMeta.build(RouteMeta.Type.ACTIVITY,ShowActivity.class,"/show/info","show"));
  }
}
```
&nbsp;&nbsp;&nbsp;&nbsp;可以看到，这些文件中，实现了IRouteRoot接口的类都是保存了group分组映射信息，实现了IRouteGroup接口的类都保存了单个分组下的路由映射信息。只要我们得到实现IRouteRoot接口的所有类文件，便能通过循环调用它的loadInfo()方法，得到所有实现IRouteGroup接口的类，而实现IRouteGroup接口的类里面同样的loadInfo()方法，通过传入一个map，便会将这个分组里的映射信息存入map里。看到map里的value是“RouteMeta.build(RouteMeta.Type.ACTIVITY,ShowActivity.class,"/show/info","show")”,RouteMeta.build()会返回RouteMeta，这里面便保存着ActivityClass的所有信息。那么我们这个框架，就有了一个功能需求，便是在app进程启动的时候(或者在你开始用路由跳转之前进行初始化都可以)进行框架的初始化，在初始化中拿到映射关系信息，保存在内存里，以便程序运行中可以快速找到路由映射信息实现跳转。下面看具体的初始化代码。<br/>
注：这里我只是讲解大体的思路，不会细致到讲解每一个方法每一行代码的具体作用，跟着我的思路你会明白框架设计的具体细节，每一步要实现的功能是什么，但是精确到方法和每一行代码的作用你还需要仔细研读demo。
```
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        EasyRouter.init(this);
    }
}

public class EasyRouter {

  private static final String TAG = "EasyRouter";
   private static final String ROUTE_ROOT_PAKCAGE = "com.xsm.easyrouter.routes";
   private static final String SDK_NAME = "EaseRouter";
   private static final String SEPARATOR = "_";
   private static final String SUFFIX_ROOT = "Root";

   private static EasyRouter sInstance;
   private static Application mContext;
   private Handler mHandler;

   private EasyRouter() {
       mHandler = new Handler(Looper.getMainLooper());
   }

   public static EasyRouter getsInstance() {
       synchronized (EasyRouter.class) {
           if (sInstance == null) {
               sInstance = new EasyRouter();
           }
       }
       return sInstance;
   }

   public static void init(Application application) {
       mContext = application;
       try {
           loadInfo();
       } catch (Exception e) {
           e.printStackTrace();
           Log.e(TAG, "初始化失败!", e);
       }
   }

   //...
}

```
可以看到，init()方法中调用了loadInfo()方法，而这个loadInfo()便是我们初始化的核心。
```
private static void loadInfo() throws PackageManager.NameNotFoundException, InterruptedException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    //获得所有 apt生成的路由类的全类名 (路由表)
    Set<String> routerMap = ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE);
    for (String className : routerMap) {
        if (className.startsWith(ROUTE_ROOT_PAKCAGE + "." + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {
            //root中注册的是分组信息 将分组信息加入仓库中
            ((IRouteRoot) Class.forName(className).getConstructor().newInstance()).loadInto(Warehouse.groupsIndex);
        }
    }
    for (Map.Entry<String, Class<? extends IRouteGroup>> stringClassEntry : Warehouse.groupsIndex.entrySet()) {
        Log.d(TAG, "Root映射表[ " + stringClassEntry.getKey() + " : " + stringClassEntry.getValue() + "]");
    }

}
```
我们首先通过ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE)得到apt生成的所有实现IRouteRoot接口的类文件集合，通过上面的讲解我们知道，拿到这些类文件便可以得到所有的routerAddress---activityClass映射关系。<br/>
这个ClassUtils.getFileNameByPackageName()方法就是具体的实现了，下面我们看具体的代码：
```
   /**
     * 得到路由表的类名
     * @param context
     * @param packageName
     * @return
     * @throws PackageManager.NameNotFoundException
     * @throws InterruptedException
     */
    public static Set<String> getFileNameByPackageName(Application context, final String packageName)
            throws PackageManager.NameNotFoundException, InterruptedException {
        final Set<String> classNames = new HashSet<>();
        List<String> paths = getSourcePaths(context);
        //使用同步计数器判断均处理完成
        final CountDownLatch countDownLatch = new CountDownLatch(paths.size());
        ThreadPoolExecutor threadPoolExecutor = DefaultPoolExecutor.newDefaultPoolExecutor(paths.size());
        for (final String path : paths) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    DexFile dexFile = null;
                    try {
                        //加载 apk中的dex 并遍历 获得所有包名为 {packageName} 的类
                        dexFile = new DexFile(path);
                        Enumeration<String> dexEntries = dexFile.entries();
                        while (dexEntries.hasMoreElements()) {
                            String className = dexEntries.nextElement();
                            if (!TextUtils.isEmpty(className) && className.startsWith(packageName)) {
                                classNames.add(className);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (null != dexFile) {
                            try {
                                dexFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        //释放一个
                        countDownLatch.countDown();
                    }
                }
            });
        }
        //等待执行完成
        countDownLatch.await();
        return classNames;
    }
```
这个方法会通过开启子线程，去扫描apk中所有的dex，遍历找到所有包名为packageName的类名，然后将类名再保存到classNames集合里。<br/>
List<String> paths = getSourcePaths(context)这句代码会获得所有的apk文件(instant run会产生很多split apk),这个方法的具体实现大家kandemo即可，不再阐述。这里用到了CountDownLatch类，会分path一个文件一个文件的检索，等到所有的类文件都找到后便会返回这个Set<String>集合。所以我们可以知道，初始化时找到这些类文件会有一定的耗时，所以ARouter这里会有一些优化，只会遍历找一次类文件，找到之后就会保存起来，下次app进程启动会检索是否有保存这些文件，如果有就会直接调用保存后的数据去初始化。

##### 第二节 路由调用

&nbsp;&nbsp;&nbsp;&nbsp;每一个加上@Route(path = "/xxx/xxx")注解的Activity都会在编译的时候通过apt生成那些映射关系的类文件，这个我们已经知道了。第一章我们已经知道了跳转的原理，到最后还是通过startActivityc传入intent进行跳转的。那么在调用路由跳转的时候，我们就需要通过传入的路由地址拿到路由的RouteMeta(这里面保存着activityClass)。具体实现看代码：
```
@Route(path = "/main/main")
public class MainActivity extends AppCompatActivity {

  public void startModule1MainActivity(View view) {
    EasyRouter.getsInstance().build("/module1/module1main").navigation();
  }

}
```
在build的时候，传入要跳转的路由地址，build()方法会返回一个Postcard对象，我们称之为跳卡。然后调用Postcard的navigation()方法完成跳转。用过ARouter的对这个跳卡都应该很熟悉吧！Postcard里面保存着跳转的信息。下面我把Postcard类的代码实现粘下来：
```
public class Postcard extends RouteMeta {
    private Bundle mBundle;
    private int flags = -1;
    //新版风格
    private Bundle optionsCompat;
    //老版
    private int enterAnim;
    private int exitAnim;

    //服务
    private IService service;

    public Postcard(String path, String group) {
        this(path, group, null);
    }

    public Postcard(String path, String group, Bundle bundle) {
        setPath(path);
        setGroup(group);
        this.mBundle = (null == bundle ? new Bundle() : bundle);
    }

    public Bundle getExtras() {return mBundle;}

    public int getEnterAnim() {return enterAnim;}

    public int getExitAnim() {return exitAnim;}

    public IService getService() {
        return service;
    }

    public void setService(IService service) {
        this.service = service;
    }

    /**
     * Intent.FLAG_ACTIVITY**
     * @param flag
     * @return
     */
    public Postcard withFlags(int flag) {
        this.flags = flag;
        return this;
    }

    public int getFlags() {
        return flags;
    }

    /**
     * 跳转动画
     *
     * @param enterAnim
     * @param exitAnim
     * @return
     */
    public Postcard withTransition(int enterAnim, int exitAnim) {
        this.enterAnim = enterAnim;
        this.exitAnim = exitAnim;
        return this;
    }

    /**
     * 转场动画
     *
     * @param compat
     * @return
     */
    public Postcard withOptionsCompat(ActivityOptionsCompat compat) {
        if (null != compat) {
            this.optionsCompat = compat.toBundle();
        }
        return this;
    }

    public Postcard withString(@Nullable String key, @Nullable String value) {
        mBundle.putString(key, value);
        return this;
    }


    public Postcard withBoolean(@Nullable String key, boolean value) {
        mBundle.putBoolean(key, value);
        return this;
    }


    public Postcard withShort(@Nullable String key, short value) {
        mBundle.putShort(key, value);
        return this;
    }


    public Postcard withInt(@Nullable String key, int value) {
        mBundle.putInt(key, value);
        return this;
    }


    public Postcard withLong(@Nullable String key, long value) {
        mBundle.putLong(key, value);
        return this;
    }


    public Postcard withDouble(@Nullable String key, double value) {
        mBundle.putDouble(key, value);
        return this;
    }


    public Postcard withByte(@Nullable String key, byte value) {
        mBundle.putByte(key, value);
        return this;
    }


    public Postcard withChar(@Nullable String key, char value) {
        mBundle.putChar(key, value);
        return this;
    }


    public Postcard withFloat(@Nullable String key, float value) {
        mBundle.putFloat(key, value);
        return this;
    }


    public Postcard withParcelable(@Nullable String key, @Nullable Parcelable value) {
        mBundle.putParcelable(key, value);
        return this;
    }


    public Postcard withStringArray(@Nullable String key, @Nullable String[] value) {
        mBundle.putStringArray(key, value);
        return this;
    }


    public Postcard withBooleanArray(@Nullable String key, boolean[] value) {
        mBundle.putBooleanArray(key, value);
        return this;
    }


    public Postcard withShortArray(@Nullable String key, short[] value) {
        mBundle.putShortArray(key, value);
        return this;
    }


    public Postcard withIntArray(@Nullable String key, int[] value) {
        mBundle.putIntArray(key, value);
        return this;
    }


    public Postcard withLongArray(@Nullable String key, long[] value) {
        mBundle.putLongArray(key, value);
        return this;
    }


    public Postcard withDoubleArray(@Nullable String key, double[] value) {
        mBundle.putDoubleArray(key, value);
        return this;
    }


    public Postcard withByteArray(@Nullable String key, byte[] value) {
        mBundle.putByteArray(key, value);
        return this;
    }


    public Postcard withCharArray(@Nullable String key, char[] value) {
        mBundle.putCharArray(key, value);
        return this;
    }


    public Postcard withFloatArray(@Nullable String key, float[] value) {
        mBundle.putFloatArray(key, value);
        return this;
    }


    public Postcard withParcelableArray(@Nullable String key, @Nullable Parcelable[] value) {
        mBundle.putParcelableArray(key, value);
        return this;
    }

    public Postcard withParcelableArrayList(@Nullable String key, @Nullable ArrayList<? extends
            Parcelable> value) {
        mBundle.putParcelableArrayList(key, value);
        return this;
    }

    public Postcard withIntegerArrayList(@Nullable String key, @Nullable ArrayList<Integer> value) {
        mBundle.putIntegerArrayList(key, value);
        return this;
    }

    public Postcard withStringArrayList(@Nullable String key, @Nullable ArrayList<String> value) {
        mBundle.putStringArrayList(key, value);
        return this;
    }

    public Bundle getOptionsBundle() {
        return optionsCompat;
    }

    public Object navigation() {
        return EasyRouter.getsInstance().navigation(null, this, -1, null);
    }

    public Object navigation(Context context) {
        return EasyRouter.getsInstance().navigation(context, this, -1, null);
    }


    public Object navigation(Context context, NavigationCallback callback) {
        return EasyRouter.getsInstance().navigation(context, this, -1, callback);
    }

    public Object navigation(Context context, int requestCode) {
        return EasyRouter.getsInstance().navigation(context, this, requestCode, null);
    }

    public Object navigation(Context context, int requestCode, NavigationCallback callback) {
        return EasyRouter.getsInstance().navigation(context, this, requestCode, callback);
    }


}

```
如果你是一个Android开发，Postcard类里面的东西就不用我再给你介绍了吧！（哈哈）我相信你一看就明白了。我们只介绍一个方法navigation()，他有好几个重载方法，方法里面会调用EasyRouter类的navigation()方法。EaseRouter的navigation()方法，就是跳转的核心了。下面请看：
```
protected Object navigation(Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
    try {
        prepareCard(postcard);
    }catch (NoRouteFoundException e) {
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

```
这个方法里先去调用了prepareCard(postcard)方法，prepareCard(postcard)代码我贴出来，
```
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
```
注意，Warehouse就是专门用来存放路由映射关系的类，这在ARouter里面也是。这段代码Warehouse.routes.get(card.getPath())通过path拿到对应的RouteMeta，这个RouteMeta里面保存了activityClass等信息。继续往下看，如果判断拿到的RouteMeta是空，说明这个路由地址还没有加载到map里面(为了效率，这里是用了懒加载)，只有在第一次用到当前路由地址的时候，会去Warehouse.routes里面拿routeMeta，如果拿到的是空，会根据当前路由地址的group拿到对应的分组，通过反射创建实例，然后调用实例的loadInfo方法，把它里面保存的映射信息添加到Warehouse.routes里面，并且再次调用prepareCard(card)，这时再通过Warehouse.routes.get(card.getPath())就可以顺利拿到RouteMeta了。进入else{}里面，调用了card.setDestination(routeMeta.getDestination())，这个setDestination就是将RouteMeta里面保存的activityClass放入Postcard里面，下面switch代码块可以先不用看，这是实现ARouter中通过依赖注入实现Provider 服务的逻辑，有心研究的同学可以去读一下demo。<br/>
好了，prepareCard()方法调用完成后，我们的postcard里面就保存了activityClass，然后switch (postcard.getType()){}会判断postcard的type为ACTIVITY，然后通过ActivityCompat.startActivity启动Activity。到这里，路由跳转的实现已经讲解完毕了。<br/>

##### 小结

EaseRouter本身只是参照ARouter手动实现的路由框架，并且剔除掉了很多东西，如过滤器等，如果想要用在项目里，建议还是用ARouter更好(毕竟这只是个练手项目，功能也不够全面，当然有同学想对demo扩展后使用那当然更好，遇到什么问题及时联系我)。我的目的是通过自己手动实现来加深对知识，框架构建等等的理解，这里面涉及到的知识点如apt、javapoet和组件化思路、编写框架的思路等。看到这里，如果感觉干货很多，欢迎点个star或分享给更多人。

##### 联系方式

email:xiasem@163.com
