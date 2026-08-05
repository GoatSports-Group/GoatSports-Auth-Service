package iuh.fit.shared.config;

import iuh.fit.shared.aspect.AuditLogAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class SharedAuditLogAutoConfiguration {

    @Bean
    public AuditLogAspect auditLogAspect(KafkaTemplate<String, Object> kafkaTemplate) {
        return new AuditLogAspect(kafkaTemplate);
    }
}
