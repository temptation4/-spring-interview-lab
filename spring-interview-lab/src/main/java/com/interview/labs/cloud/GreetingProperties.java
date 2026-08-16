package com.interview.labs.cloud;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * {@code @RefreshScope} means this bean gets re-created (re-reading its {@code @Value}
 * fields) whenever {@code POST /actuator/refresh} is called, without restarting the app —
 * the point of an externalized Config Server: change a property centrally, push a refresh,
 * every subscribed instance picks it up.
 */
@RefreshScope
@Component
public class GreetingProperties {

    @Value("${lab.greeting:Hello from the default, no-Config-Server value}")
    private String greeting;

    public String getGreeting() {
        return greeting;
    }
}
