package com.kafka_sc.injestion_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InjestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InjestionServiceApplication.class, args);
	}

}
