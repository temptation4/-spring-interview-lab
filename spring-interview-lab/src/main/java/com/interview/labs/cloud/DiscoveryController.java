package com.interview.labs.cloud;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Requires a Eureka server at {@code eureka.client.service-url.defaultZone} (see
 * application.properties) to see anything besides this app's own instance. Without one
 * running, the app still starts fine — {@code DiscoveryClient} just returns an empty list.
 */
@RestController
@RequestMapping("/cloud")
public class DiscoveryController {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate loadBalancedRestTemplate;
    private final GreetingProperties greetingProperties;

    public DiscoveryController(
            DiscoveryClient discoveryClient,
            RestTemplate loadBalancedRestTemplate,
            GreetingProperties greetingProperties) {

        this.discoveryClient = discoveryClient;
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.greetingProperties = greetingProperties;
    }

    // GET /cloud/config — value sourced from Config Server if one is running, else the local default
    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("greeting", greetingProperties.getGreeting());
    }

    // GET /cloud/services — everything currently registered with the discovery server
    @GetMapping("/services")
    public List<String> services() {
        return discoveryClient.getServices();
    }

    // GET /cloud/instances/{serviceName} — where a given logical service name currently resolves to
    @GetMapping("/instances/{serviceName}")
    public List<ServiceInstance> instances(@PathVariable String serviceName) {
        return discoveryClient.getInstances(serviceName);
    }

    /**
     * GET /cloud/call/{serviceName}/{path} — demonstrates a load-balanced call: this app
     * never sees a real host:port, only the logical service name. Ribbon/Spring Cloud
     * LoadBalancer picks a live instance behind the scenes.
     */
    @GetMapping("/call/{serviceName}/{path}")
    public ResponseEntity<String> callByServiceName(@PathVariable String serviceName, @PathVariable String path) {
        String url = "http://" + serviceName + "/" + path;
        return loadBalancedRestTemplate.getForEntity(url, String.class);
    }

    // GET /cloud/self — this instance's own registration info, as Eureka sees it
    @GetMapping("/self")
    public Map<String, Object> self() {
        List<ServiceInstance> self = discoveryClient.getInstances("spring-interview-lab");
        return Map.of("registeredInstances", self.size(), "instances", self);
    }
}
