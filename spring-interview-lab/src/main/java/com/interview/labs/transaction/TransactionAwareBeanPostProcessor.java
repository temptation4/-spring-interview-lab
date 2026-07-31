package com.interview.labs.transaction;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Teaching BeanPostProcessor — NOT the one Spring itself uses to build transaction
// proxies (that's an internal InfrastructureAdvisorAutoProxyCreator). This one just
// makes "BeanPostProcessor inspects every bean via reflection at startup" a real,
// observable line in the console instead of a diagram.
@Component
public class TransactionAwareBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        // If another BeanPostProcessor already wrapped this bean in a CGLIB proxy,
        // the @Transactional-annotated methods live on the superclass, not on the
        // proxy's own declared methods.
        Class<?> targetClass = bean.getClass();
        if (targetClass.getName().contains("$$")) {
            targetClass = targetClass.getSuperclass();
        }

        List<String> transactionalMethods = Arrays.stream(targetClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Transactional.class))
                .map(Method::getName)
                .collect(Collectors.toList());

        if (!transactionalMethods.isEmpty()) {
            System.out.println("[TransactionAwareBeanPostProcessor] " + beanName
                    + " has @Transactional methods: " + transactionalMethods);
        }

        return bean;
    }
}
