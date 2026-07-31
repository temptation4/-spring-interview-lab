package com.interview.labs.aop;

import com.interview.labs.transaction.TransactionContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class LoggingAspect {

    @Before("execution(* com.interview.labs.jpa.service.*.*(..))")
    public void before(JoinPoint joinPoint) {

        System.out.println("Before Method [" + TransactionContextHolder.get() + "] : "
                + joinPoint.getSignature().getName());

    }

    @After("execution(* com.interview.labs.jpa.service.*.*(..))")
    public void after(JoinPoint joinPoint) {

        System.out.println("After Method [" + TransactionContextHolder.get() + "] : "
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

    // ThreadLocal demo: a correlation id is bound to this thread for the
    // duration of the call so before()/after() can read it without it being
    // passed as a parameter — the same technique TransactionSynchronizationManager
    // uses to bind a Connection. clear() runs in `finally` so a pooled Tomcat
    // thread never carries a stale id into the next request.
    @Around("execution(* com.interview.labs.jpa.service.*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        TransactionContextHolder.set(UUID.randomUUID().toString().substring(0, 8));

        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed(); // without this call, the target method never runs

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Around : " + pjp.getSignature().getName()
                    + " took " + elapsed + "ms");

            return result;
        } finally {
            TransactionContextHolder.clear();
        }
    }

}
