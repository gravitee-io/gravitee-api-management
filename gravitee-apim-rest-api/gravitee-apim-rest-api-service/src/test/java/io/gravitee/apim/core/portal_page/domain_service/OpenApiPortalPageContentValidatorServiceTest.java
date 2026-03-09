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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.open_api.OpenApiValidator;
import io.gravitee.apim.core.open_api.exception.OpenApiContentEmptyException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenApiPortalPageContentValidatorServiceTest {

    private OpenApiValidator openApiValidator;
    private OpenApiPortalPageContentValidatorService validator;

    @BeforeEach
    void setUp() {
        openApiValidator = mock(OpenApiValidator.class);
        validator = new OpenApiPortalPageContentValidatorService(openApiValidator);
    }

    @Test
    void should_apply_to_open_api_content() {
        // Given
        PortalPageContent<?> content = OpenApiPageContent.create("org", "env", "openapi: 3.0.0");

        // When
        boolean result = validator.appliesTo(content);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void should_not_apply_to_gravitee_markdown_content() {
        // Given
        PortalPageContent<?> content = GraviteeMarkdownPageContent.create("org", "env", "content");

        // When
        boolean result = validator.appliesTo(content);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void should_delegate_validation_to_open_api_validator() {
        // Given
        UpdatePortalPageContent updateContent = UpdatePortalPageContent.builder().content("openapi: 3.0.0").build();

        // When
        validator.validate(updateContent);

        // Then
        verify(openApiValidator).validateNotEmpty(eq(new OpenApi("openapi: 3.0.0")));
    }

    @Test
    void should_throw_when_validator_rejects_content() {
        // Given
        UpdatePortalPageContent updateContent = UpdatePortalPageContent.builder().content("").build();
        doThrow(new OpenApiContentEmptyException()).when(openApiValidator).validateNotEmpty(any());

        // When / Then
        assertThatThrownBy(() -> validator.validate(updateContent))
            .isInstanceOf(OpenApiContentEmptyException.class)
            .hasMessage("Content must not be null or empty");
    }
}
