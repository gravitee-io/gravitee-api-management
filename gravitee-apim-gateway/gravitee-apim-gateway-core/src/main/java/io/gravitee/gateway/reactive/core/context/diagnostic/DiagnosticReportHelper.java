package io.gravitee.gateway.reactive.core.context.diagnostic;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.reporter.api.diagnostic.Diagnostic;
import java.util.Objects;
import org.springframework.core.NestedExceptionUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DiagnosticReportHelper {

    public static final String UNKNOWN_TECHNICAL_ERROR_MESSAGE = "Unknown technical error";
    public static final String INTERNAL_ERROR = "internal_error";

    private DiagnosticReportHelper() {}

    public static Diagnostic fromExecutionFailure(
        Diagnostic.ComponentType componentType,
        String componentName,
        ExecutionFailure executionFailure
    ) {
        String key = executionFailure.key() != null ? executionFailure.key() : INTERNAL_ERROR;
        String message = executionFailure.message() != null ? executionFailure.message() : UNKNOWN_TECHNICAL_ERROR_MESSAGE;
        Throwable cause = executionFailure.cause();

        if (cause != null) {
            String causeMessage = NestedExceptionUtils.getMostSpecificCause(cause).getMessage();
            message += " (" + Objects.requireNonNullElseGet(causeMessage, () -> cause.getClass().getSimpleName()) + ")";
        }

        return new Diagnostic(componentType, componentName, key, message);
    }

    public static Diagnostic fromThrowable(Diagnostic.ComponentType componentType, String componentName, Throwable throwable) {
        String key = INTERNAL_ERROR;
        String message = UNKNOWN_TECHNICAL_ERROR_MESSAGE;

        if (throwable instanceof InterruptionFailureException interruptionFailureException) {
            key = interruptionFailureException.getExecutionFailure().key();
            message = interruptionFailureException.getExecutionFailure().message();

            String causeMessage = NestedExceptionUtils.getMostSpecificCause(interruptionFailureException.getCause()).getMessage();
            if (causeMessage != null) {
                message += " (" + causeMessage + ")";
            }
        } else if (throwable != null) {
            message = NestedExceptionUtils.getMostSpecificCause(throwable).getMessage();

            if (message == null) {
                message += UNKNOWN_TECHNICAL_ERROR_MESSAGE + " (" + throwable.getClass().getSimpleName() + ")";
            }
        }

        return new Diagnostic(componentType, componentName, key, message);
    }

    public static Diagnostic fromExecutionWarn(Diagnostic.ComponentType componentType, String componentName, ExecutionWarn executionWarn) {
        String key = executionWarn.key() != null ? executionWarn.key() : INTERNAL_ERROR;
        String message = executionWarn.message() != null ? executionWarn.message() : UNKNOWN_TECHNICAL_ERROR_MESSAGE;
        Throwable cause = executionWarn.cause();

        if (cause != null) {
            String causeMessage = NestedExceptionUtils.getMostSpecificCause(cause).getMessage();
            message += " (" + Objects.requireNonNullElseGet(causeMessage, () -> cause.getClass().getSimpleName()) + ")";
        }

        return new Diagnostic(componentType, componentName, key, message);
    }
}
