package org.vito.server.booksplit.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {
    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic topicGenerationResult() {
        return new NewTopic("generation_response", 1, (short) 1);
    }

    @Bean
    public NewTopic topicGenerationProgress() {
        return new NewTopic("generation_progress", 1, (short) 1);
    }

    @Bean
    public NewTopic topicGenerationRequest() {
        return new NewTopic("generation_request", 1, (short) 1);
    }
}
