package com.gcgo.framework.annotation;

import java.lang.annotation.*;

@Documented//表明这个注解应该被 javadoc工具记录
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Security {
    String[] value() default "";
}
