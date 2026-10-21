package iuh.fit.auth.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.topic.user-registration}")
    private String registrationTopic;

    @Value("${spring.kafka.topic.user-sync}")
    private String syncTopic;

    @Value("${spring.kafka.topic.user-deleted}")
    private String userDeletedTopic;

    @Bean
    public NewTopic userDeletedTopic() {
        return TopicBuilder.name(userDeletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userRegistrationTopic() {
        return TopicBuilder.name(registrationTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userSyncTopic() {
        return TopicBuilder.name(syncTopic)
                .partitions(3)
                .replicas(2)
                .build();
    }
}
