package iuh.fit.auth.application.port.cache;

import java.util.concurrent.TimeUnit;

public interface RedisPort {
    String GATEWAY_ROLE_PERMISSIONS_PREFIX = "PERMISSION:ROLE:v2:";

    /** Best-effort: Redis lỗi thì chỉ ghi cảnh báo (quyền cũ hết hạn sau 15 phút), không làm hỏng thao tác gọi. */
    void evictRolePermissions(String roleName);

    void saveWithTTL(String key, String value, long ttl, TimeUnit timeUnit);

    boolean hasKey(String key);

    String getValue(String key);

    void deleteKey(String key);

    void replaceKey(String oldKey, String newKey, String value, long ttl, TimeUnit timeUnit);

    void updateValue(String key, String value);

    void deleteByPattern(String pattern);

    long incrementWithTtl(String key, long ttl, TimeUnit timeUnit);
}
