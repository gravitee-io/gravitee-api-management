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
package io.gravitee.apim.core.portal_page.use_case;

import static fixtures.core.model.PortalPageContentFixtures.CONTENT_ID;
import static fixtures.core.model.PortalPageContentFixtures.ENVIRONMENT_ID;
import static fixtures.core.model.PortalPageContentFixtures.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePortalPageContentConfigurationUseCaseTest {

    private static final String OPENAPI_CONTENT = "openapi: 3.0.3\ninfo:\n  title: Test";

    private UpdatePortalPageContentConfigurationUseCase useCase;

    @BeforeEach
    void setUp() {
        PortalPageContentQueryServiceInMemory queryService = new PortalPageContentQueryServiceInMemory();
        PortalPageContentCrudServiceInMemory crudService = new PortalPageContentCrudServiceInMemory();
        useCase = new UpdatePortalPageContentConfigurationUseCase(queryService, crudService);

        var content = PortalPageContentFixtures.anOpenApiPageContent(
            PortalPageContentId.of(CONTENT_ID),
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            OPENAPI_CONTENT,
            new RedocConfiguration()
        );

        queryService.initWith(List.of(content));
        crudService.initWith(List.of(content));
    }

    @Test
    void should_update_configuration_without_changing_content() {
        // Given
        var configuration = new SwaggerUiConfiguration(
            true,
            "full",
            true,
            10,
            true,
            true,
            true,
            true,
            true,
            true,
            "https://example.com",
            true,
            true,
            true
        );
        var input = UpdatePortalPageContentConfigurationUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .configuration(configuration)
            .build();

        // When
        var output = useCase.execute(input);

        // Then
        assertThat(output.portalPageContent().getContent().value()).isEqualTo(OPENAPI_CONTENT);
        assertThat(output.portalPageContent().getViewerSettings()).isEqualTo(configuration);
    }

    @Test
    void should_throw_when_content_not_found() {
        // Given
        var input = UpdatePortalPageContentConfigurationUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId("00000000-0000-0000-0000-000000000002")
            .configuration(new RedocConfiguration())
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
    }

    @Test
    void should_throw_when_content_is_not_openapi() {
        // Given
        PortalPageContentQueryServiceInMemory queryService = new PortalPageContentQueryServiceInMemory();
        PortalPageContentCrudServiceInMemory crudService = new PortalPageContentCrudServiceInMemory();
        useCase = new UpdatePortalPageContentConfigurationUseCase(queryService, crudService);
        var content = PortalPageContentFixtures.aGraviteeMarkdownPageContent();
        queryService.initWith(List.of(content));
        crudService.initWith(List.of(content));

        var input = UpdatePortalPageContentConfigurationUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .configuration(new RedocConfiguration())
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
    }
}
