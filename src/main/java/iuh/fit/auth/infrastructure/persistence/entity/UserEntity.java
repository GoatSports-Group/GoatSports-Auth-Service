package iuh.fit.auth.infrastructure.persistence.entity;

import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.GenderEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID userId;
    @Column(columnDefinition = "UUID")
    private UUID keycloakId;
    private String email;
    private String username;
    private String password;
    private String fullName;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String phone;
    private String country;
    private LocalDateTime deletionRequestedAt;
    @Enumerated(EnumType.STRING)
    private UserStatusEnum status;
    @Enumerated(EnumType.STRING)
    private GenderEnum gender;

    @ElementCollection(targetClass = AuthProviderEnum.class, fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_auth_providers",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    @Builder.Default
    private Set<AuthProviderEnum> authProviders = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<PlayerSportProfileEntity> playerSportProfiles = new HashSet<>();
}
