package de.idealo.kafka.idempotency;

import java.time.Duration;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import de.idealo.kafka.idempotency.configuration.IdealoKafkaIdempotencyAutoconfiguration;
import de.idealo.kafka.idempotency.persistence.RecordIdempotencyLookup;

/**
 * AOP aspect which defines pointcuts and advices for using {@link IdempotentListener} annotations.
 * In order for this to work properly, this class must be a bean in the spring context and AOP needs to be enabled
 * (see {@link org.springframework.context.annotation.EnableAspectJAutoProxy}).
 * Also, an implementation of {@link RecordIdempotencyLookup}) must be in the classpath.
 */
@Aspect
public class IdempotentListenerAspect {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotentListener.class);

    private final IdealoKafkaIdempotencyAutoconfiguration configuration;
    private final RecordIdempotencyLookup idempotencyLookup;
    private final RecordIdentityExtractor recordIdentityExtractor;

    public IdempotentListenerAspect(final IdealoKafkaIdempotencyAutoconfiguration configuration,
            RecordIdempotencyLookup idempotencyLookup, RecordIdentityExtractor recordIdentityExtractor) {
        this.configuration = configuration;
        this.idempotencyLookup = idempotencyLookup;
        this.recordIdentityExtractor = recordIdentityExtractor;
    }

    /**
     * Around-advice to intercept methods that are annotated at one or more of their arguments.
     *
     * @param joinPoint     the invocation's join point
     * @return              the result of the actual method invocation
     * @throws Throwable    any exception thrown during method invocation
     */
    @Around(value = "@annotation(de.idealo.kafka.idempotency.IdempotentListener)")
    public Object check(ProceedingJoinPoint joinPoint) throws Throwable { // NOSONAR
        if (!configuration.isCheckEnabled()) {
            LOG.debug("Idempotency check is disabled. Skipped for the record {} at location {}");
            return joinPoint.proceed();
        }

        try {
            final var recordId = recordIdentityExtractor.extract(joinPoint);

            if (idempotencyLookup.isLogged(recordId)) {
                // skip method invocation
                LOG.debug("Listener method invocation will be skipped for event due to idempotency check for the record {} at location {}",
                        recordId, joinPoint.getSignature().toString());
                return null;
            }
        } catch (Throwable e) { // NOSONAR
            LOG.error("Could not look up the idempotency information due to an error", e);
            if (!configuration.isSuppressErrors()) {
                throw new IdempotencyCheckException(e);
            }
        }
        // proceed as normal
        return joinPoint.proceed();
    }

    @AfterReturning(
            value = "@annotation(idempotentListener)",
            argNames = "joinPoint,idempotentListener"
    )
    public void persist(JoinPoint joinPoint, IdempotentListener idempotentListener) {
        if (!configuration.isPersistenceEnabled()) {
            LOG.debug("Saving idempotency markers is disabled. Skipped for the record {} at location {}");
            return;
        }

        try {
            final var recordId = recordIdentityExtractor.extract(joinPoint);
            if(!StringUtils.isEmpty(recordId)) {
                idempotencyLookup.log(recordId, selectTtl(idempotentListener.ttl()));
            }
        } catch (Throwable e) { // NOSONAR
            LOG.error("Could not save idempotency marker for the record", e);
            return;
        }
    }

    /**
     * Takes TTL given as an annotation parameter. If non-zero, it is converted from seconds to Duration and returned,
     *   otherwise the global default is used.
     * @param ttl
     * @return
     */
    private Duration selectTtl(final int ttl) {
        if (ttl <= 0) {
            return configuration.getTtl();
        }

        return Duration.ofSeconds(ttl);
    }
}
