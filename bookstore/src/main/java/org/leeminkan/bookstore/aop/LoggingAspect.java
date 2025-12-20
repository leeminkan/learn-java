package org.leeminkan.bookstore.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component // Ensures Spring manages this class
public class LoggingAspect {

    // This method is the Advice (the action)
    @Before("execution(* org.leeminkan.bookstore.service.*.*(..))")
    public void logMethodCall(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();

        System.out.println("AOP: Executing method: " + className + "." + methodName);
    }
}