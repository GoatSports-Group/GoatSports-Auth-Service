package iuh.fit.auth;

import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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

    @Test
    void contextLoads() {
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
