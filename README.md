## EasyRouter

### 简介

&nbsp;&nbsp;&nbsp;&nbsp;最近可能入了魔怔，也可能是闲的蛋疼，自己私下学习了ARouter的原理以及一些APT的知识，为了加深对技术的理解，同时也本着热爱开源的精神为大家提供分享，所以就带着大家强行撸码，分析下ARouter路由原理和Android中APT的使用吧<br/>
&nbsp;&nbsp;&nbsp;&nbsp;EaseRouter项目通过自己手写路由框架(EaseRouter)来为大家一步一步展示阿里Arouter路由原理。本项目搭建组件化开发，得以使EaseRouter得到运用。(注：组件化和路由本身并没有什么联系，但是两个相互没有依赖的组件之间需要通信双向通信，startActivity()是实现不了的，必须需要一个协定的通信的方式，此时类似ARouter和ActivityRouter的框架就派上用场了)。

### 涉及知识点

* Router原理
* APT知识


##### 第一节：组件化原理

&nbsp;&nbsp;&nbsp;&nbsp;本文的重点是对通过APT实现组件路由进行介绍，所以对于组件化的基本知识在文中不会过多阐述，如有同学对组件化有不理解，可以参考网上众多的博客等介绍，然后再阅读demo中的组件化配置进行熟悉。</br>
<img src="http://pcayc3ynm.bkt.clouddn.com/module_1.png" /> <br/>
&nbsp;&nbsp;&nbsp;&nbsp;如图，在组件化中，为了业务逻辑的彻底解耦，同时也为了每个module都可以方便的单独运行和调试，上层的各个module不会进行相互依赖(只有在打正式包的时候才会让app壳module去依赖上层的其他module)，而是共同依赖于base module，base module中会依赖一些公共的第三方库和其他配置。那么在上层的各个module中，如何进行通信呢？<br/>
&nbsp;&nbsp;&nbsp;&nbsp;我们知道，传统的Activity之间通信，通过startActivity(intent)，而在组件化的项目中，上层的module没有依赖关系(即便两个module有依赖关系，也只能是单向的依赖)，那么假如login module中的一个Activity需要启动pay_module中的一个Activity便不能通过startActivity来进行跳转。那么大家想一下还有什么其他办法呢？ 可能有同学会想到隐式跳转，这当然也是一种解决方法，但是一个项目中不可能所有的跳转都是隐式的，这样会多很多过滤配置，而且非常不利于后期维护。如果你非要使用隐式跳转，那么我再提个需求，假如pay_module需要使用login_module中的用户信息，你总不能再启动一次LoginActivity去拿用户信息吧，显然这种方法是不太现实的。那么有没有一种更好的解决办法呢？办法当然是有的。下面看图：<br>
<img src="http://pcayc3ynm.bkt.clouddn.com/module_2.png" /> <br/>
&nbsp;&nbsp;&nbsp;&nbsp;在组件化中，我们通常都会在base_module上层再依赖一个router_module,而这个router_module就是负责各个模块之间页面跳转的。<br/>
&nbsp;&nbsp;&nbsp;&nbsp;在类似ARouter的路由框架中，在每个需要对其他module提供调用的Activity中，都会声明类似下面@Route注解，我们称之为路由地址
```
@Route(path = "/main/main")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}


@Route(path = "/module1/module1main")
public class Module1MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module1_main);
    }
}

```
这个注解有什么用呢，路由框架会在项目的编译器扫描所有添加@Route注解的Activity类，然后将route注解中的path值和Activity.class文件一一对应保存。为了让大家理解，我这里来使用近乎伪代码给大家简单演示一下。
```
//项目编译扫描后通过apt生成如下方法
public HashMap<String, ClassBean> routeInfo() {
    HashMap<String, ClassBean> route = new HashMap<String, ClassBean>();
    route.put("/main/main", MainActivityBean);
    route.put("/module1/module1main", Module1MainActivityBean);
    route.put("/login/login", LoginActivityBean);
}
```
这样我们想在app模块login模块的LoginActivity，那么便只需要调用如此：
```
//不同模块之间启动Activity
public void login(String name, String password) {
    HashMap<String, ClassBean> route = routeInfo();
    ClassBean classBean = route.get("/login/login");
    LoginActivity.class clazz = classBean.getActivity();
    Intent intent = new Intent(this, clazz);
    intent.putExtra("name", name);
    intent.putExtra("password", password);
    startActivity(intent);
}

```
用过ARouter的同学应该知道，用ARouter启动Activity应该是下面这个写法
```
// 2. Jump with parameters
ARouter.getInstance().build("/test/login")
			.withString("password", 666666)
			.withString("name", "小三")
			.navigation();
```
那么ARouter背后的原理是怎么样的呢？实际上它的核心思想也是这么做的，build()方法通过传入要到达页面的路由地址，ARouter会通过它自己生成的路由表找到路由地址对应的LoginActivityBean类，然后new Intent()，当调用ARouter的withString()方法它的内部会调用intent.putExtra(String name, String value)，然后调用navigation()方法进行startActivity(),此时已经得到ActivityBean，便可以通过ActivityBean得到LoginActivity.class类文件，就可以顺利的启动Activity了。


