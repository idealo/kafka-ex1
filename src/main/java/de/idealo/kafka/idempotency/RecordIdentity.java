package de.idealo.kafka.idempotency;

import java.util.List;

public class RecordIdentity {

    private static final String DELIMITER = "-";
    private final String id;

    public RecordIdentity(final List<String> recordIdComponents) throws IdempotencyCheckException {
        this.id = buildId(recordIdComponents);
    }

    private String buildId(final List<String> recordIdComponents) throws IdempotencyCheckException {
        if (recordIdComponents.size() == 0) {
            throw instantiateException();
        }
        var joint = String.join(DELIMITER, recordIdComponents);
        if (joint == null || joint.equals("")) {
            throw instantiateException();
        }
        return joint;
    }

    private IdempotencyCheckException instantiateException() {
        return new IdempotencyCheckException(
                "No idempotency id arguments found. There must be at least one listener argument annotated with @IdempotencyId and the value must not be empty or null");
    }

    public String toString(final String prefix) {
        return prefix + this.toString();
    }

    @Override
    public String toString() {
        return id;
    }
}
