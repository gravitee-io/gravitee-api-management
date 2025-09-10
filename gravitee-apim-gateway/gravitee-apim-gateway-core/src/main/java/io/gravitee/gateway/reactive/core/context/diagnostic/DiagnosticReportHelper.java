/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.core.context.diagnostic;

import io.gravitee.gateway.reactive.api.ComponentType;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.NestedExceptionUtils;

public class DiagnosticReportHelper {

    public static final String UNKNOWN_TECHNICAL_ERROR_MESSAGE = "Unknown technical error";
    public static final String INTERNAL_ERROR = "internal_error";
    public static final String UNKNOWN_COMPONENT = "Unknown component";

    private static final Pattern EXCEPTION_SUFFIX = Pattern.compile("Exception$");
    private static final Pattern CAMEL_CASE_SPLIT = Pattern.compile("([a-z])([A-Z])");

    private DiagnosticReportHelper() {}

    public static Diagnostic fromExecutionFailure(
        ComponentType componentType,
        String componentName,
        String legacyErrorKey,
        String legacyErrorMessage,
        ExecutionFailure executionFailure
    ) {
        String key = Optional
            .ofNullable(executionFailure.key())
            .orElseGet(() -> Optional.ofNullable(legacyErrorKey).orElse(INTERNAL_ERROR));
        String message = Optional
            .ofNullable(executionFailure.message())
            .orElseGet(() -> Optional.ofNullable(legacyErrorMessage).orElse(UNKNOWN_TECHNICAL_ERROR_MESSAGE));
        Throwable cause = executionFailure.cause();

        if (cause != null) {
            message += " (" + prettifyThrowableName(NestedExceptionUtils.getMostSpecificCause(cause)) + ")";
        }

        return new Diagnostic(
            key,
            message,
            Optional.ofNullable(componentType).map(Enum::name).orElse(UNKNOWN_COMPONENT),
            Optional.ofNullable(componentName).orElse(UNKNOWN_COMPONENT)
        );
    }

    public static Diagnostic fromExecutionWarn(ComponentType componentType, String componentName, ExecutionWarn executionWarn) {
        String key = Optional.ofNullable(executionWarn.key()).orElse(INTERNAL_ERROR);
        String message = Optional.ofNullable(executionWarn.message()).orElse(UNKNOWN_TECHNICAL_ERROR_MESSAGE);
        Throwable cause = executionWarn.cause();

        if (cause != null) {
            message += " (" + prettifyThrowableName(NestedExceptionUtils.getMostSpecificCause(cause)) + ")";
        }

        return new Diagnostic(
            key,
            message,
            Optional.ofNullable(componentType).map(Enum::name).orElse(UNKNOWN_COMPONENT),
            Optional.ofNullable(componentName).orElse(UNKNOWN_COMPONENT)
        );
    }

    private static String prettifyThrowableName(Throwable t) {
        return Optional
            .ofNullable(t.getMessage())
            .filter(s -> !s.isBlank())
            .orElseGet(() -> {
                String name = t.getClass().getSimpleName();
                name = EXCEPTION_SUFFIX.matcher(name).replaceAll("");
                name = CAMEL_CASE_SPLIT.matcher(name).replaceAll("$1 $2").trim();
                return name;
            });
    }
}
