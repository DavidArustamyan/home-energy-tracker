package com.kafka_sc.usage_service.exception;

public class UsageDataUnavailableException extends RuntimeException {
    public UsageDataUnavailableException(Throwable cause) {
        super("Usage data storage is temporarily unavailable", cause);
    }
}
