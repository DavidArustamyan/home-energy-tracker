package com.kafka_sc.api_gateway.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

@Configuration
public class FallbackRoutes {

    @Bean
    public RouterFunction<ServerResponse> fallbackRoute() {
        return route("fallback")
                .route(RequestPredicates.path("/fallback/{service}"), request -> {
                    String service = request.pathVariable("service");
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            service + " service temporarily unavailable"
                    );
                    problem.setTitle("Service unavailable");
                    return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                            .body(problem);
                }).build();
    }
}
