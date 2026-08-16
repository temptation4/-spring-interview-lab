package com.interview.labs.cloud;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CloudConfig {

    /**
     * {@code @LoadBalanced} makes this RestTemplate resolve hostnames against the service
     * registry instead of DNS — e.g. {@code http://some-other-service/api} is rewritten to
     * whichever instance Eureka currently has registered under that name.
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}
