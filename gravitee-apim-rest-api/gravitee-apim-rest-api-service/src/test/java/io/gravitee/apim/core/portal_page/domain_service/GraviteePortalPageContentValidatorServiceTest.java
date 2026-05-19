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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.api.service_provider.ApiTemplateModelProvider;
import io.gravitee.apim.core.environment.service_provider.EnvironmentTemplateModelProvider;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GraviteePortalPageContentValidatorServiceTest {

    private GraviteeMarkdownValidator gmdValidator;
    private PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private PortalNavigationEnclosingApiDomainService portalNavigationEnclosingApiDomainService;
    private PortalNavigationTemplatingService portalNavigationTemplatingService;
    private ApiTemplateModelProvider apiTemplateModelProvider;
    private EnvironmentTemplateModelProvider environmentTemplateModelProvider;
    private GraviteePortalPageContentValidatorService validator;

    @BeforeEach
    void setUp() {
        gmdValidator = mock(GraviteeMarkdownValidator.class);
        portalNavigationItemsQueryService = mock(PortalNavigationItemsQueryService.class);
        portalNavigationEnclosingApiDomainService = mock(PortalNavigationEnclosingApiDomainService.class);
        portalNavigationTemplatingService = mock(PortalNavigationTemplatingService.class);
        apiTemplateModelProvider = mock(ApiTemplateModelProvider.class);
        environmentTemplateModelProvider = mock(EnvironmentTemplateModelProvider.class);
        when(environmentTemplateModelProvider.getEnvironmentMetadata(any())).thenReturn(Map.of());
        validator = new GraviteePortalPageContentValidatorService(
            gmdValidator,
            portalNavigationItemsQueryService,
            portalNavigationEnclosingApiDomainService,
            portalNavigationTemplatingService,
            apiTemplateModelProvider,
            environmentTemplateModelProvider
        );
    }

    @Test
    void should_apply_to_gravitee_markdown_content() {
        // Given
        PortalPageContent<?> content = GraviteeMarkdownPageContent.create("org", "env", "content");

        // When
        boolean result = validator.appliesTo(content);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void should_delegate_validation_to_gmd_validator() {
        // Given
        PortalPageContent<?> existing = GraviteeMarkdownPageContent.create("org", "env", "old");
        UpdatePortalPageContent updateContent = UpdatePortalPageContent.builder().content("test content").build();
        when(portalNavigationItemsQueryService.findNavigationPageByPortalPageContentId(any(), any())).thenReturn(Optional.empty());

        // When
        validator.validate(existing, updateContent);

        // Then
        verify(gmdValidator).validateNotEmpty(argThat(gmd -> gmd.value().equals("test content")));
        verify(portalNavigationTemplatingService, never()).renderGraviteeMarkdown(any());
    }

    @Test
    void should_throw_when_validator_rejects_content() {
        // Given
        PortalPageContent<?> existing = GraviteeMarkdownPageContent.create("org", "env", "old");
        UpdatePortalPageContent updateContent = UpdatePortalPageContent.builder().content("").build();
        doThrow(new GraviteeMarkdownContentEmptyException()).when(gmdValidator).validateNotEmpty(any());

        // When / Then
        assertThatThrownBy(() -> validator.validate(existing, updateContent))
            .isInstanceOf(GraviteeMarkdownContentEmptyException.class)
            .hasMessage("Content must not be null or empty");
    }

    @Test
    void should_map_template_failure_to_validation_exception_when_page_linked_without_api_ancestor() {
        var contentId = PortalPageContentId.random();
        var existing = new GraviteeMarkdownPageContent(
            contentId,
            PortalNavigationItemFixtures.ORG_ID,
            PortalNavigationItemFixtures.ENV_ID,
            GraviteeMarkdown.of("Hello ${metadata.nope}")
        );
        var page = PortalNavigationItemFixtures.aPage("00000000-0000-0000-0000-000000000081", "Page", null, contentId);
        page.markAsRoot();
        when(
            portalNavigationItemsQueryService.findNavigationPageByPortalPageContentId(
                eq(PortalNavigationItemFixtures.ENV_ID),
                eq(contentId)
            )
        ).thenReturn(Optional.of(page));
        when(portalNavigationEnclosingApiDomainService.findEnclosingApiId(eq(PortalNavigationItemFixtures.ENV_ID), eq(page))).thenReturn(
            Optional.empty()
        );
        when(portalNavigationTemplatingService.renderGraviteeMarkdown(any())).thenThrow(new PortalPageContentTemplateException("missing"));

        var update = UpdatePortalPageContent.builder().content("Hello ${metadata.nope}").build();

        assertThatThrownBy(() -> validator.validate(existing, update)).isInstanceOf(InvalidPortalPageContentTemplateException.class);
        verify(portalNavigationTemplatingService).renderGraviteeMarkdown(argThat(input -> input.model().containsKey("metadata")));
    }

    @Test
    void should_validate_under_api_context_when_enclosing_api_exists() {
        var contentId = PortalPageContentId.random();
        var existing = new GraviteeMarkdownPageContent(
            contentId,
            PortalNavigationItemFixtures.ORG_ID,
            PortalNavigationItemFixtures.ENV_ID,
            GraviteeMarkdown.of("${api.name}")
        );
        var folder = PortalNavigationItemFixtures.aFolder("00000000-0000-0000-0000-000000000080", "Folder");
        folder.markAsRoot();
        var apiNode = PortalNavigationItemFixtures.anApi("00000000-0000-0000-0000-000000000082", "API", folder.getId(), "api-1");
        apiNode.updateParent(folder);
        var page = PortalNavigationItemFixtures.aPage("00000000-0000-0000-0000-000000000083", "Page", apiNode.getId(), contentId);
        page.updateParent(apiNode);

        when(
            portalNavigationItemsQueryService.findNavigationPageByPortalPageContentId(
                eq(PortalNavigationItemFixtures.ENV_ID),
                eq(contentId)
            )
        ).thenReturn(Optional.of(page));
        when(portalNavigationEnclosingApiDomainService.findEnclosingApiId(eq(PortalNavigationItemFixtures.ENV_ID), eq(page))).thenReturn(
            Optional.of("api-1")
        );
        var fakeApiModel = new Object();
        when(
            apiTemplateModelProvider.getApiTemplateModel(
                eq(PortalNavigationItemFixtures.ORG_ID),
                eq(PortalNavigationItemFixtures.ENV_ID),
                eq("api-1")
            )
        ).thenReturn(fakeApiModel);

        var update = UpdatePortalPageContent.builder().content("${api.name}").build();
        validator.validate(existing, update);

        verify(portalNavigationTemplatingService).renderGraviteeMarkdown(
            argThat(input -> input.rawMarkdown().value().equals("${api.name}") && input.model().get("api") == fakeApiModel)
        );
    }
}
