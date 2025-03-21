package com.xin.xinpicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)  //注解生效时间
public @interface AuthCheck {
    /**
     * 必须具有某个角色
     */
    String mustRole() default "";
}
