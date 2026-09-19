package iuh.fit.auth.application.port.cache;

import java.util.concurrent.TimeUnit;

public interface RedisPort {
    void saveWithTTL(String key, String value, long ttl, TimeUnit timeUnit);

    boolean hasKey(String key);

    String getValue(String key);

    void deleteKey(String key);

    void replaceKey(String oldKey, String newKey, String value, long ttl, TimeUnit timeUnit);

    void updateValue(String key, String value);

    void deleteByPattern(String pattern);

    long incrementWithTtl(String key, long ttl, TimeUnit timeUnit);
}
