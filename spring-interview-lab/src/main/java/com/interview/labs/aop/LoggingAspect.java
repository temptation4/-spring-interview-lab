package com.interview.labs.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
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

    @AfterReturning(pointcut = "execution(* com.interview.labs.jpa.service.*.*(..))", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {

        System.out.println("After Returning : " + joinPoint.getSignature().getName()
                + " -> " + result);

    }

    @AfterThrowing(pointcut = "execution(* com.interview.labs.jpa.service.*.*(..))", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Exception ex) {

        System.out.println("After Throwing : " + joinPoint.getSignature().getName()
                + " -> " + ex.getMessage());

    }

    @Around("execution(* com.interview.labs.jpa.service.*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        long start = System.currentTimeMillis();

        Object result = pjp.proceed(); // without this call, the target method never runs

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("Around : " + pjp.getSignature().getName()
                + " took " + elapsed + "ms");

        return result;
    }

}
