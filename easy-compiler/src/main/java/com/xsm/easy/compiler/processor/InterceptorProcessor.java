package com.xsm.easy.compiler.processor;

import com.google.auto.service.AutoService;
import com.xsm.easy.annotation.Interceptor;
import com.xsm.easy.compiler.utils.Constant;
import com.xsm.easy.compiler.utils.Log;
import com.xsm.easy.compiler.utils.Utils;

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

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        log = Log.newLog(processingEnv.getMessager());
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filerUtils = processingEnv.getFiler();
        iInterceptor = elementUtils.getTypeElement(Constant.IINTERCEPTOR).asType();
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
    private void parseInterceptor(Set<? extends Element> elements) {
        if (!Utils.isEmpty(elements)) {

            TypeElement iInterceptor = elementUtils.getTypeElement(Constant.IINTERCEPTOR);
            TypeElement iInterceptorGroup = elementUtils.getTypeElement(Constant.IINTERCEPTOR_GROUP);
            
        }
    }

    /**
     * 验证节点是否含有拦截器注解
     * @author luoxiaohui
     * @createTime 2019-05-23 20:21
     */
    private boolean verify(Element element) {

        Interceptor interceptor = element.getAnnotation(Interceptor.class);
        return interceptor != null && ((TypeElement) element).getInterfaces().contains(iInterceptor);
    }
}
