## EasyRouter

### 简介

&nbsp;&nbsp;&nbsp;&nbsp;最近可能入了魔怔，也可能是闲的蛋疼，自己私下学习了ARouter的原理以及一些APT的知识，为了加深对技术的理解，同时也本着热爱开源的精神为大家提供分享，所以就带着大家强行撸码，分析下ARouter路由原理和Android中APT的使用吧</br>
&nbsp;&nbsp;&nbsp;&nbsp;EaseRouter项目通过自己手写路由框架(EaseRouter)来为大家一步一步展示阿里Arouter路由原理。本项目搭建组件化开发，得以使EaseRouter得到运用。(注：组件化和路由本身并没有什么联系，但是两个相互没有依赖的组件之间需要通信双向通信，startActivity()是实现不了的，必须需要一个协定的通信的方式，此时类似ARouter和ActivityRouter的框架就派上用场了)。

### 涉及知识点

* Router原理
* APT知识


##### 第一节：组件化原理

&nbsp;&nbsp;&nbsp;&nbsp;本文的重点是对通过APT实现组件路由进行介绍，所以对于组件化的基本知识在文中不会过多阐述，如有同学对组件化有不理解，可以参考网上众多的博客等介绍，然后再阅读demo中的组件化配置进行熟悉。</br>
![avatar](D:\AtomWorkSpace\image\module_1.png) </br>
&nbsp;&nbsp;&nbsp;&nbsp;如图，在组件化中，为了业务逻辑的彻底解耦，同时也为了每个module都可以方便的单独运行和调试，上层的各个module不会进行相互依赖(只有在打正式包的时候才会让app壳module去依赖上层的其他module)，而是共同依赖于base module，base module中会依赖一些公共的第三方库和其他配置。那么在上层的各个module中，如何进行通信呢？</br>
&nbsp;&nbsp;&nbsp;&nbsp;我们知道，传统的Activity之间通信，通过startActivity(intent)，而在组件化的项目中，上层的module没有依赖关系(即便两个module有依赖关系，也只能是单向的依赖)，那么假如login module中的一个Activity需要启动pay_module中的一个Activity便不能通过startActivity来进行跳转。那么大家想一下还有什么其他办法呢？ 可能有同学会想到隐式跳转，这当然也是一种解决方法，但是一个项目中不可能所有的跳转都是隐式的，这样会多很多过滤配置，而且非常不利于后期维护。如果你非要使用隐式跳转，那么我再提个需求，假如pay_module需要使用login_module中的用户信息，你总不能再启动一次LoginActivity去拿用户信息吧，显然这种方法是不太现实的。有同学可能会想到反射，但是在一个项目中页面之间的跳转大量使用反射对性能的损耗大概让很多人望而生畏了吧。那么有没有一种更好的解决办法呢？办法当然是有的。下面看图：</br>
![avatar](D:\AtomWorkSpace\image\module_2.png) </br>
&nbsp;&nbsp;&nbsp;&nbsp;在组件化中，我们通常都会在base_module上层再依赖一个router_module,而这个router_module就是负责各个模块之间页面跳转的。</br>
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
这个注解有什么用呢，路由框架会在项目的编译器扫描所有添加@Route注解的Activity类，然后将route注解中的path值和Activity.class文件一一对应保存。为了让大家理解，我这里来简单演示一下。
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

&nbsp;&nbsp;&nbsp;&nbsp;既然已经知道在Activity类上加上@Route注解之后，便可通过apt来生成对应的路由表，那么这节我们就来讲述一下如何通过apt来生成路由表。这节我会拿着demo里面的代码来跟大家详细解述。我们先来了解一下apt吧！</br>
&nbsp;&nbsp;&nbsp;&nbsp;APT是Annotation Processing Tool的简称,即注解处理工具。它是在编译器对代码中指定的注解进行解析，然后做一些其他处理（如生成新的Java文件）。如我们常用的ButterKnife，其原理就是通过注解处理器在编译期扫描代码，针对我们在代码中加入的@BindView、@OnClick等注解进行扫描处理，然后生成XXX_ViewBinding类，实现了view的绑定。</br>
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
当项目变得越来越大庞大的时候，为了便于管理和减小路由表的越来越庞大，我们对所有的路由进行分组。如一个模块下的所有路由分为一组，对应我们上节中"/module1/module1main"，module1就是一个组。
&nbsp;&nbsp;&nbsp;&nbsp;第二步：定义注解处理器，用来在编译期扫描用到注解的类，然后做处理。

