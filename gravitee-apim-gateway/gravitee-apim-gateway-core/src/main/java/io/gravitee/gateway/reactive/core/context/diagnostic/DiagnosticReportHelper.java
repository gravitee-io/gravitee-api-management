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

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionIssue;
import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.core.context.ComponentScope;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.core.NestedExceptionUtils;

public class DiagnosticReportHelper {

    public static final String UNKNOWN_TECHNICAL_ERROR_MESSAGE = "Unknown technical error";
    public static final String INTERNAL_ERROR = "internal_error";
    public static final String UNKNOWN_COMPONENT = "Unknown component";

    private static final Pattern EXCEPTION_SUFFIX = Pattern.compile("Exception$");
    private static final Pattern CAMEL_CASE_SPLIT = Pattern.compile("([a-z])([A-Z])");

    private DiagnosticReportHelper() {}

    public static Diagnostic fromExecutionFailure(
        ComponentScope.ComponentEntry component,
        String legacyErrorKey,
        String legacyErrorMessage,
        ExecutionFailure failure
    ) {
        return fromIssue(component, failure, legacyErrorKey, legacyErrorMessage);
    }

    public static Diagnostic fromExecutionWarn(ComponentScope.ComponentEntry component, ExecutionWarn warn) {
        return fromIssue(component, warn, null, null);
    }

    private static Diagnostic fromIssue(
        ComponentScope.ComponentEntry component,
        ExecutionIssue issue,
        String legacyKey,
        String legacyMessage
    ) {
        String key = Optional.ofNullable(issue.key()).orElse(Optional.ofNullable(legacyKey).orElse(INTERNAL_ERROR));

        String message = Optional.ofNullable(issue.message()).orElse(
            Optional.ofNullable(legacyMessage).orElse(UNKNOWN_TECHNICAL_ERROR_MESSAGE)
        );

        message = withCauseSuffix(message, issue.cause());

        return buildDiagnostic(component, key, message);
    }

    private static Diagnostic buildDiagnostic(ComponentScope.ComponentEntry component, String key, String message) {
        String componentType = Optional.ofNullable(component)
            .map(ComponentScope.ComponentEntry::type)
            .map(Enum::name)
            .orElse(UNKNOWN_COMPONENT);

        String componentName = Optional.ofNullable(component).map(ComponentScope.ComponentEntry::name).orElse(UNKNOWN_COMPONENT);

        return new Diagnostic(key, message, componentType, componentName);
    }

    private static String withCauseSuffix(String base, Throwable cause) {
        if (cause == null) return base;
        Throwable mostSpecific = NestedExceptionUtils.getMostSpecificCause(cause);
        return base + " (" + prettifyThrowableName(mostSpecific) + ")";
    }

    private static String prettifyThrowableName(Throwable t) {
        return Optional.ofNullable(t.getMessage())
            .filter(s -> !s.isBlank())
            .orElseGet(() -> {
                String name = t.getClass().getSimpleName();
                name = EXCEPTION_SUFFIX.matcher(name).replaceAll("");
                name = CAMEL_CASE_SPLIT.matcher(name).replaceAll("$1 $2").trim();
                return name;
            });
    }
}
