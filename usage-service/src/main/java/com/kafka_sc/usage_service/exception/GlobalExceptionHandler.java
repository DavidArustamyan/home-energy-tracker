package com.kafka_sc.usage_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleDependencyUnavailable(ResourceAccessException ex) {
        log.warn("Dependency unavailable: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Dependent service is temporarily unavailable"
        );
        problem.setTitle("Service unavailable");
        return problem;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ProblemDetail handleDependencyError(RestClientResponseException ex) {
        log.warn("Dependency returned {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "Dependent service returned an error"
        );
        problem.setTitle("Bad gateway");
        return problem;
    }

    @ExceptionHandler(UsageDataUnavailableException.class)
    public ProblemDetail handleUsageDataUnavailable(UsageDataUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Service unavailable");
        return problem;
    }


    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error happened", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"
        );
        problem.setTitle("Internal server error");
        return problem;
    }
}
