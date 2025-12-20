package com.leeminkan.lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // Works on Classes only
@Retention(RetentionPolicy.SOURCE) // Only needed at compile time, discard at runtime
public @interface MyBuilder {
}