package com.kafka_sc.alert_service;

import com.kafka_sc.alert_service.testsupport.MySqlTestcontainersBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AlertServiceApplicationTests extends MySqlTestcontainersBase {
	@Test
	void contextLoads() {
	}
}
