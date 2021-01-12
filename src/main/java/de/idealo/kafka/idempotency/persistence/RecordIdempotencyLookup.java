package de.idealo.kafka.idempotency.persistence;

import java.time.Duration;

import de.idealo.kafka.idempotency.RecordIdentity;

public interface RecordIdempotencyLookup {

    boolean isLogged(RecordIdentity id);

    void log(final RecordIdentity id, final Duration ttl);
}
