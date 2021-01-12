package de.idealo.kafka.idempotency;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Extracts all available listener method arguments annotated with {@link IdempotencyId} and forms RecordIdentity out of them.
 */
public class RecordIdentityExtractor {

    private static final Set<Class> SUPPORTED_ID_TYPES = new HashSet(Arrays.asList(
            String.class, Long.class, Integer.class, int.class, long.class
    ));

    public RecordIdentity extract(final JoinPoint joinPoint) throws IdempotencyCheckException {
        final var arguments = joinPoint.getArgs();
        final var parameters = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameters();
        final List<String> idComponents = new ArrayList<>();

        for (int i = 0; i < parameters.length; i++) {
            if (isAnnotatedParam(parameters[i])) {
                if (!SUPPORTED_ID_TYPES.contains(parameters[i].getType())) {
                    throw new IdempotencyCheckException("Idempotency check error: only following arguments are currently supported for the IdempotencyId:"
                            + " String, Long, Integer, int, long.");
                }
                idComponents.add(String.valueOf(arguments[i]));
            }
        }

        return new RecordIdentity(idComponents);
    }

    /**
     * Checks if the parameter from the joint point is annotated with {@link IdempotencyId}.
     * @param parameter
     * @return
     */
    private Boolean isAnnotatedParam(AnnotatedElement parameter) {
        var eventIdempotentParam = parameter.getAnnotation(IdempotencyId.class);

        return eventIdempotentParam != null;
    }
}
