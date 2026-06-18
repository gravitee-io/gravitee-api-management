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
package io.gravitee.apim.infra.adapter;

import static fixtures.core.model.SwaggerUiConfigurationFixtures.aSwaggerUiConfiguration;
import static fixtures.repository.model.PortalPageContentRepositoryFixtures.anOpenApiPageContent;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalPageContentFixtures;
import io.gravitee.apim.core.async_api.AsyncApi;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.repository.management.model.PortalPageContent;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContentAdapterTest {

    private final PortalPageContentAdapter adapter = PortalPageContentAdapter.INSTANCE;

    @Nested
    class ToEntity {

        @Test
        void should_map_gravitee_markdown_content_to_entity() {
            // Given
            var repositoryContent = io.gravitee.repository.management.model.PortalPageContent.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .type(io.gravitee.repository.management.model.PortalPageContent.Type.GRAVITEE_MARKDOWN)
                .content("# Welcome\n\nThis is a sample page content.")
                .configuration("{}")
                .build();

            // When
            var entity = adapter.toEntity(repositoryContent);

            // Then
            assertThat(entity).isInstanceOf(GraviteeMarkdownPageContent.class);
            var markdownContent = (GraviteeMarkdownPageContent) entity;
            assertThat(markdownContent.getId()).isEqualTo(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440000"));
            assertThat(markdownContent.getContent().value()).isEqualTo("# Welcome\n\nThis is a sample page content.");
        }

        @Test
        void should_map_gravitee_markdown_content_with_empty_configuration() {
            // Given
            var repositoryContent = io.gravitee.repository.management.model.PortalPageContent.builder()
                .id("550e8400-e29b-41d4-a716-446655440001")
                .type(io.gravitee.repository.management.model.PortalPageContent.Type.GRAVITEE_MARKDOWN)
                .content("Simple content without configuration")
                .configuration(null)
                .build();

            // When
            var entity = adapter.toEntity(repositoryContent);

            // Then
            assertThat(entity).isInstanceOf(GraviteeMarkdownPageContent.class);
            var markdownContent = (GraviteeMarkdownPageContent) entity;
            assertThat(markdownContent.getId()).isEqualTo(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440001"));
            assertThat(markdownContent.getContent().value()).isEqualTo("Simple content without configuration");
        }

        @Test
        void should_map_openapi_content_to_entity() {
            // Given
            var repositoryContent = io.gravitee.repository.management.model.PortalPageContent.builder()
                .id("550e8400-e29b-41d4-a716-446655440002")
                .organizationId("ORG")
                .environmentId("ENV")
                .type(io.gravitee.repository.management.model.PortalPageContent.Type.OPENAPI)
                .content("openapi: 3.0.0\ninfo:\n  title: Test API")
                .build();

            // When
            var entity = adapter.toEntity(repositoryContent);

            // Then
            assertThat(entity).isInstanceOf(OpenApiPageContent.class);
            var openApiContent = (OpenApiPageContent) entity;
            assertThat(openApiContent.getId()).isEqualTo(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440002"));
            assertThat(openApiContent.getContent().value()).isEqualTo("openapi: 3.0.0\ninfo:\n  title: Test API");
            assertThat(openApiContent.getViewerSettings()).isInstanceOf(RedocConfiguration.class);
        }

        @Test
        void should_map_openapi_content_with_redoc_configuration_to_entity() {
            // Given
            var repositoryContent = anOpenApiPageContent(
                "550e8400-e29b-41d4-a716-446655440004",
                "openapi: 3.0.0",
                "{\"viewer\":\"REDOC\"}"
            );

            // When
            var entity = adapter.toEntity(repositoryContent);

            // Then
            assertThat(entity).isInstanceOf(OpenApiPageContent.class);
            var openApiContent = (OpenApiPageContent) entity;
            assertThat(openApiContent.getViewerSettings()).isInstanceOfSatisfying(RedocConfiguration.class, configuration ->
                assertThat(configuration.tryItUrl()).isEmpty()
            );
        }

        @Test
        void should_map_openapi_content_with_redoc_try_it_url_configuration_to_entity() {
            // Given
            var repositoryContent = anOpenApiPageContent(
                "550e8400-e29b-41d4-a716-446655440006",
                "openapi: 3.0.0",
                "{\"viewer\":\"REDOC\",\"tryItUrl\":\"https://redoc.example.com\"}"
            );

            // When
            var entity = adapter.toEntity(repositoryContent);

            // Then
            assertThat(entity).isInstanceOf(OpenApiPageContent.class);
            var openApiContent = (OpenApiPageContent) entity;
            assertThat(openApiContent.getViewerSettings()).isInstanceOfSatisfying(RedocConfiguration.class, configuration ->
                assertThat(configuration.tryItUrl()).isEqualTo("https://redoc.example.com")
            );
        }

        @Test
        void should_map_openapi_content_with_swagger_configuration_to_entity() {
            // Given
            var repositoryContent = io.gravitee.repository.management.model.PortalPageContent.builder()
                .id("550e8400-e29b-41d4-a716-446655440005")
                .organizationId("ORG")
                .environmentId("ENV")
                .type(io.gravitee.repository.management.model.PortalPageContent.Type.OPENAPI)
                .content("openapi: 3.0.0")
                .configuration(
                    "{\"viewer\":\"SWAGGER\",\"displayOperationId\":true,\"docExpansion\":\"full\",\"maxDisplayedTags\":3,\"tryItUrl\":\"https://try-it.example.com\",\"contextPathAsServerPath\":true}"
                )
                .build();

            // When
            var entity = adapter.toEntity(repositoryContent);

            // Then
            assertThat(entity).isInstanceOf(OpenApiPageContent.class);
            var openApiContent = (OpenApiPageContent) entity;
            assertThat(openApiContent.getViewerSettings()).isInstanceOfSatisfying(SwaggerUiConfiguration.class, configuration -> {
                assertThat(configuration.displayOperationId()).isTrue();
                assertThat(configuration.docExpansion()).isEqualTo("full");
                assertThat(configuration.maxDisplayedTags()).isEqualTo(3);
                assertThat(configuration.tryItUrl()).isEqualTo("https://try-it.example.com");
                assertThat(configuration.contextPathAsServerPath()).isTrue();
            });
        }

        @Test
        void should_map_asyncapi_content_to_entity() {
            // Given
            var repositoryContent = io.gravitee.repository.management.model.PortalPageContent.builder()
                .id("550e8400-e29b-41d4-a716-446655440003")
                .organizationId("ORG")
                .environmentId("ENV")
                .type(io.gravitee.repository.management.model.PortalPageContent.Type.ASYNCAPI)
                .content("asyncapi: '3.0.0'\ninfo:\n  title: Test API")
                .build();

            // When
            var entity = adapter.toEntity(repositoryContent);

            // Then
            assertThat(entity).isInstanceOf(AsyncApiPageContent.class);
            var asyncApiContent = (AsyncApiPageContent) entity;
            assertThat(asyncApiContent.getId()).isEqualTo(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440003"));
            assertThat(asyncApiContent.getContent().value()).isEqualTo("asyncapi: '3.0.0'\ninfo:\n  title: Test API");
        }
    }

    @Nested
    class ToRepository {

        @Test
        void should_map_gravitee_markdown_content_to_repository() {
            // Given
            final var entityContent = new GraviteeMarkdownPageContent(
                PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440000"),
                "DEFAULT_ORG",
                "DEFAULT_ENV",
                new GraviteeMarkdown("# Welcome\n\nThis is a sample page content.")
            );

            // When
            var repositoryContent = adapter.toRepository(entityContent);

            // Then
            assertThat(repositoryContent.getType()).isEqualTo(PortalPageContent.Type.GRAVITEE_MARKDOWN);
            assertThat(repositoryContent.getId()).isEqualTo(entityContent.getId().toString());
            assertThat(repositoryContent.getOrganizationId()).isEqualTo("DEFAULT_ORG");
            assertThat(repositoryContent.getEnvironmentId()).isEqualTo("DEFAULT_ENV");
            assertThat(repositoryContent.getContent()).isEqualTo(entityContent.getContent().value());
        }

        @Test
        void should_map_asyncapi_content_to_repository() {
            // Given
            final var entityContent = new AsyncApiPageContent(
                PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440003"),
                "DEFAULT_ORG",
                "DEFAULT_ENV",
                new AsyncApi("asyncapi: '3.0.0'\ninfo:\n  title: Test")
            );

            // When
            var repositoryContent = adapter.toRepository(entityContent);

            // Then
            assertThat(repositoryContent.getType()).isEqualTo(PortalPageContent.Type.ASYNCAPI);
            assertThat(repositoryContent.getId()).isEqualTo(entityContent.getId().toString());
            assertThat(repositoryContent.getOrganizationId()).isEqualTo("DEFAULT_ORG");
            assertThat(repositoryContent.getEnvironmentId()).isEqualTo("DEFAULT_ENV");
            assertThat(repositoryContent.getContent()).isEqualTo(entityContent.getContent().value());
        }

        @Test
        void should_map_openapi_content_with_viewer_configuration_to_repository() {
            // Given
            final var entityContent = PortalPageContentFixtures.anOpenApiPageContent(
                PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440004"),
                "DEFAULT_ORG",
                "DEFAULT_ENV",
                "openapi: 3.0.0",
                aSwaggerUiConfiguration()
            );

            // When
            var repositoryContent = adapter.toRepository(entityContent);

            // Then
            assertThat(repositoryContent.getType()).isEqualTo(PortalPageContent.Type.OPENAPI);
            assertThat(repositoryContent.getConfiguration()).contains("\"viewer\":\"SWAGGER\"");
            assertThat(repositoryContent.getConfiguration()).contains("\"docExpansion\":\"full\"");
            assertThat(repositoryContent.getConfiguration()).contains("\"tryItUrl\":\"https://try-it.example.com\"");
            assertThat(repositoryContent.getConfiguration()).contains("\"contextPathAsServerPath\":true");
        }

        @Test
        void should_map_openapi_content_with_redoc_try_it_url_configuration_to_repository() {
            // Given
            final var entityContent = PortalPageContentFixtures.anOpenApiPageContent(
                PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440006"),
                "DEFAULT_ORG",
                "DEFAULT_ENV",
                "openapi: 3.0.0",
                new RedocConfiguration("https://redoc.example.com")
            );

            // When
            var repositoryContent = adapter.toRepository(entityContent);

            // Then
            assertThat(repositoryContent.getType()).isEqualTo(PortalPageContent.Type.OPENAPI);
            assertThat(repositoryContent.getConfiguration()).contains("\"viewer\":\"REDOC\"");
            assertThat(repositoryContent.getConfiguration()).contains("\"tryItUrl\":\"https://redoc.example.com\"");
        }
    }
}
