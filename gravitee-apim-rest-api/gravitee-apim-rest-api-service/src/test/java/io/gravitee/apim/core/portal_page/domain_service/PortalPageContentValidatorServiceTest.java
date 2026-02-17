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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContentValidatorServiceTest {

    private PortalPageContentValidator mockValidator;
    private PortalPageContentValidatorService service;

    @BeforeEach
    void setUp() {
        mockValidator = mock(PortalPageContentValidator.class);
        service = new PortalPageContentValidatorService(List.of(mockValidator));
    }

    @Test
    void should_call_applicable_validators() {
        // Given
        PortalPageContent existingContent = GraviteeMarkdownPageContent.create("org", "env", "old");
        UpdatePortalPageContent updateContent = UpdatePortalPageContent.builder().content("new content").build();
        when(mockValidator.appliesTo(existingContent)).thenReturn(true);

        // When
        service.validateForUpdate(existingContent, updateContent);

        // Then
        verify(mockValidator).appliesTo(existingContent);
        verify(mockValidator).validate(updateContent);
    }

    @Test
    void should_skip_non_applicable_validators() {
        // Given
        PortalPageContent existingContent = GraviteeMarkdownPageContent.create("org", "env", "old");
        UpdatePortalPageContent updateContent = UpdatePortalPageContent.builder().content("new content").build();
        when(mockValidator.appliesTo(existingContent)).thenReturn(false);

        // When
        service.validateForUpdate(existingContent, updateContent);

        // Then
        verify(mockValidator).appliesTo(existingContent);
        verify(mockValidator, never()).validate(any());
    }
}
