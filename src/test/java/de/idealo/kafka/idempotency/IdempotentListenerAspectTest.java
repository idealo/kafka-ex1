package de.idealo.kafka.idempotency;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.idealo.kafka.idempotency.configuration.IdealoKafkaIdempotencyAutoconfiguration;
import de.idealo.kafka.idempotency.persistence.RecordIdempotencyLookup;

public class IdempotentListenerAspectTest {

    private final RecordIdempotencyLookup lookup = mock(RecordIdempotencyLookup.class);
    private final RecordIdentityExtractor idExtractor = mock(RecordIdentityExtractor.class);
    private final MethodSignature methodSignature = mock(MethodSignature.class);
    private final ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
    private final IdealoKafkaIdempotencyAutoconfiguration configuration = mock(IdealoKafkaIdempotencyAutoconfiguration.class);
    private final IdempotentListenerAspect aspect = new IdempotentListenerAspect(configuration, lookup, idExtractor);

    private RecordIdentity recordId;
    private IdempotentListener annotation = mock(IdempotentListener.class);

    @BeforeEach
    public void setUp() throws Throwable {
        this.recordId = new RecordIdentity(Arrays.asList(UUID.randomUUID().toString()));

        when(idExtractor.extract(proceedingJoinPoint)).thenReturn(recordId);
        when(proceedingJoinPoint.proceed()).thenReturn(proceedingJoinPoint);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    public void checks_record_feature_disabled() throws Throwable {
        when(configuration.isCheckEnabled()).thenReturn(false);

        assertThat(aspect.check(proceedingJoinPoint)).isEqualTo(proceedingJoinPoint);
        verify(lookup, never()).isLogged(any());
    }

    @Test
    public void checks_record_logged() throws Throwable {
        when(configuration.isCheckEnabled()).thenReturn(true);
        when(lookup.isLogged(any())).thenReturn(true);

        assertThat(aspect.check(proceedingJoinPoint)).isNull();
    }

    @Test
    public void checks_record_not_logged() throws Throwable {
        when(configuration.isCheckEnabled()).thenReturn(true);
        when(lookup.isLogged(any())).thenReturn(false);

        assertThat(aspect.check(proceedingJoinPoint)).isNotNull();
    }

    @Test
    public void bubbles_errors_on_check() throws Throwable {
        when(configuration.isCheckEnabled()).thenReturn(true);
        when(configuration.isSuppressErrors()).thenReturn(false);
        when(idExtractor.extract(proceedingJoinPoint)).thenThrow(new IdempotencyCheckException("test"));

        assertThrows(IdempotencyCheckException.class, () -> aspect.check(proceedingJoinPoint));
    }

    @Test
    public void suppresses_errors_on_property() throws Throwable {
        when(configuration.isCheckEnabled()).thenReturn(true);
        when(configuration.isSuppressErrors()).thenReturn(true);
        when(idExtractor.extract(proceedingJoinPoint)).thenThrow(new IdempotencyCheckException("test"));

        assertThat(aspect.check(proceedingJoinPoint)).isNotNull();
    }

    @Test
    public void logs() {
        when(configuration.isPersistenceEnabled()).thenReturn(true);
        when(configuration.getTtl()).thenReturn(Duration.ofSeconds(2));
        when(annotation.ttl()).thenReturn(0);

        aspect.persist(proceedingJoinPoint, annotation);

        verify(lookup).log(recordId, Duration.ofSeconds(2));
    }

    @Test
    public void logs_with_custom_ttl() {
        when(configuration.isPersistenceEnabled()).thenReturn(true);
        when(configuration.getTtl()).thenReturn(Duration.ofSeconds(86400));
        when(annotation.ttl()).thenReturn(10);

        aspect.persist(proceedingJoinPoint, annotation);

        verify(lookup).log(recordId, Duration.ofSeconds(10));
    }

    @Test
    public void logs_disabled() {
        when(configuration.isPersistenceEnabled()).thenReturn(false);

        aspect.persist(proceedingJoinPoint, annotation);

        verify(lookup, never()).log(any(), any());
    }

    @Test
    public void logs_no_param_extracted() throws Throwable {
        when(configuration.isPersistenceEnabled()).thenReturn(true);
        when(idExtractor.extract(proceedingJoinPoint)).thenThrow(new IdempotencyCheckException("test"));

        aspect.persist(proceedingJoinPoint, annotation);

        verify(lookup, never()).log(any(), any());
    }
}
