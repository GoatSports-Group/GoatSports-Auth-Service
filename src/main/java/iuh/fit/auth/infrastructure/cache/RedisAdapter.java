package iuh.fit.auth.infrastructure.cache;

import iuh.fit.auth.application.port.cache.RedisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisAdapter implements RedisPort {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveWithTTL(String key, String value, long ttl, TimeUnit timeUnit) {
        this.redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
    }

    @Override
    public boolean hasKey(String key) {
        return this.redisTemplate.hasKey(key);
    }

    @Override
    public String getValue(String key) {
        Object value = this.redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public void deleteKey(String key) {
        this.redisTemplate.delete(key);
    }

    @Override
    public void replaceKey(String oldKey, String newKey, String value, long ttl, TimeUnit timeUnit) {
        this.redisTemplate.delete(oldKey);
        this.redisTemplate.opsForValue().set(newKey, value, ttl, timeUnit);
    }

    @Override
    public void updateValue(String key, String value) {
        this.redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.stringCommands().set(
                    key.getBytes(),
                    value.getBytes(),
                    Expiration.keepTtl(),
                    RedisStringCommands.SetOption.upsert());
            return null;
        });
    }

    @Override
    public void deleteByPattern(String pattern) {
        Set<String> keys = this.redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            this.redisTemplate.delete(keys);
        }
    }

    @Override
    public long incrementWithTtl(String key, long ttl, TimeUnit timeUnit) {
        Long value = this.redisTemplate.opsForValue().increment(key);
        long current = value == null ? 1L : value;
        if (current == 1L) {
            this.redisTemplate.expire(key, ttl, timeUnit);
        }
        return current;
    }

    @Override
    public void evictRolePermissions(String roleName) {
        try {
            this.redisTemplate.delete(GATEWAY_ROLE_PERMISSIONS_PREFIX + roleName);
        } catch (RuntimeException exception) {
            log.warn("Không xóa được cache quyền của vai trò {}: {}", roleName, exception.getMessage());
        }
    }
}
