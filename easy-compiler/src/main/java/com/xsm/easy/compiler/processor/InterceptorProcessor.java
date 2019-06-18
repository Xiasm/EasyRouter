package com.xsm.easy.compiler.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.xsm.easy.annotation.Interceptor;
import com.xsm.easy.compiler.utils.Constant;
import com.xsm.easy.compiler.utils.Log;
import com.xsm.easy.compiler.utils.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * @author: luoxiaohui
 * @date: 2019-05-23 19:48
 * @desc: 拦截器
 */
@AutoService(Processor.class)
@SupportedOptions(Constant.ARGUMENTS_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes(Constant.ANNOTATION_TYPE_INTERCEPTOR)
public class InterceptorProcessor extends AbstractProcessor {

    private Map<Integer, Element> interceptors = new HashMap<>();
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

    private TypeMirror iInterceptor;
    private Log log;
    private String moduleName = "";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        log = Log.newLog(processingEnv.getMessager());
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filerUtils = processingEnv.getFiler();
        iInterceptor = elementUtils.getTypeElement(Constant.IINTERCEPTOR).asType();

        Map<String, String> options = processingEnv.getOptions();
        if (!Utils.isEmpty(options)) {
            moduleName = options.get(Constant.ARGUMENTS_NAME);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!Utils.isEmpty(annotations)) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Interceptor.class);
            try {
                parseInterceptor(elements);
            } catch (Exception e) {
                log.i(e.getMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * 解析拦截器
     *
     * @author luoxiaohui
     * @createTime 2019-05-23 20:12
     */
    private void parseInterceptor(Set<? extends Element> elements) throws IOException {
        if (!Utils.isEmpty(elements)) {

            for (Element element : elements) {
                if (verify(element)) {

                    Interceptor interceptor = element.getAnnotation(Interceptor.class);
                    interceptors.put(interceptor.priority(), element);
                }
            }

            TypeElement iInterceptor = elementUtils.getTypeElement(Constant.IINTERCEPTOR);
            TypeElement iInterceptorGroup = elementUtils.getTypeElement(Constant.IINTERCEPTOR_GROUP);
            /**
             * Map<String, Class<? extends IInterceptor></>>
             */
            ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Integer.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(iInterceptor))
                    )
            );
            /**
             * 参数+变量名
             * Map<String, Class<? extends IInterceptor>> interceptors
             */
            ParameterSpec parameterSpec = ParameterSpec.builder(parameterizedTypeName, "interceptors").build();
            /**
             * 构建方法
             * public void loadInto(Map<String, Class<? extends IInterceptor>> interceptors){}
             */
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constant.METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(parameterSpec);
            if (!interceptors.isEmpty() && interceptors.size() > 0) {
                /**
                 * 构建方法体中的语句
                 */
                for (Map.Entry<Integer, Element> entry : interceptors.entrySet()) {
                    methodBuilder.addStatement("interceptors.put(" + entry.getKey() + ", $T.class)",
                            ClassName.get((TypeElement) entry.getValue()));
                }
            }
            /**
             * 将文件写入磁盘中
             * 路径是在app/build/source/api/debug/PACKAGE_OF_GENERATE_FILE下面
             */
            JavaFile.builder(Constant.PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(Constant.NAME_OF_INTERCEPTOR + moduleName)
                            .addModifiers(Modifier.PUBLIC)
                            .addMethod(methodBuilder.build())
                            .addSuperinterface(ClassName.get(iInterceptorGroup))
                            .build()
            ).build().writeTo(filerUtils);
        }
    }

    /**
     * 验证节点是否含有拦截器注解
     *
     * @author luoxiaohui
     * @createTime 2019-05-23 20:21
     */
    private boolean verify(Element element) {

        Interceptor interceptor = element.getAnnotation(Interceptor.class);
        return interceptor != null && ((TypeElement) element).getInterfaces().contains(iInterceptor);
    }
}
