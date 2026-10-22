package com.challange.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("order-service-route", r -> r.path("/api/orders/**")
                        .uri("http://order-service:1200"))

                .route("inventory-service-route", r -> r.path("/api/products/**")
                        .uri("http://inventory-service:1100"))

                .build();
    }
}