package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Truy vấn cho trang admin (tên theo lô, người nhận thông báo, báo cáo) chạy trên H2 chế độ PostgreSQL. */
@DataJpaTest
@ActiveProfiles("test")
class UserAdminQueriesTest {
    private static final UUID START = new UUID(0L, 0L);

    @Autowired private TestEntityManager em;
    @Autowired private JpaUserRepository repository;

    private final List<UUID> activePlayersAndOwners = new ArrayList<>();
    private UUID admin;

    @BeforeEach
    void setUp() {
        RoleEntity player = role(RoleEnum.PLAYER);
        RoleEntity owner = role(RoleEnum.VENUE_OWNER);
        RoleEntity adminRole = role(RoleEnum.ADMIN);
        for (int index = 0; index < 5; index++) {
            activePlayersAndOwners.add(user("p" + index, UserStatusEnum.ACTIVE, player));
        }
        activePlayersAndOwners.add(user("o0", UserStatusEnum.ACTIVE, owner));
        user("blocked", UserStatusEnum.BLOCKED, player);
        admin = user("admin", UserStatusEnum.ACTIVE, adminRole);
        em.flush();
        em.clear();
        // PostgreSQL/H2 so uuid theo byte không dấu; UUID.compareTo của Java so long có dấu nên không dùng được.
        activePlayersAndOwners.sort(Comparator.comparing(UUID::toString));
    }

    @Test
    void recipientKeysetPagesCoverEveryActiveRecipientOnceInIdOrder() {
        Set<String> roles = Set.of(RoleEnum.PLAYER.name(), RoleEnum.VENUE_OWNER.name());
        List<UUID> collected = new ArrayList<>();
        UUID after = START;
        while (true) {
            List<UUID> page = repository.findIdsByStatusAndRoles(UserStatusEnum.ACTIVE, roles, after, PageRequest.of(0, 4));
            if (page.isEmpty()) break;
            collected.addAll(page);
            after = page.getLast();
        }

        // Người bị khóa và admin không nhận; không trùng, không sót qua ranh giới trang.
        assertThat(collected).containsExactlyElementsOf(activePlayersAndOwners);
        assertThat(repository.countByStatusAndRoles(UserStatusEnum.ACTIVE, roles)).isEqualTo(6);
        assertThat(repository.countByStatusAndRoles(UserStatusEnum.ACTIVE, Set.of(RoleEnum.VENUE_OWNER.name()))).isEqualTo(1);
    }

    @Test
    void summariesAndReportRowsReadOnlyTheNeededColumns() {
        assertThat(repository.findSummariesByIds(List.of(admin, UUID.randomUUID())))
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.fullName()).isEqualTo("User admin");
                    assertThat(summary.email()).isEqualTo("admin@goat.test");
                });

        assertThat(repository.findAllForReport()).hasSize(8)
                .anySatisfy(row -> {
                    assertThat(row.role()).isEqualTo(RoleEnum.PLAYER.name());
                    assertThat(row.status()).isEqualTo(UserStatusEnum.BLOCKED);
                });
    }

    private RoleEntity role(RoleEnum name) {
        return em.persist(RoleEntity.builder().name(name.name()).active(true).build());
    }

    private UUID user(String username, UserStatusEnum status, RoleEntity role) {
        return em.persist(UserEntity.builder()
                .username(username)
                .fullName("User " + username)
                .email(username + "@goat.test")
                .status(status)
                .role(role)
                .build()).getUserId();
    }
}
