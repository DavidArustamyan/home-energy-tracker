package com.kafka_sc.insight_service.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI insightServiceApiDocs() {
        return new OpenAPI()
                .info(new Info()
                        .title("insight Service API")
                        .description("insight service API for Home Energy Tracker")
                        .contact(getContact())
                        .license(getLicense())
                        .version("1.0.0"));
    }

    private static License getLicense() {
        License license = new License();
        license.setName("Creative Commons Attribution-NonCommercial 4.0 International License");
        license.setUrl("https://creativecommons.org/licenses/by-nc/4.0/");
        return license;
    }

    private static Contact getContact() {
        Contact contact = new Contact();
        contact.setUrl("https://kafka-sc.com");
        contact.setEmail("kafka-sc@Gmail.com");
        return contact;
    }
}

