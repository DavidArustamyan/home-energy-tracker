package com.kafka_sc.insight_service.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                                You are an expert energy efficiency advisor.
                                Provide concise and practical advice on how users can reduce
                                their energy consumption based on their usage patterns.
                                """
                )
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