##### 第二节：APT详解

&nbsp;&nbsp;&nbsp;&nbsp;既然已经知道在Activity类上加上@Route注解之后，便可通过apt来生成对应的路由表，那么这节我们就来讲述一下如何通过apt来生成路由表。这节我会拿着demo里面的代码来跟大家详细解述。我们先来了解一下apt吧！<br/>
&nbsp;&nbsp;&nbsp;&nbsp;APT是Annotation Processing Tool的简称,即注解处理工具。它是在编译器对代码中指定的注解进行解析，然后做一些其他处理（如生成新的Java文件）。如我们常用的ButterKnife，其原理就是通过注解处理器在编译期扫描代码，针对我们在代码中加入的@BindView、@OnClick等注解进行扫描处理，然后生成XXX_ViewBinding类，实现了view的绑定。<br/>
&nbsp;&nbsp;&nbsp;&nbsp;简单讲，要通过apt生成我们的路由表，首先第一步需要定义注解
```
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Route {
    /**
     * 路由的路径
     * @return
     */
    String path();

    /**
     * 将路由节点进行分组，可以实现动态加载
     * @return
     */
    String group() default "";

}
```
当项目变得越来越大庞大的时候，为了便于管理和减小路由表的越来越庞大，我们对所有的路由进行分组。在ARouter中会要求路由地址至少需要两级，如"/xx/xx",一个模块下可以有多个分组。如app module，
```
@Route(path = "/main/main")
public class MainActivity extends AppCompatActivity {}

@Route(path = "/main/main2")
public class Main2Activity extends AppCompatActivity {}

@Route(path = "/show/info")
public class ShowActivity extends AppCompatActivity {}

```
那么在apt编译的时候，我们将会生成EaseRouter_Root_app文件和EaseRouter_Group_main、EaseRouter_Group_show文件，EaseRouter_Root_app文件对应于app module，里面会记录app下的所有分组，EaseRouter_Group_main、EaseRouter_Group_show文件分别记载着此分组的所有路由地址。<br/>
本demo中生成类如下所示，先不要管这些类是怎么生成的，仔细看类的内容
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
大家会看到生成的类分别实现了IRouteRoot和IRouteGroup接口，并且实现了loadInto()方法，而loadInto方法通过传入一个特定类型的map就能把分组信息放入map里。这两个接口是干嘛的我们先搁置，继续往下看<br/>
好了，如果我们在login_module中想启动MainActivity类，首先，我们已知MainActivity类的路由地址是"/main/main"，第一个"/main"代表组名，那么我们岂不是可以像下面这样调用去得到MainActivity类文件，然后startActivity。这里的RouteMeta只是存有Activity class文件的封装类。
```
public void test() {
    EaseRouter_Root_app rootApp = new EaseRouter_Root_app();
    HashMap<String, Class<? extends IRouteGroup>> rootMap = new HashMap<>();
    rootApp.loadInto(rootMap);

    //得到/main分组
    Class<? extends IRouteGroup> aClass = rootMap.get("main");
    try {
        HashMap<String, RouteMeta> groupMap = new HashMap<>();
        aClass.newInstance().loadInto(groupMap);
        //得到MainActivity
        RouteMeta main = groupMap.get("main");
        Class<?> mainActivityClass = main.getDestination();

        Intent intent = new Intent(this, mainActivityClass);
        startActivity(intent);
    } catch (InstantiationException e) {
        e.printStackTrace();
    } catch (IllegalAccessException e) {
        e.printStackTrace();
    }

}
```


