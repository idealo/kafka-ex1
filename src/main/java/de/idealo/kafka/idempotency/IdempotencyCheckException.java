package de.idealo.kafka.idempotency;

/**
 * Indicates that an errors has occurred while trying to execute the idempotency check, demanded by {@link IdempotencyId}.
 */
public class IdempotencyCheckException extends Exception {
    public IdempotencyCheckException(final String message) {
        super(message);
    }

    public IdempotencyCheckException(final Throwable cause) {
        super(cause);
    }
}
