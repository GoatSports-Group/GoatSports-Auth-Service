package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.role.CreateRoleRequestCommand;
import iuh.fit.auth.application.dto.request.role.UpdateRoleRequestCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleServiceImpl service =
            new RoleServiceImpl(roles, mock(PermissionRepository.class), users, mock(RedisPort.class));

    @BeforeEach
    void setUp() throws Exception {
        when(roles.findByName(any())).thenReturn(Optional.empty());
        when(roles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsACustomRoleWithANormalisedName() throws Exception {
        Role created = service.createRole(new CreateRoleRequestCommand("  support staff ", "Chăm sóc khách"));

        assertThat(created.getName()).isEqualTo("SUPPORT_STAFF");
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void rejectsBadOrDuplicateNames() {
        assertThatThrownBy(() -> service.createRole(new CreateRoleRequestCommand("hỗ trợ", null)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("chữ in hoa");
        when(roles.findByName("SDASD")).thenReturn(Optional.of(role("SDASD")));
        assertThatThrownBy(() -> service.createRole(new CreateRoleRequestCommand("sdasd", null)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("đã tồn tại");
    }

    @Test
    void systemRolesKeepTheirNameAndCannotBeDeletedAndAdminStaysActive() {
        Role admin = role("ADMIN");
        when(roles.findById(admin.getRoleId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.updateRole(update(admin, "SUPER_ADMIN", true)))
                .hasMessageContaining("Không đổi được tên");
        assertThatThrownBy(() -> service.updateRole(update(admin, "ADMIN", false)))
                .hasMessageContaining("tạm ngưng");
        assertThatThrownBy(() -> service.deleteRole(admin.getRoleId().toString()))
                .hasMessageContaining("vai trò hệ thống");
    }

    @Test
    void aCustomRoleStillHeldByAccountsIsNotDeleted() throws Exception {
        Role support = role("SUPPORT_STAFF");
        when(roles.findById(support.getRoleId())).thenReturn(Optional.of(support));
        when(users.countByRole()).thenReturn(Map.of("SUPPORT_STAFF", 2L));

        assertThatThrownBy(() -> service.deleteRole(support.getRoleId().toString())).hasMessageContaining("Còn 2 tài khoản");
        verify(roles, never()).delete(any());

        when(users.countByRole()).thenReturn(Map.of());
        service.deleteRole(support.getRoleId().toString());
        verify(roles).delete(support);
    }

    private static Role role(String name) {
        Role role = new Role();
        role.setRoleId(UUID.randomUUID());
        role.setName(name);
        role.setActive(true);
        return role;
    }

    private static UpdateRoleRequestCommand update(Role role, String name, boolean active) {
        return new UpdateRoleRequestCommand(role.getRoleId().toString(), name, "", active, null);
    }
}
