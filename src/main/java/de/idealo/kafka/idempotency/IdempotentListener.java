package de.idealo.kafka.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IdempotentListener {
    /**
     * Listener-specific TTL for the idempotency information, in seconds.
     * It overrides the globally-set default TTL.
     * Defaults to 0, which means the global property is used.
     * @return TTL for the idempotency information, in seconds
     */
    int ttl() default 0;
}
