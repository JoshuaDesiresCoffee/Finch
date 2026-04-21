package io.github.finch.core;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Database {
    String url();
    String user() default "";
    String password() default "";
}