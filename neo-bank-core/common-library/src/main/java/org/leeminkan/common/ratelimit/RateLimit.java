package org.leeminkan.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    // Maximum requests allowed in the window
    long limit();

    // The time window duration
    long duration();

    // The time unit (Seconds, Minutes)
    TimeUnit unit() default TimeUnit.SECONDS;

    // A unique key prefix to separate limits for different endpoints
    // e.g. "transfer-api", "account-create"
    String key() default "default";
}