&nbsp;&nbsp;&nbsp;&nbsp;第二步：定义注解处理器，用来在编译期扫描加入@Route注解的类，然后做处理。<br/>
这一步，也是apt最核心的一步了，新建RouterProcessor 继承自 AbstractProcessor,然后实现process方法，此时我们需要为RouterProcessor指明它需要处理什么注解，这里引入一个google开源的自动注册工具AutoService，如下依赖(也可以手动进行注册，不过略微麻烦)：
```
implementation 'com.google.auto.service:auto-service:1.0-rc2'
```
这个工具可以通过添加注解来为RouterProcessor指定它需要的配置，如下所示
```
@AutoService(Processor.class)
public class RouterProcessor extends AbstractProcessor {
}
```
完整的RouterProcessor注解处理器配置如下：
```
@AutoService(Processor.class)
/**
  处理器接收的参数 替代 {@link AbstractProcessor#getSupportedOptions()} 函数
 */
@SupportedOptions(Constant.ARGUMENTS_NAME)
/**
 * 指定使用的Java版本 替代 {@link AbstractProcessor#getSupportedSourceVersion()} 函数
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
/**
 * 注册给哪些注解的  替代 {@link AbstractProcessor#getSupportedAnnotationTypes()} 函数
 */
@SupportedAnnotationTypes(Constant.ANNOTATION_TYPE_ROUTE)

public class RouterProcessor extends AbstractProcessor {
    /**
     * key:组名 value:类名
     */
    private Map<String, String> rootMap = new TreeMap<>();
    /**
     * 分组 key:组名 value:对应组的路由信息
     */
    private Map<String, List<RouteMeta>> groupMap = new HashMap<>();

    /**
     * 节点工具类 (类、函数、属性都是节点)
     */
    private Elements elementUtils;

    /**
     * type(类信息)工具类
     */
    private Types typeUtils;

    /**
     * 文件生成器 类/资源
     */
    private Filer filerUtils;

    private String moduleName;

    private Log log;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //获得apt的日志输出
        log = Log.newLog(processingEnvironment.getMessager());
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        filerUtils = processingEnvironment.getFiler();

        //参数是模块名 为了防止多模块/组件化开发的时候 生成相同的 xx$$ROOT$$文件
        Map<String, String> options = processingEnvironment.getOptions();
        if (!Utils.isEmpty(options)) {
            moduleName = options.get(Constant.ARGUMENTS_NAME);
        }
        if (Utils.isEmpty(moduleName)) {
            throw new RuntimeException("Not set processor moudleName option !");
        }
        log.i("init RouterProcessor " + moduleName + " success !");
    }

    /**
     *
     * @param set 使用了支持处理注解的节点集合
     * @param roundEnvironment 表示当前或是之前的运行环境,可以通过该对象查找找到的注解。
     * @return true 表示后续处理器不会再处理(已经处理)
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (!Utils.isEmpty(set)) {
            //被Route注解的节点集合
            Set<? extends Element> rootElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
            if (!Utils.isEmpty(rootElements)) {
                processorRoute(rootElements);
            }
            return true;
        }
        return false;
    }

    //...

}

```
每个组件module都会在build.gradle下指定如下信息：
```
javaCompileOptions {
            annotationProcessorOptions {
                arguments = [moduleName: project.getName()]
            }
        }
```
我们通过@SupportedOptions(Constant.ARGUMENTS_NAME)拿到每个module的名字，用来生成对应的module存放路由信息的文件名。<br/>
@SupportedAnnotationTypes(Constant.ANNOTATION_TYPE_ROUTE)指定了需要处理的注解的路径信息,在此就是Route.class的路径地址。

