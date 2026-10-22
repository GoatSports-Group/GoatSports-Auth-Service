package iuh.fit.auth.application.service;

import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.service.impl.InternalServiceImpl;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.RoleEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalServiceImplTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserMapper userMapper;

    private InternalServiceImpl service() {
        return new InternalServiceImpl(userRepository, roleRepository, userMapper);
    }

    private Role role(RoleEnum name) {
        return Role.builder().roleId(UUID.randomUUID()).name(name.name()).build();
    }

    @Test
    void grantingVenueOwnerRoleTwiceWritesOnlyOnce() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).role(role(RoleEnum.VENUE_OWNER)).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service().assignVenueOwnerRole(userId.toString());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(roleRepository);
    }

    @Test
    void playerIsPromotedToVenueOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).role(role(RoleEnum.PLAYER)).build();
        Role venueOwner = role(RoleEnum.VENUE_OWNER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleEnum.VENUE_OWNER.name())).thenReturn(Optional.of(venueOwner));

        service().assignVenueOwnerRole(userId.toString());

        assertThat(user.getRole().getName()).isEqualTo(RoleEnum.VENUE_OWNER.name());
        verify(userRepository).save(user);
    }

    @Test
    void aPausedRoleCarriesNoPermissionsButAnActiveCustomRoleDoes() throws Exception {
        UUID userId = UUID.randomUUID();
        Role support = Role.builder().roleId(UUID.randomUUID()).name("SUPPORT_STAFF").active(true)
                .permissions(Set.of(Permission.builder().name("GET_USERS").build()))
                .build();
        User user = User.builder().userId(userId).role(support).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(service().getUserPermissions(userId.toString()).permissions()).containsExactly("GET_USERS");

        support.setActive(false);
        assertThat(service().getUserPermissions(userId.toString()).permissions()).isEmpty();
    }
}
