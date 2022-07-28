package com.example.butterknife_java;


import java.lang.reflect.Method;

/**
 * @author lhl
 */
public class ButterKnife {

    public static void bind(Object target) {
        try {
            Class<?> clazz = target.getClass();
            // 反射获取apt生成的指定类
            Class<?> bindViewClass = Class.forName(clazz.getName() + "$$ViewBinding");
            // 获取它的方法
            Method method = bindViewClass.getMethod("bind", clazz);
            // 执行方法
            method.invoke(bindViewClass.newInstance(), target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}