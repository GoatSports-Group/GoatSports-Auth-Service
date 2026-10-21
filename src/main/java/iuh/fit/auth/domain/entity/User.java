package iuh.fit.auth.domain.entity;

import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.GenderEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    private UUID userId;
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
    private UserStatusEnum status;
    private GenderEnum gender;
    @Builder.Default
    private Set<AuthProviderEnum> authProviders = new HashSet<>();
    private Role role;

    @Builder.Default
    private Set<PlayerSportProfile> playerSportProfiles = new HashSet<>();
}
