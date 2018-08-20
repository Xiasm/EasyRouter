package com.xsm.easy.compiler.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.xsm.easy.annotation.Extra;
import com.xsm.easy.compiler.utils.Constant;
import com.xsm.easy.compiler.utils.LoadExtraBuilder;
import com.xsm.easy.compiler.utils.Log;
import com.xsm.easy.compiler.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Author: 夏胜明
 * Date: 2018/8/20 0020
 * Email: xiasem@163.com
 * Description:
 */
@AutoService(Processor.class)
@SupportedOptions(Constant.ARGUMENTS_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({Constant.ANN_TYPE_EXTRA})
public class ExtraProcessor extends AbstractProcessor {

    /**
     * 节点工具类 (类、函数、属性都是节点)
     */
    private Elements elementUtils;

    /**
     * type(类信息)工具类
     */
    private Types typeUtils;
    /**
     * 类/资源生成器
     */
    private Filer filerUtils;

    /**
     * 记录所有需要注入的属性 key:类节点 value:需要注入的属性节点集合
     */
    private Map<TypeElement, List<Element>> parentAndChild = new HashMap<>();
    private Log log;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //获得apt的日志输出
        log = Log.newLog(processingEnvironment.getMessager());
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        filerUtils = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (!Utils.isEmpty(set)) {
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Extra.class);
            if (!Utils.isEmpty(elements)) {
                try {
                    categories(elements);
                    generateAutoWired();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    private void generateAutoWired() throws IOException {
        TypeMirror typeActivity = elementUtils.getTypeElement(Constant.ACTIVITY).asType();
        TypeElement iExtra = elementUtils.getTypeElement(Constant.IEXTRA);

        if (!Utils.isEmpty(parentAndChild)) {
            // 参数 Object target
            ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();
            for (Map.Entry<TypeElement, List<Element>> entry : parentAndChild.entrySet()) {
                TypeElement rawClassElement = entry.getKey();
                if (!typeUtils.isSubtype(rawClassElement.asType(), typeActivity)) {
                    throw new RuntimeException("just support activity filed: " + rawClassElement);
                }
                //封装的函数生成类
                LoadExtraBuilder loadExtra = new LoadExtraBuilder(objectParamSpec);
                loadExtra.setElementUtils(elementUtils);
                loadExtra.setTypeUtils(typeUtils);
                ClassName className = ClassName.get(rawClassElement);
                loadExtra.injectTarget(className);
                //遍历属性
                for (int i = 0; i < entry.getValue().size(); i++) {
                    Element element = entry.getValue().get(i);
                    loadExtra.buildStatement(element);
                }

                // 生成java类名
                String extraClassName = rawClassElement.getSimpleName() + Constant.NAME_OF_EXTRA;
                // 生成 XX$$Autowired
                JavaFile.builder(className.packageName(), TypeSpec.classBuilder(extraClassName)
                        .addSuperinterface(ClassName.get(iExtra))
                        .addModifiers(PUBLIC).addMethod(loadExtra.build()).build())
                        .build().writeTo(filerUtils);
                log.i("Generated Extra: " + className.packageName() + "." + extraClassName);
            }
        }
    }

    /**
     * 记录需要生成的类与属性
     *
     * @param elements
     * @throws IllegalAccessException
     */
    private void categories(Set<? extends Element> elements) {
        for (Element element : elements) {
            //获得父节点 (类)
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            if (parentAndChild.containsKey(enclosingElement)) {
                parentAndChild.get(enclosingElement).add(element);
            } else {
                List<Element> childs = new ArrayList<>();
                childs.add(element);
                parentAndChild.put(enclosingElement, childs);
            }
        }
    }


}
