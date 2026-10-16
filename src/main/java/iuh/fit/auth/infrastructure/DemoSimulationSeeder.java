package iuh.fit.auth.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cộng đồng mô phỏng (dev / demo): {@link DemoPopulation#PLAYERS} người chơi có hồ sơ thể thao, ELO và giờ rảnh, cùng
 * {@link DemoPopulation#OWNERS} chủ sân, để gợi ý sân, ghép trận, xếp hạng bảng tin và dashboard có dữ liệu để chạy.
 * Bật bằng {@code app.demo-data.simulation.enabled=true}. Tài khoản hư cấu (email @demo.goatsports.vn), dùng chung
 * mật khẩu demo; chạy lại bao nhiêu lần cũng được.
 */
@Slf4j
@Component
@Order(21)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-data.simulation", name = "enabled", havingValue = "true")
public class DemoSimulationSeeder implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        List<DemoPopulation.Player> players = DemoPopulation.players();
        Integer existing = jdbc.queryForObject("select count(*) from users where email like ?", Integer.class,
                "%@" + DemoPopulation.DOMAIN);
        if (existing != null && existing >= players.size() + DemoPopulation.OWNERS) {
            log.info("Dữ liệu mô phỏng: {} tài khoản đã có, bỏ qua", existing);
            return;
        }
        String password = passwordEncoder.encode(DemoDataSeeder.DEMO_PASSWORD);
        List<Object[]> users = new ArrayList<>();
        for (DemoPopulation.Owner owner : DemoPopulation.owners()) {
            users.add(new Object[]{owner.userId(), owner.email(), owner.username(), password, owner.fullName(),
                    phone(owner.email()), java.sql.Date.valueOf("1985-06-15"), owner.city().name(), "MALE", "VENUE_OWNER"});
        }
        for (DemoPopulation.Player player : players) {
            users.add(new Object[]{player.userId(), player.email(), player.username(), password, player.fullName(),
                    phone(player.email()), java.sql.Date.valueOf(player.birthDate()), player.city().name(),
                    player.male() ? "MALE" : "FEMALE", "PLAYER"});
        }
        int[][] inserted = jdbc.batchUpdate("""
                        insert into users (user_id, email, username, password, full_name, phone, date_of_birth, country,
                                           status, gender, role_id, created_at, created_by, updated_at, updated_by)
                        select ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, r.role_id, now(), 'demo-sim', now(), 'demo-sim'
                        from roles r where r.name = ?
                        on conflict do nothing
                        """, users, 200, (statement, row) -> {
            for (int column = 0; column < row.length; column++) statement.setObject(column + 1, row[column]);
        });
        jdbc.batchUpdate("""
                        insert into user_auth_providers (user_id, auth_provider)
                        select ?, 'LOCAL' where not exists (
                            select 1 from user_auth_providers where user_id = ? and auth_provider = 'LOCAL')
                        """, users, 200, (statement, row) -> {
            statement.setObject(1, row[0]);
            statement.setObject(2, row[0]);
        });

        List<Object[]> profiles = new ArrayList<>();
        List<Object[]> slots = new ArrayList<>();
        for (DemoPopulation.Player player : players) {
            for (int position = 0; position < player.sports().size(); position++) {
                String sport = player.sports().get(position);
                // Môn phụ chơi ít hơn: ELO và số trận thấp hơn môn chính.
                int elo = position == 0 ? player.elo() : Math.max(950, player.elo() - 150);
                int matches = (int) Math.round(player.bookingsPerWeek() * (position == 0 ? 14 : 5));
                int wins = (int) Math.round(matches * Math.min(0.75, Math.max(0.25, (elo - 1000) / 1200.0 + 0.3)));
                int draws = matches / 8;
                int losses = Math.max(0, matches - wins - draws);
                UUID profileId = id("profile", player.email() + ":" + sport);
                profiles.add(new Object[]{profileId, player.userId(), sport, player.skill(), elo, player.latitude(),
                        player.longitude(), 10 + (player.index() % 4) * 5, wins, losses, draws, matches,
                        matches == 0 ? 0D : (double) wins / matches});
                for (DemoPopulation.Slot slot : player.availability()) {
                    slots.add(new Object[]{id("availability", profileId + "|" + slot.day() + "|" + slot.start()),
                            profileId, slot.day().name(), java.sql.Time.valueOf(slot.start()),
                            java.sql.Time.valueOf(slot.end())});
                }
            }
        }
        jdbc.batchUpdate("""
                        insert into player_sport_profiles (profile_id, user_id, sport_type, skill_level, elo_rating,
                                                           latitude, longitude, play_radius_km, win_count, loss_count,
                                                           draw_count, match_count, win_rate,
                                                           created_at, created_by, updated_at, updated_by)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), 'demo-sim', now(), 'demo-sim')
                        on conflict do nothing
                        """, profiles, 200, (statement, row) -> {
            for (int column = 0; column < row.length; column++) statement.setObject(column + 1, row[column]);
        });
        jdbc.batchUpdate("""
                        insert into availabilities (availability_id, profile_id, day_of_week, start_time, end_time,
                                                    timezone, active)
                        values (?, ?, ?, ?, ?, 'Asia/Ho_Chi_Minh', true)
                        on conflict do nothing
                        """, slots, 500, (statement, row) -> {
            for (int column = 0; column < row.length; column++) statement.setObject(column + 1, row[column]);
        });
        log.info("Dữ liệu mô phỏng: {} tài khoản, {} hồ sơ thể thao, {} khung giờ rảnh",
                sum(inserted), profiles.size(), slots.size());
    }

    private static UUID id(String kind, String key) {
        return UUID.nameUUIDFromBytes(("goatsports:" + kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    /** Số điện thoại giả, cố định theo email (đầu 09, 10 số). */
    private static String phone(String email) {
        long digits = Math.abs(UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits());
        return "09" + String.format("%08d", digits % 100_000_000L);
    }

    private static int sum(int[][] batches) {
        int total = 0;
        for (int[] batch : batches) for (int count : batch) total += Math.max(count, 0);
        return total;
    }
}
