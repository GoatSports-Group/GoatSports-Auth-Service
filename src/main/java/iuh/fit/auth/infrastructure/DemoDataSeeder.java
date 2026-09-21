package iuh.fit.auth.infrastructure;

import iuh.fit.auth.domain.enums.GenderEnum;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu mẫu cho môi trường dev. Bật bằng `app.demo-data.enabled=true`.
 *
 * <p>Khoá chính sinh **tất định** từ email:
 * {@code UUID.nameUUIDFromBytes("goatsports:user:" + email)}. Nhờ vậy venue-service,
 * club-service và social-service tính ra đúng cùng một UUID mà không cần gọi chéo,
 * nên quan hệ giữa các service khớp nhau ngay cả khi seed chạy độc lập.
 *
 * <p>Dùng native insert kèm {@code ON CONFLICT DO NOTHING} thay vì JPA: với
 * {@code @GeneratedValue(AUTO)} thì merge một entity có sẵn id vẫn có thể bị generator
 * ghi đè, mà ở đây id bắt buộc phải giữ nguyên. Kèm luôn tính idempotent, chạy lại bao nhiêu lần cũng được.
 *
 * <p>Đây là tài khoản mẫu hư cấu, không phải người thật; mật khẩu chung để tiện thử nghiệm.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    /** Mật khẩu chung của mọi tài khoản mẫu; lưu xuống DB đã băm BCrypt. */
    public static final String DEMO_PASSWORD = "Minhdat13122003@";

    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    /** Quy ước dùng chung cho mọi service: id người dùng suy ra từ email. */
    public static UUID userId(String email) {
        return UUID.nameUUIDFromBytes(("goatsports:user:" + email).getBytes(StandardCharsets.UTF_8));
    }

    private record UserSeed(
            RoleEnum role, String username, String fullName, String email,
            String phone, LocalDate dateOfBirth, GenderEnum gender, String city) {}

    private record ProfileSeed(
            String email, SportType sportType, SkillLevel skillLevel, int elo,
            int win, int loss, int draw, double latitude, double longitude) {}

    private static final List<UserSeed> USERS = List.of(
            // --- Chủ sân ---
            new UserSeed(RoleEnum.VENUE_OWNER, "baotq", "Trần Quốc Bảo",
                    "bao.tranquoc@goatsports.vn", "0903812447",
                    LocalDate.of(1987, 4, 12), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.VENUE_OWNER, "huongntm", "Nguyễn Thị Mai Hương",
                    "huong.nguyenthimai@goatsports.vn", "0932176509",
                    LocalDate.of(1990, 9, 3), GenderEnum.FEMALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.VENUE_OWNER, "phuclh", "Lê Hoàng Phúc",
                    "phuc.lehoang@goatsports.vn", "0977245130",
                    LocalDate.of(1985, 12, 27), GenderEnum.MALE, "Hà Nội"),
            new UserSeed(RoleEnum.VENUE_OWNER, "khoipm", "Phạm Minh Khôi",
                    "khoi.phamminh@goatsports.vn", "0325690814",
                    LocalDate.of(1993, 6, 18), GenderEnum.MALE, "Đà Nẵng"),

            // --- Người chơi ---
            new UserSeed(RoleEnum.PLAYER, "huydg", "Đặng Gia Huy",
                    "huy.danggia@goatsports.vn", "0901334726",
                    LocalDate.of(1998, 2, 21), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "tungvt", "Vũ Thanh Tùng",
                    "tung.vuthanh@goatsports.vn", "0938561092",
                    LocalDate.of(1996, 11, 8), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "minhhn", "Hoàng Nhật Minh",
                    "minh.hoangnhat@goatsports.vn", "0967418235",
                    LocalDate.of(2000, 5, 14), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "linhbk", "Bùi Khánh Linh",
                    "linh.buikhanh@goatsports.vn", "0356902178",
                    LocalDate.of(1999, 8, 30), GenderEnum.FEMALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "anhnd", "Ngô Đức Anh",
                    "anh.ngoduc@goatsports.vn", "0912745360",
                    LocalDate.of(1994, 3, 5), GenderEnum.MALE, "Hà Nội"),
            new UserSeed(RoleEnum.PLAYER, "dangth", "Trịnh Hải Đăng",
                    "dang.trinhhai@goatsports.vn", "0945218673",
                    LocalDate.of(1997, 7, 19), GenderEnum.MALE, "Hà Nội"),
            new UserSeed(RoleEnum.PLAYER, "trangly", "Lý Thu Trang",
                    "trang.lythu@goatsports.vn", "0389634205",
                    LocalDate.of(2001, 1, 26), GenderEnum.FEMALE, "Đà Nẵng"),
            new UserSeed(RoleEnum.PLAYER, "ngoccb", "Cao Bảo Ngọc",
                    "ngoc.caobao@goatsports.vn", "0796150482",
                    LocalDate.of(1995, 10, 11), GenderEnum.FEMALE, "Đà Nẵng"),
            new UserSeed(RoleEnum.PLAYER, "annv", "Nguyễn Văn An",
                    "an.nguyenvan@goatsports.vn", "0902457813",
                    LocalDate.of(1998, 6, 17), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "quanmt", "Trần Minh Quân",
                    "quan.tranminh@goatsports.vn", "0936172845",
                    LocalDate.of(1997, 9, 4), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "namlh", "Lê Hoàng Nam",
                    "nam.lehoang@goatsports.vn", "0962847153",
                    LocalDate.of(2000, 1, 22), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "huypg", "Phạm Gia Huy",
                    "huy.phamgia@goatsports.vn", "0983157246",
                    LocalDate.of(1996, 12, 9), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "kietdt", "Đỗ Tuấn Kiệt",
                    "kiet.dotuan@goatsports.vn", "0324815967",
                    LocalDate.of(1999, 3, 28), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "minhnv", "Nguyễn Văn Minh",
                    "minh.nguyenvan@goatsports.vn", "0357281946",
                    LocalDate.of(1995, 7, 13), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "phucth", "Trần Hoàng Phúc",
                    "phuc.tranhoang@goatsports.vn", "0374192865",
                    LocalDate.of(1998, 10, 2), GenderEnum.MALE, "Hồ Chí Minh"),
            new UserSeed(RoleEnum.PLAYER, "ducnm", "Ngô Minh Đức",
                    "duc.ngominh@goatsports.vn", "0792846135",
                    LocalDate.of(2001, 5, 19), GenderEnum.MALE, "Hồ Chí Minh")
    );

    private static final List<ProfileSeed> PROFILES = List.of(
            new ProfileSeed("huy.danggia@goatsports.vn", SportType.FOOTBALL, SkillLevel.ADVANCED,
                    1580, 24, 11, 5, 10.8014, 106.7109),
            new ProfileSeed("huy.danggia@goatsports.vn", SportType.BADMINTON, SkillLevel.INTERMEDIATE,
                    1320, 9, 7, 1, 10.8014, 106.7109),
            new ProfileSeed("tung.vuthanh@goatsports.vn", SportType.FOOTBALL, SkillLevel.INTERMEDIATE,
                    1415, 16, 14, 6, 10.7972, 106.7018),
            new ProfileSeed("minh.hoangnhat@goatsports.vn", SportType.PICKLEBALL, SkillLevel.BEGINNER,
                    1105, 3, 8, 0, 10.7769, 106.7009),
            new ProfileSeed("linh.buikhanh@goatsports.vn", SportType.BADMINTON, SkillLevel.ADVANCED,
                    1642, 31, 9, 2, 10.8231, 106.6297),
            new ProfileSeed("anh.ngoduc@goatsports.vn", SportType.TENNIS, SkillLevel.PRO,
                    1875, 48, 12, 4, 21.0278, 105.8342),
            new ProfileSeed("dang.trinhhai@goatsports.vn", SportType.FOOTBALL, SkillLevel.INTERMEDIATE,
                    1388, 13, 15, 7, 21.0069, 105.8430),
            new ProfileSeed("trang.lythu@goatsports.vn", SportType.BADMINTON, SkillLevel.INTERMEDIATE,
                    1296, 11, 10, 3, 16.0544, 108.2022),
            new ProfileSeed("ngoc.caobao@goatsports.vn", SportType.PICKLEBALL, SkillLevel.INTERMEDIATE,
                    1341, 14, 9, 1, 16.0678, 108.2208),
            new ProfileSeed("an.nguyenvan@goatsports.vn", SportType.FOOTBALL, SkillLevel.INTERMEDIATE,
                    1450, 18, 10, 4, 10.8021, 106.7102),
            new ProfileSeed("an.nguyenvan@goatsports.vn", SportType.BADMINTON, SkillLevel.BEGINNER,
                    1125, 5, 8, 1, 10.8021, 106.7102),
            new ProfileSeed("quan.tranminh@goatsports.vn", SportType.FOOTBALL, SkillLevel.ADVANCED,
                    1572, 27, 12, 5, 10.8084, 106.7076),
            new ProfileSeed("nam.lehoang@goatsports.vn", SportType.FOOTBALL, SkillLevel.INTERMEDIATE,
                    1390, 14, 11, 6, 10.7964, 106.6991),
            new ProfileSeed("nam.lehoang@goatsports.vn", SportType.BADMINTON, SkillLevel.INTERMEDIATE,
                    1312, 12, 9, 3, 10.7964, 106.6991),
            new ProfileSeed("huy.phamgia@goatsports.vn", SportType.FOOTBALL, SkillLevel.ADVANCED,
                    1610, 32, 13, 4, 10.8058, 106.7150),
            new ProfileSeed("kiet.dotuan@goatsports.vn", SportType.FOOTBALL, SkillLevel.INTERMEDIATE,
                    1432, 17, 13, 5, 10.7999, 106.7044),
            new ProfileSeed("kiet.dotuan@goatsports.vn", SportType.BADMINTON, SkillLevel.INTERMEDIATE,
                    1368, 16, 10, 2, 10.7999, 106.7044),
            new ProfileSeed("minh.nguyenvan@goatsports.vn", SportType.FOOTBALL, SkillLevel.ADVANCED,
                    1528, 24, 14, 6, 10.8037, 106.7138),
            new ProfileSeed("phuc.tranhoang@goatsports.vn", SportType.FOOTBALL, SkillLevel.INTERMEDIATE,
                    1466, 20, 15, 5, 10.8103, 106.7055),
            new ProfileSeed("duc.ngominh@goatsports.vn", SportType.FOOTBALL, SkillLevel.INTERMEDIATE,
                    1410, 15, 12, 7, 10.7982, 106.7087)
    );

    @Override
    @Transactional
    public void run(String... args) {
        String hashedPassword = passwordEncoder.encode(DEMO_PASSWORD);
        int users = 0;
        for (UserSeed seed : USERS) {
            users += insertUser(seed, hashedPassword);
        }
        int profiles = 0;
        for (ProfileSeed seed : PROFILES) {
            profiles += insertProfile(seed);
        }
        log.info("Dữ liệu mẫu: thêm {} người dùng và {} hồ sơ thể thao (bỏ qua bản ghi đã có)",
                users, profiles);
    }

    private int insertUser(UserSeed seed, String hashedPassword) {
        UUID id = userId(seed.email());
        int inserted = entityManager.createNativeQuery("""
                        insert into users (user_id, email, username, password, full_name, phone,
                                           date_of_birth, country, status, gender, role_id,
                                           created_at, created_by, updated_at, updated_by)
                        select :id, :email, :username, :password, :fullName, :phone,
                               :dateOfBirth, :country, 'ACTIVE', :gender, r.role_id,
                               now(), 'demo-seed', now(), 'demo-seed'
                        from roles r where r.name = :role
                        on conflict do nothing
                        """)
                .setParameter("id", id)
                .setParameter("email", seed.email())
                .setParameter("username", seed.username())
                .setParameter("password", hashedPassword)
                .setParameter("fullName", seed.fullName())
                .setParameter("phone", seed.phone())
                .setParameter("dateOfBirth", seed.dateOfBirth())
                .setParameter("country", seed.city())
                .setParameter("gender", seed.gender().name())
                .setParameter("role", seed.role().name())
                .executeUpdate();

        entityManager.createNativeQuery("""
                        insert into user_auth_providers (user_id, auth_provider)
                        select :id, 'LOCAL'
                        where not exists (
                            select 1 from user_auth_providers
                            where user_id = :id and auth_provider = 'LOCAL')
                        """)
                .setParameter("id", id)
                .executeUpdate();

        return inserted;
    }

    private int insertProfile(ProfileSeed seed) {
        int matches = seed.win() + seed.loss() + seed.draw();
        double winRate = matches == 0 ? 0D : (double) seed.win() / matches;
        return entityManager.createNativeQuery("""
                        insert into player_sport_profiles (profile_id, user_id, sport_type, skill_level,
                                                           elo_rating, latitude, longitude, play_radius_km,
                                                           win_count, loss_count, draw_count, match_count, win_rate,
                                                           created_at, created_by, updated_at, updated_by)
                        values (:profileId, :userId, :sportType, :skillLevel,
                                :elo, :latitude, :longitude, 10,
                                :win, :loss, :draw, :matches, :winRate,
                                now(), 'demo-seed', now(), 'demo-seed')
                        on conflict do nothing
                        """)
                .setParameter("profileId", UUID.nameUUIDFromBytes(
                        ("goatsports:profile:" + seed.email() + ":" + seed.sportType())
                                .getBytes(StandardCharsets.UTF_8)))
                .setParameter("userId", userId(seed.email()))
                .setParameter("sportType", seed.sportType().name())
                .setParameter("skillLevel", seed.skillLevel().name())
                .setParameter("elo", seed.elo())
                .setParameter("latitude", seed.latitude())
                .setParameter("longitude", seed.longitude())
                .setParameter("win", seed.win())
                .setParameter("loss", seed.loss())
                .setParameter("draw", seed.draw())
                .setParameter("matches", matches)
                .setParameter("winRate", winRate)
                .executeUpdate();
    }
}
