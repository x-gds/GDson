package com.gdson;

import com.google.gson.FieldNamingPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface GDson {
    FieldNamingPolicy fieldNamingPolicy() default FieldNamingPolicy.IDENTITY;

    boolean serializeNulls() default false;
}
