package iuh.fit.auth.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@MappedSuperclass
@Getter
@Setter
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
