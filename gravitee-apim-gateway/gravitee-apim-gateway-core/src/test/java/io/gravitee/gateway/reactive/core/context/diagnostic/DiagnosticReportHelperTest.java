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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactive.api.ComponentType;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class DiagnosticReportHelperTest {

    @Nested
    @DisplayName("fromExecutionFailure")
    class FromExecutionFailureTests {

        @Test
        @DisplayName("Should create diagnostic with execution failure key and message")
        void shouldCreateDiagnosticWithExecutionFailureKeyAndMessage() {
            // Given
            ExecutionFailure executionFailure = new ExecutionFailure(400).key("INVALID_REQUEST").message("The request is invalid");

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(
                ComponentType.POLICY,
                "test-policy",
                "legacy-key",
                "legacy-message",
                executionFailure
            );

            // Then
            assertThat(diagnostic.getKey()).isEqualTo("INVALID_REQUEST");
            assertThat(diagnostic.getMessage()).isEqualTo("The request is invalid");
            assertThat(diagnostic.getComponentType()).isEqualTo("POLICY");
            assertThat(diagnostic.getComponentName()).isEqualTo("test-policy");
        }

        @Test
        @DisplayName("Should fallback to legacy values when execution failure values are null")
        void shouldFallbackToLegacyValuesWhenExecutionFailureValuesAreNull() {
            // Given
            ExecutionFailure executionFailure = new ExecutionFailure(400);

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(
                ComponentType.POLICY,
                "test-policy",
                "legacy-key",
                "legacy-message",
                executionFailure
            );

            // Then
            assertThat(diagnostic.getKey()).isEqualTo("legacy-key");
            assertThat(diagnostic.getMessage()).isEqualTo("legacy-message");
        }

        @Test
        @DisplayName("Should use default values when all values are null")
        void shouldUseDefaultValuesWhenAllValuesAreNull() {
            // Given
            ExecutionFailure executionFailure = new ExecutionFailure(400);

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(null, null, null, null, executionFailure);

            // Then
            assertThat(diagnostic.getKey()).isEqualTo(DiagnosticReportHelper.INTERNAL_ERROR);
            assertThat(diagnostic.getMessage()).isEqualTo(DiagnosticReportHelper.UNKNOWN_TECHNICAL_ERROR_MESSAGE);
            assertThat(diagnostic.getComponentType()).isEqualTo(DiagnosticReportHelper.UNKNOWN_COMPONENT);
            assertThat(diagnostic.getComponentName()).isEqualTo(DiagnosticReportHelper.UNKNOWN_COMPONENT);
        }

        @Test
        @DisplayName("Should include cause information when present")
        void shouldIncludeCauseInformationWhenPresent() {
            // Given
            RuntimeException cause = new RuntimeException("Root cause");
            ExecutionFailure executionFailure = new ExecutionFailure(500)
                .key("INTERNAL_ERROR")
                .message("Something went wrong")
                .cause(cause);

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(
                ComponentType.POLICY,
                "test-policy",
                null,
                null,
                executionFailure
            );

            // Then
            assertThat(diagnostic.getMessage()).isEqualTo("Something went wrong (Root cause)");
        }

        @Test
        @DisplayName("Should prettify exception name when cause has no message")
        void shouldPrettifyExceptionNameWhenCauseHasNoMessage() {
            // Given
            RuntimeException cause = new RuntimeException();
            ExecutionFailure executionFailure = new ExecutionFailure(500)
                .key("INTERNAL_ERROR")
                .message("Something went wrong")
                .cause(cause);

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(
                ComponentType.POLICY,
                "test-policy",
                null,
                null,
                executionFailure
            );

            // Then
            assertThat(diagnostic.getMessage()).isEqualTo("Something went wrong (Runtime)");
        }

        @Test
        @DisplayName("Should handle nested exceptions correctly")
        void shouldHandleNestedExceptionsCorrectly() {
            // Given
            RuntimeException rootCause = new RuntimeException("Root cause");
            IllegalStateException wrapper = new IllegalStateException("Wrapper", rootCause);
            ExecutionFailure executionFailure = new ExecutionFailure(500)
                .key("INTERNAL_ERROR")
                .message("Something went wrong")
                .cause(wrapper);

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(
                ComponentType.POLICY,
                "test-policy",
                null,
                null,
                executionFailure
            );

            // Then
            assertThat(diagnostic.getMessage()).isEqualTo("Something went wrong (Root cause)");
        }
    }

    @Nested
    @DisplayName("fromExecutionWarn")
    class FromExecutionWarnTests {

        @Test
        @DisplayName("Should create diagnostic with execution warn key and message")
        void shouldCreateDiagnosticWithExecutionWarnKeyAndMessage() {
            // Given
            ExecutionWarn executionWarn = new ExecutionWarn("WARNING_KEY").message("This is a warning");

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionWarn(ComponentType.POLICY, "test-policy", executionWarn);

            // Then
            assertThat(diagnostic.getKey()).isEqualTo("WARNING_KEY");
            assertThat(diagnostic.getMessage()).isEqualTo("This is a warning");
            assertThat(diagnostic.getComponentType()).isEqualTo("POLICY");
            assertThat(diagnostic.getComponentName()).isEqualTo("test-policy");
        }

        @Test
        @DisplayName("Should use default values when execution warn values are null")
        void shouldUseDefaultValuesWhenExecutionWarnValuesAreNull() {
            // Given
            ExecutionWarn executionWarn = new ExecutionWarn(null);

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionWarn(ComponentType.POLICY, "test-policy", executionWarn);

            // Then
            assertThat(diagnostic.getKey()).isEqualTo(DiagnosticReportHelper.INTERNAL_ERROR);
            assertThat(diagnostic.getMessage()).isEqualTo(DiagnosticReportHelper.UNKNOWN_TECHNICAL_ERROR_MESSAGE);
        }

        @Test
        @DisplayName("Should use default component values when null")
        void shouldUseDefaultComponentValuesWhenNull() {
            // Given
            ExecutionWarn executionWarn = new ExecutionWarn("WARNING_KEY").message("This is a warning");

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionWarn(null, null, executionWarn);

            // Then
            assertThat(diagnostic.getComponentType()).isEqualTo(DiagnosticReportHelper.UNKNOWN_COMPONENT);
            assertThat(diagnostic.getComponentName()).isEqualTo(DiagnosticReportHelper.UNKNOWN_COMPONENT);
        }

        @Test
        @DisplayName("Should include cause information when present")
        void shouldIncludeCauseInformationWhenPresent() {
            // Given
            RuntimeException cause = new RuntimeException("Warning cause");
            ExecutionWarn executionWarn = new ExecutionWarn("WARNING_KEY").message("This is a warning").cause(cause);

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionWarn(ComponentType.POLICY, "test-policy", executionWarn);

            // Then
            assertThat(diagnostic.getMessage()).isEqualTo("This is a warning (Warning cause)");
        }

        @Test
        @DisplayName("Should prettify exception name when cause has no message")
        void shouldPrettifyExceptionNameWhenCauseHasNoMessage() {
            // Given
            RuntimeException cause = new RuntimeException();
            ExecutionWarn executionWarn = new ExecutionWarn("WARNING_KEY").message("This is a warning").cause(cause);

            // When
            Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionWarn(ComponentType.POLICY, "test-policy", executionWarn);

            // Then
            assertThat(diagnostic.getMessage()).isEqualTo("This is a warning (Runtime)");
        }
    }

    @Nested
    @DisplayName("prettifyThrowableName")
    class PrettifyThrowableNameTests {

        @Test
        @DisplayName("Should return message when throwable has non-blank message")
        void shouldReturnMessageWhenThrowableHasNonBlankMessage() {
            // Given
            RuntimeException throwable = new RuntimeException("Custom error message");

            // When
            String result = invokePrettifyThrowableName(throwable);

            // Then
            assertThat(result).isEqualTo("Custom error message");
        }

        @Test
        @DisplayName("Should prettify class name when message is blank")
        void shouldPrettifyClassNameWhenMessageIsBlank() {
            // Given
            RuntimeException throwable = new RuntimeException("");

            // When
            String result = invokePrettifyThrowableName(throwable);

            // Then
            assertThat(result).isEqualTo("Runtime");
        }

        @Test
        @DisplayName("Should prettify class name when message is null")
        void shouldPrettifyClassNameWhenMessageIsNull() {
            // Given
            RuntimeException throwable = new RuntimeException((String) null);

            // When
            String result = invokePrettifyThrowableName(throwable);

            // Then
            assertThat(result).isEqualTo("Runtime");
        }

        @Test
        @DisplayName("Should remove Exception suffix from class name")
        void shouldRemoveExceptionSuffixFromClassName() {
            // Given
            CustomException throwable = new CustomException();

            // When
            String result = invokePrettifyThrowableName(throwable);

            // Then
            assertThat(result).isEqualTo("Custom");
        }

        @Test
        @DisplayName("Should split camelCase class names")
        void shouldSplitCamelCaseClassNames() {
            // Given
            CustomCamelCaseException throwable = new CustomCamelCaseException();

            // When
            String result = invokePrettifyThrowableName(throwable);

            // Then
            assertThat(result).isEqualTo("Custom Camel Case");
        }

        // Helper method to access private method via reflection
        private String invokePrettifyThrowableName(Throwable throwable) {
            try {
                var method = DiagnosticReportHelper.class.getDeclaredMethod("prettifyThrowableName", Throwable.class);
                method.setAccessible(true);
                return (String) method.invoke(null, throwable);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke prettifyThrowableName", e);
            }
        }
    }

    // Test exception classes
    private static class CustomException extends RuntimeException {}

    private static class CustomCamelCaseException extends RuntimeException {}
}