我们实现init方法，拿到log apt日志输出工具用以输出apt日志信息,然后通过以下代码得到每个module配置的模块名
```
//参数是模块名 为了防止多模块/组件化开发的时候 生成相同的 xx$$ROOT$$文件
Map<String, String> options = processingEnvironment.getOptions();
if (!Utils.isEmpty(options)) {
    moduleName = options.get(Constant.ARGUMENTS_NAME);
}
if (Utils.isEmpty(moduleName)) {
    throw new RuntimeException("Not set processor moudleName option !");
}
```
&nbsp;&nbsp;&nbsp;&nbsp; 第三步，在process()方法里开始生成EaseRouter_Route_moduleName类文件和EaseRouter_Group_moduleName文件。这里在process()里生成文件用javapoet，这是squareup公司开源的一个库，通过调用它的api，可以很方便的生成java文件，在含有注解处理器的module中引入依赖如下：
```
implementation 'com.squareup:javapoet:1.7.0'
```
好了，我们终于可以生成文件了,在process()方法里有如下代码，
```
if (!Utils.isEmpty(set)) {
    //被Route注解的节点集合
    Set<? extends Element> rootElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
    if (!Utils.isEmpty(rootElements)) {
        processorRoute(rootElements);
    }
    return true;
}
return false;
```
set就是扫描得到的支持处理注解的节点集合，然后得到rootElements，即被@Route注解的节点集合，此时就可以调用
processorRoute(rootElements)方法去生成文件了。
```
private void processorRoute(Set<? extends Element> rootElements) {
    //获得Activity这个类的节点信息
    TypeElement activity = elementUtils.getTypeElement(Constant.ACTIVITY);
    TypeElement service = elementUtils.getTypeElement(Constant.ISERVICE);
    for (Element element : rootElements) {
        RouteMeta routeMeta;
        //类信息
        TypeMirror typeMirror = element.asType();
        log.i("Route class:" + typeMirror.toString());
        Route route = element.getAnnotation(Route.class);
        if (typeUtils.isSubtype(typeMirror, activity.asType())) {
            routeMeta = new RouteMeta(RouteMeta.Type.ACTIVITY, route, element);
        } else if (typeUtils.isSubtype(typeMirror, service.asType())) {
            routeMeta = new RouteMeta(RouteMeta.Type.ISERVICE, route, element);
        } else {
            throw new RuntimeException("Just support Activity or IService Route: " + element);
        }
        categories(routeMeta);
    }
    TypeElement iRouteGroup = elementUtils.getTypeElement(Constant.IROUTE_GROUP);
    TypeElement iRouteRoot = elementUtils.getTypeElement(Constant.IROUTE_ROOT);

    //生成Group记录分组表
    generatedGroup(iRouteGroup);

    //生成Root类 作用：记录<分组，对应的Group类>
    generatedRoot(iRouteRoot, iRouteGroup);
}

```
上面提到生成的root文件和group文件分别实现了IRouteRoot和IRouteGroup接口，就是通过下面这两行文件指定了生成的文件实现了这两个接口。
```
TypeElement iRouteGroup = elementUtils.getTypeElement(Constant.IROUTE_GROUP);
TypeElement iRouteRoot = elementUtils.getTypeElement(Constant.IROUTE_ROOT);
```
generatedGroup(iRouteGroup)和generatedRoot(iRouteRoot, iRouteGroup)就是生成上面提到的EaseRouter_Root_app和EaseRouter_Group_main等的方法，代码太过，我粘出一个供大家参考，其实思路都是一样的，大家可以后续在demo里详细分析，这里我只是讲解核心的思想。
```
/**
 * 生成Root类  作用：记录<分组，对应的Group类>
 * @param iRouteRoot
 * @param iRouteGroup
 */
private void generatedRoot(TypeElement iRouteRoot, TypeElement iRouteGroup) {
    //创建参数类型 Map<String,Class<? extends IRouteGroup>> routes>
    //Wildcard 通配符
    ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
            ClassName.get(Map.class),
            ClassName.get(String.class),
            ParameterizedTypeName.get(
                    ClassName.get(Class.class),
                    WildcardTypeName.subtypeOf(ClassName.get(iRouteGroup))
            ));
    //参数 Map<String,Class<? extends IRouteGroup>> routes> routes
    ParameterSpec parameter = ParameterSpec.builder(parameterizedTypeName, "routes").build();
    //函数 public void loadInfo(Map<String,Class<? extends IRouteGroup>> routes> routes)
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constant.METHOD_LOAD_INTO)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(parameter);
    //函数体
    for (Map.Entry<String, String> entry : rootMap.entrySet()) {
        methodBuilder.addStatement("routes.put($S, $T.class)", entry.getKey(), ClassName.get(Constant.PACKAGE_OF_GENERATE_FILE, entry.getValue()));
    }
    //生成$Root$类
    String className = Constant.NAME_OF_ROOT + moduleName;
    TypeSpec typeSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName.get(iRouteRoot))
            .addModifiers(Modifier.PUBLIC)
            .addMethod(methodBuilder.build())
            .build();
    try {
        JavaFile.builder(Constant.PACKAGE_OF_GENERATE_FILE, typeSpec).build().writeTo(filerUtils);
        log.i("Generated RouteRoot：" + Constant.PACKAGE_OF_GENERATE_FILE + "." + className);
    } catch (IOException e) {
        e.printStackTrace();
    }
}

```
可以看到，生成Java文件其实并不神秘，只是要熟悉javapoet的api就够了。

[下一篇，router架构编写实现](https://github.com/Xiasm/EasyRouter/blob/master/README2.md)
