package de.idealo.kafka.idempotency.persistence;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;

import de.idealo.kafka.idempotency.RecordIdentity;
import de.idealo.kafka.idempotency.configuration.IdealoKafkaIdempotencyAutoconfiguration;

public class RedisRecordIdempotencyLookup implements RecordIdempotencyLookup {

    /**
     * Actually this can be everything.
     */
    private static final String LOOKUP_VALUE = "1";

    private static final String KEY_DELIMITER = "_";

    private final StringRedisTemplate redisTemplate;

    private final String prefix;

    public RedisRecordIdempotencyLookup(final IdealoKafkaIdempotencyAutoconfiguration configuration,
            final StringRedisTemplate template) {
        this.redisTemplate = template;
        this.prefix = configuration.getKeyPrefix() + KEY_DELIMITER + configuration.getConsumerGroupId() + KEY_DELIMITER;
    }

    @Override
    public boolean isLogged(RecordIdentity id) {
        return redisTemplate.hasKey(id.toString(prefix));
    }

    @Override
    public void log(final RecordIdentity id, final Duration ttl) {
        redisTemplate.opsForValue().set(id.toString(prefix), LOOKUP_VALUE, ttl);
    }

}
