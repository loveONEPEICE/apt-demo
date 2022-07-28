package com.example.process;

import com.example.annotation.BindView;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * @author lhl
 */
@AutoService(Processor.class) //https://github.com/google/auto
public class MyBindViewProcessor extends AbstractProcessor {
    /**
     * Element操作类
     */
    private Elements mElementUtils;
    /**
     * 类信息工具类
     */
    private Types mTypeUtils;
    /**
     * 日志工具类
     */
    private Messager mMessager;
    /**
     * 文件创建工具类
     */
    private Filer mFiler;

    /**
     * 节点信息缓存
     */
    private Map<String, List<NodeInfo>> mCache = new HashMap<>();

    /**
     * 注解处理器的初始化阶段，可以通过ProcessingEnvironment来获取一些帮助我们来处理注解的工具类
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mElementUtils = processingEnv.getElementUtils();
        mTypeUtils = processingEnv.getTypeUtils();
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
        String initText = " init processor!";
        System.out.println("initText = " + initText);
        mMessager.printMessage(Diagnostic.Kind.NOTE, initText);
    }

    /**
     * 指明有哪些注解需要被扫描到，返回注解的全路径（包名+类名）
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(BindView.class.getCanonicalName());
    }

    /**
     * 用来指定当前正在使用的Java版本，一般返回SourceVersion.latestSupported()表示最新的java版本即可
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 核心方法，注解的处理和生成代码都是在这个方法中完成
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        // 获取所有 @BindView 节点
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(BindView.class);
        if (elements == null || elements.isEmpty()) {
            return false;
        }

        // 遍历节点
        for (Element element : elements) {
            // 获取节点包信息
            String packageName = mElementUtils.getPackageOf(element).getQualifiedName().toString();
            // 获取节点类信息，由于 @BindView 作用于成员属性上，所以这里使用 getEnclosingElement() 获取父节点信息
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            mMessager.printMessage(Diagnostic.Kind.NOTE, "qualifiedName：" + enclosingElement.getQualifiedName().toString());
            String className = enclosingElement.getSimpleName().toString();
            // 获取节点类型
            String typeName = element.asType().toString();
            // 获取节点标记的属性名称
            String nodeName = element.getSimpleName().toString();
            // 获取注解
            BindView annotation = element.getAnnotation(BindView.class);
            // 获取注解的值
            int value = annotation.value();
            // 打印
            mMessager.printMessage(Diagnostic.Kind.NOTE, "packageName：" + packageName);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "className：" + className);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "typeName：" + typeName);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "nodeName：" + nodeName);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "value：" + value);

            // 缓存KEY
            String key = packageName + "." + className;
            // 缓存节点信息
            List<NodeInfo> nodeInfos = mCache.get(key);
            if (nodeInfos == null) {
                nodeInfos = new ArrayList<>();
                nodeInfos.add(new NodeInfo(packageName, className, typeName, nodeName, value));
                // 缓存
                mCache.put(key, nodeInfos);
            } else {
                nodeInfos.add(new NodeInfo(packageName, className, typeName, nodeName, value));
            }
        }

        // 判断临时缓存是否不为空
        if (!mCache.isEmpty()) {
            // 遍历临时缓存文件
            for (Map.Entry<String, List<NodeInfo>> stringListEntry : mCache.entrySet()) {
                try {
                    List<NodeInfo> value = stringListEntry.getValue();
                    // 原生
//                    createFile(value);
                    // 通过 POET 创建文件
                    createFileByJavaPoet(value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    private void createFile(List<NodeInfo> infos) throws IOException {
        NodeInfo info = infos.get(0);

        String className = info.getClassName();
        // 生成的文件名(类名)
        String currentClassName = className + "$$ViewBinding";
        StringBuilder bindViewFile = new StringBuilder();
        String packageName = info.getPackageName();
        String newline = "\n";
        bindViewFile.append("package ").append(packageName).append(";").append(newline);
        bindViewFile.append("public class ").append(currentClassName).append(" {").append(newline);
        String target = "target";
        bindViewFile.append("public static void ")
                .append("bind(")
                .append(className).append(" ").append(target).append(") {").append(newline);
        for (int i = 0; i < infos.size(); i++) {
            NodeInfo nodeInfo = infos.get(i);
            bindViewFile.append(target).append(".").append(nodeInfo.getNodeName()).append(" = (")
                    .append(nodeInfo.getTypeName()).append(")").append(target).append(".").append("findViewById(")
                    .append(nodeInfo.getValue()).append(");").append(newline);

        }
        bindViewFile.append(newline).append("}").append(newline).append("}");
        JavaFileObject sourceFile = mFiler.createSourceFile(currentClassName);
        OutputStream outputStream = sourceFile.openOutputStream();
        String viewBindingJava = bindViewFile.toString();
        System.out.println("viewBindingJava = \n" + viewBindingJava);
        outputStream.write(viewBindingJava.getBytes());
        outputStream.close();

    }
    /**
     * 创建文件，自动生成代码
     */
    private void createFileByJavaPoet(List<NodeInfo> infos) throws IOException {
        NodeInfo info = infos.get(0);

        // 生成的文件名(类名)
        String className = info.getClassName() + "$$ViewBinding";

        // 方法参数
        ParameterSpec parameterSpec = ParameterSpec.builder(
                        ClassName.get(info.getPackageName(), info.getClassName()), "target")
                .build();


        // 方法
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("bind")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(parameterSpec)
                .returns(void.class);

        // 给方法添加代码块
        for (NodeInfo nodeInfo : infos) {
            // target.textView = (TextView) target.findViewByID(R.id.text_view);
            methodSpecBuilder.addStatement("target.$L = ($L)target.findViewById($L)",
                    nodeInfo.getNodeName(),
                    nodeInfo.getTypeName(),
                    nodeInfo.getValue());
        }

        // 类
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(methodSpecBuilder.build())
                .build();

        // 生成文件
        JavaFile.builder(info.getPackageName(), typeSpec)
                .build()
                .writeTo(mFiler);
    }
}