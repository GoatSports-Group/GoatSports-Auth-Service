package iuh.fit.auth.infrastructure;

import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaPermissionRepository;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaRoleRepository;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseInitializerTest {
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JpaRoleRepository roleRepository;
    @Mock
    private JpaUserRepository userRepository;
    @Mock
    private JpaPermissionRepository permissionRepository;

    @Test
    void assignsVenuePermissionsToLeastPrivilegedRoles() throws Exception {
        Map<RoleEnum, RoleEntity> roles = new EnumMap<>(RoleEnum.class);
        for (RoleEnum roleName : RoleEnum.values()) {
            roles.put(roleName, RoleEntity.builder()
                    .name(roleName)
                    .active(true)
                    .permissions(new HashSet<>())
                    .build());
        }
        when(roleRepository.findByName(any(RoleEnum.class)))
                .thenAnswer(invocation -> Optional.of(roles.get(invocation.getArgument(0))));
        when(roleRepository.save(any(RoleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.existsByUsernameOrEmail(any(), any())).thenReturn(true);
        when(permissionRepository.findByName(any())).thenReturn(Optional.empty());
        when(permissionRepository.save(any(PermissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new DatabaseInitializer(
                passwordEncoder, roleRepository, userRepository, permissionRepository
        ).run();

        Set<String> adminPermissions = names(roles.get(RoleEnum.ADMIN));
        Set<String> playerPermissions = names(roles.get(RoleEnum.PLAYER));
        Set<String> ownerPermissions = names(roles.get(RoleEnum.VENUE_OWNER));

        assertThat(adminPermissions).contains(
                "GET_ALL_VENUES", "GET_ANY_VENUE", "GET_ANY_VENUE_COURTS", "GET_ANY_VENUE_COURT"
        );
        assertThat(adminPermissions).doesNotContain("UPDATE_OWN_VENUE", "CREATE_OWN_VENUE_COURT");
        assertThat(ownerPermissions).containsExactlyInAnyOrder(
                "GET_OWN_VENUES",
                "GET_OWN_VENUE",
                "UPDATE_OWN_VENUE",
                "GET_OWN_VENUE_COURTS",
                "CREATE_OWN_VENUE_COURT",
                "GET_OWN_VENUE_COURT",
                "UPDATE_OWN_VENUE_COURT",
                "CHANGE_OWN_VENUE_COURT_STATUS",
                "DELETE_OWN_VENUE_COURT",
                "GET_OWN_COURT_PRICING_RULES",
                "CREATE_OWN_COURT_PRICING_RULE",
                "UPDATE_OWN_COURT_PRICING_RULE",
                "DELETE_OWN_COURT_PRICING_RULE",
                "GET_OWN_TIME_SLOTS",
                "CREATE_OWN_TIME_SLOTS",
                "UPDATE_OWN_TIME_SLOT_STATUS",
                "DELETE_OWN_TIME_SLOT",
                "CREATE_OWN_OWNER_APPLICATION",
                "GET_OWN_OWNER_APPLICATION",
                "GET_OWN_BOOKINGS",
                "GET_OWN_BOOKING",
                "UPDATE_OWN_BOOKING_STATUS",
                "LOOKUP_OWN_CHECK_IN",
                "GET_OWN_CHECK_INS",
                "CONFIRM_OWN_CHECK_IN",
                "CREATE_OWN_WALK_IN_BOOKING",
                "GET_OWN_REVENUE",
                "GET_OWN_REVIEWS"
        );
        assertThat(adminPermissions).doesNotContain(
                "GET_OWN_REVENUE", "GET_OWN_REVIEWS", "CREATE_OWN_REVIEW"
        );
        assertThat(playerPermissions).containsExactly("CREATE_OWN_REVIEW");
        assertThat(ownerPermissions).doesNotContain("CREATE_OWN_REVIEW");
    }

    private Set<String> names(RoleEntity role) {
        return role.getPermissions().stream()
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
    }
}
