package com.interview.labs.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Before("execution(* com.interview.labs.jpa.service.*.*(..))")
    public void before(JoinPoint joinPoint) {

        System.out.println("Before Method : "
                + joinPoint.getSignature().getName());

    }

    @After("execution(* com.interview.labs.jpa.service.*.*(..))")
    public void after(JoinPoint joinPoint) {

        System.out.println("After Method : "
                + joinPoint.getSignature().getName());

    }

}
