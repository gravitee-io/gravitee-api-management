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
package io.gravitee.apim.core.portal_page.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import io.gravitee.apim.core.portal_page.exception.EmptyPortalPageContentException;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContentValidatorServiceTest {

    private final PortalPageContentValidatorService validatorService = new PortalPageContentValidatorService();

    @Nested
    class ValidateForUpdate {

        @Test
        void should_validate_successfully_when_content_is_not_empty() {
            // Given
            final var updatePortalPageContent = UpdatePortalPageContent.builder().content("Valid content").build();

            // When
            validatorService.validateForUpdate(updatePortalPageContent);

            // Then
            // No exception thrown
        }

        @Test
        void should_throw_exception_when_content_is_null() {
            // Given
            final var updatePortalPageContent = UpdatePortalPageContent.builder().content(null).build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateForUpdate(updatePortalPageContent);

            // Then
            Exception exception = assertThrows(EmptyPortalPageContentException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Content must not be null or empty");
        }

        @Test
        void should_throw_exception_when_content_is_empty() {
            // Given
            final var updatePortalPageContent = UpdatePortalPageContent.builder().content("").build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateForUpdate(updatePortalPageContent);

            // Then
            Exception exception = assertThrows(EmptyPortalPageContentException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Content must not be null or empty");
        }

        @Test
        void should_throw_exception_when_content_is_only_whitespace() {
            // Given
            final var updatePortalPageContent = UpdatePortalPageContent.builder().content("   ").build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateForUpdate(updatePortalPageContent);

            // Then
            Exception exception = assertThrows(EmptyPortalPageContentException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Content must not be null or empty");
        }
    }
}
