package iuh.fit.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import iuh.fit.auth.service.JwtService;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class BaseEntity {
    @CreationTimestamp
    private LocalDateTime createdAt;
    private String createdBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private String updatedBy;

    @PrePersist
    public void onCreate() {
        this.createdBy = JwtService.getCurrentUser().isPresent()
                ? JwtService.getCurrentUser().get()
                : "System";
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedBy = JwtService.getCurrentUser().isPresent()
                ? JwtService.getCurrentUser().get()
                : "System";
    }
}
