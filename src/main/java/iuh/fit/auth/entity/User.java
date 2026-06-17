package iuh.fit.auth.entity;

import iuh.fit.auth.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;

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
public class User extends BaseEntity {
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
    private String status;
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;
}
