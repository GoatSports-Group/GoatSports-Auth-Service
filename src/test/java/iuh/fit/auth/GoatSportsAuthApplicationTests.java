package iuh.fit.auth;

import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.infrastructure.decorator.AuthenticationDecorator;
import iuh.fit.auth.infrastructure.decorator.InternalDecorator;
import iuh.fit.auth.infrastructure.decorator.PermissionDecorator;
import iuh.fit.auth.infrastructure.decorator.RoleDecorator;
import iuh.fit.auth.infrastructure.decorator.UserDecorator;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaRoleRepository;
import iuh.fit.auth.presentation.controller.AuthenticationController;
import iuh.fit.auth.presentation.controller.UserController;
import iuh.fit.auth.presentation.controller.admin.PermissionAdminController;
import iuh.fit.auth.presentation.controller.admin.RoleAdminController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@ActiveProfiles("test")
class GoatSportsAuthApplicationTests {

    @Autowired
    private JpaRoleRepository roleRepository;
    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    /** Field trùng tên bean *Impl từng làm controller bỏ qua decorator, nên @Transactional ở decorator vô tác dụng. */
    @Test
    void controllersGoThroughTheTransactionalDecorators() {
        assertThat(field(UserController.class, "userService")).isInstanceOf(UserDecorator.class);
        assertThat(field(AuthenticationController.class, "authenticationService"))
                .isInstanceOf(AuthenticationDecorator.class);
        assertThat(field(AuthenticationController.class, "internalService")).isInstanceOf(InternalDecorator.class);
        assertThat(field(RoleAdminController.class, "roleService")).isInstanceOf(RoleDecorator.class);
        assertThat(field(PermissionAdminController.class, "permissionService")).isInstanceOf(PermissionDecorator.class);
    }

    private Object field(Class<?> controller, String name) {
        Object target = AopTestUtils.getUltimateTargetObject(context.getBean(controller));
        return ReflectionTestUtils.getField(target, name);
    }

    @Test
    @Transactional
    void venuePermissionsArePersistedForTheExpectedRoles() {
        Set<String> admin = permissionNames(RoleEnum.ADMIN);
        Set<String> owner = permissionNames(RoleEnum.VENUE_OWNER);
        Set<String> player = permissionNames(RoleEnum.PLAYER);

        assertThat(admin).contains(
                "GET_ALL_VENUES", "GET_ANY_VENUE", "GET_ANY_VENUE_COURTS", "GET_ANY_VENUE_COURT"
        );
        assertThat(owner).contains(
                "GET_OWN_VENUES", "GET_OWN_VENUE", "UPDATE_OWN_VENUE",
                "GET_OWN_VENUE_COURTS", "CREATE_OWN_VENUE_COURT",
                "GET_OWN_VENUE_COURT", "UPDATE_OWN_VENUE_COURT",
                "CHANGE_OWN_VENUE_COURT_STATUS"
        );
        assertThat(player).doesNotContainAnyElementsOf(owner);
        assertThat(admin).doesNotContainAnyElementsOf(owner);
    }

    private Set<String> permissionNames(RoleEnum roleName) {
        return roleRepository.findByName(roleName.name()).orElseThrow().getPermissions().stream()
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
    }
}
