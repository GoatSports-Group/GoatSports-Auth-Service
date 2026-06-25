package iuh.fit.auth.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {
    @CreationTimestamp
    private LocalDateTime createdAt;
    @CreatedBy
    private String createdBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @LastModifiedBy
    private String updatedBy;
}
