package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.application.dto.request.user.ScoutingSearchCommand;
import iuh.fit.auth.application.dto.response.user.ScoutedPlayerCommand;
import iuh.fit.auth.domain.enums.DayOfWeek;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.AvailabilityEntity;
import iuh.fit.auth.infrastructure.persistence.entity.PlayerSportProfileEntity;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.mapper.AvailabilityEntityMapperImpl;
import iuh.fit.auth.infrastructure.persistence.mapper.PlayerSportProfileEntityMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the scouting JPQL against a real (H2, PostgreSQL mode) database. */
@DataJpaTest
@ActiveProfiles("test")
@Import({PlayerSportProfileRepositoryImpl.class, PlayerSportProfileEntityMapperImpl.class, AvailabilityEntityMapperImpl.class})
class PlayerSportProfileRepositoryImplScoutingTest {
    // Club anchor: Hoàn Kiếm, Hà Nội.
    private static final double LAT = 21.0285;
    private static final double LNG = 105.8542;

    @Autowired
    private TestEntityManager em;
    @Autowired
    private PlayerSportProfileRepositoryImpl repository;

    private RoleEntity player;
    private UserEntity near;
    private UserEntity far;
    private UserEntity hidden;

    @BeforeEach
    void setUp() {
        player = em.persist(RoleEntity.builder().name(RoleEnum.PLAYER.name()).active(true).build());
        near = user("near", UserStatusEnum.ACTIVE);
        far = user("far", UserStatusEnum.ACTIVE);
        hidden = user("hidden", UserStatusEnum.ACTIVE);
        UserEntity blocked = user("blocked", UserStatusEnum.BLOCKED);

        profile(near, LAT + 0.01, LNG, SkillLevel.INTERMEDIATE, Set.of("Tiền đạo"), DayOfWeek.SATURDAY, true);
        profile(far, LAT + 0.05, LNG + 0.05, SkillLevel.ADVANCED, Set.of("Thủ môn"), DayOfWeek.MONDAY, true);
        profile(hidden, LAT, LNG, SkillLevel.INTERMEDIATE, Set.of("Tiền đạo"), DayOfWeek.SATURDAY, false);
        profile(blocked, LAT, LNG, SkillLevel.INTERMEDIATE, Set.of("Tiền đạo"), DayOfWeek.SATURDAY, true);
        em.flush();
        em.clear();
    }

    @Test
    void returnsDiscoverableActivePlayersNearestFirst() {
        List<ScoutedPlayerCommand> result = search(null, null, null);

        assertThat(result).extracting(ScoutedPlayerCommand::username).containsExactly("near", "far");
        ScoutedPlayerCommand first = result.getFirst();
        assertThat(first.profileVisible()).isTrue();
        assertThat(first.preferredPositions()).containsExactly("Tiền đạo");
        assertThat(first.availabilities()).singleElement()
                .satisfies(slot -> assertThat(slot.dayOfWeek()).isEqualTo(DayOfWeek.SATURDAY));
    }

    @Test
    void filtersBySkillPositionAndDay() {
        assertThat(search(Set.of(SkillLevel.ADVANCED), null, null))
                .extracting(ScoutedPlayerCommand::username).containsExactly("far");
        // Case-insensitive substring of a free-text position.
        assertThat(search(null, "thủ", null))
                .extracting(ScoutedPlayerCommand::username).containsExactly("far");
        assertThat(search(null, null, DayOfWeek.SATURDAY))
                .extracting(ScoutedPlayerCommand::username).containsExactly("near");
        // '%' is a literal character, not a wildcard.
        assertThat(search(null, "%", null)).isEmpty();
    }

    @Test
    void lookupHidesProfileDetailsOfPlayersWhoOptedOut() {
        List<ScoutedPlayerCommand> result = repository.findScoutingProfiles(
                SportType.FOOTBALL, List.of(near.getUserId(), hidden.getUserId(), UUID.randomUUID()));

        assertThat(result).hasSize(2);
        ScoutedPlayerCommand hiddenView = result.stream()
                .filter(item -> item.userId().equals(hidden.getUserId())).findFirst().orElseThrow();
        assertThat(hiddenView.profileVisible()).isFalse();
        assertThat(hiddenView.fullName()).isEqualTo("Player hidden");
        assertThat(hiddenView.skillLevel()).isNull();
        assertThat(result).filteredOn(ScoutedPlayerCommand::profileVisible)
                .extracting(ScoutedPlayerCommand::username).containsExactly("near");
    }

    private List<ScoutedPlayerCommand> search(Set<SkillLevel> skills, String position, DayOfWeek day) {
        ScoutingSearchCommand command = new ScoutingSearchCommand(
                SportType.FOOTBALL, LAT, LNG, 25, 10, skills, position, day);
        return repository.findScoutingCandidates(command, LAT - 1, LAT + 1, LNG - 1, LNG + 1, 10);
    }

    private UserEntity user(String username, UserStatusEnum status) {
        return em.persist(UserEntity.builder()
                .username(username)
                .fullName("Player " + username)
                .email(username + "@goat.test")
                .status(status)
                .role(player)
                .build());
    }

    private void profile(UserEntity user, double lat, double lng, SkillLevel skill, Set<String> positions,
                         DayOfWeek day, boolean discoverable) {
        PlayerSportProfileEntity profile = PlayerSportProfileEntity.builder()
                .user(user)
                .sportType(SportType.FOOTBALL)
                .skillLevel(skill)
                .eloRating(1200)
                .preferredPositions(new HashSet<>(positions))
                .city("Hà Nội")
                .latitude(lat)
                .longitude(lng)
                .playRadiusKm(10.0)
                .matchCount(0)
                .discoverable(discoverable)
                .availabilities(new HashSet<>())
                .build();
        profile.getAvailabilities().add(AvailabilityEntity.builder()
                .profile(profile)
                .dayOfWeek(day)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .timezone("Asia/Ho_Chi_Minh")
                .active(true)
                .build());
        em.persist(profile);
    }
}
