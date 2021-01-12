package de.idealo.kafka.idempotency.configuration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import de.idealo.kafka.idempotency.IdempotentListenerAspect;
import de.idealo.kafka.idempotency.RecordIdentityExtractor;
import de.idealo.kafka.idempotency.persistence.RecordIdempotencyLookup;
import de.idealo.kafka.idempotency.persistence.RedisRecordIdempotencyLookup;

@Configuration
@AutoConfigureAfter({ KafkaAutoConfiguration.class })
@EnableConfigurationProperties(KafkaListenerIdempotencyProperties.class)
@ConditionalOnProperty(
        prefix = IdealoKafkaIdempotencyAutoconfiguration.PROPERTY_PREFIX,
        name = "enabled", havingValue = "true",
        matchIfMissing = true
)
public class IdealoKafkaIdempotencyAutoconfiguration {

    public static final String PROPERTY_PREFIX = "idealo.kafka.idempotency";

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private KafkaListenerIdempotencyProperties idempotencyProperties;

    /**
     * Combining conditionals the autoconfiguration in future will be able to use different persistence lookup providers.
     * @param template
     * @return
     */
    @Bean
    public RecordIdempotencyLookup redisRecordIdempotencyLookup(StringRedisTemplate template) {
        return new RedisRecordIdempotencyLookup(this, template);
    }

    @Bean
    public IdempotentListenerAspect idempotentListenerAspect(RecordIdempotencyLookup recordIdempotencyLookup) {
        return new IdempotentListenerAspect(this, recordIdempotencyLookup, new RecordIdentityExtractor());
    }

    /**
     * Whether the idempotency check on listeners is enabled.
     * @return
     */
    public boolean isCheckEnabled() {
        return idempotencyProperties.isCheckEnabled();
    }

    /**
     * Whether the idempotency markers are persisted after a record has been consumed.
     * @return
     */
    public boolean isPersistenceEnabled() {
        return idempotencyProperties.isPersistenceEnabled();
    }

    /**
     * Gets TTL for persistence markers.
     * @return
     */
    public Duration getTtl() {
        return idempotencyProperties.getTtl();
    }

    /**
     * Gets the prefix used for the Redis keys that hold the idempotency information.
     * @return
     */
    public String getKeyPrefix() {
        return idempotencyProperties.getKeyPrefix();
    }

    /**
     * Gets the consumer group id (set via the standard Spring Kafka properties).
     * @return
     */
    public String getConsumerGroupId() {
        return kafkaProperties.getConsumer().getGroupId();
    }

    /**
     * Whether to suppress any exceptions or to bubble them up.
     * @return
     */
    public boolean isSuppressErrors() {
        return idempotencyProperties.isSuppressErrors();
    }
}
