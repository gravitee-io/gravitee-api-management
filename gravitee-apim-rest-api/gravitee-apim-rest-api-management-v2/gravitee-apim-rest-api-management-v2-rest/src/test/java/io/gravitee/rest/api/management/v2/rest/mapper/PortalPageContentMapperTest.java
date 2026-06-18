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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalPageContentFixtures;
import fixtures.core.model.SwaggerUiConfigurationFixtures;
import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.BasePortalPageOpenApiConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageContentType;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageOpenApiConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageRedocConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageSwaggerConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContentMapperTest {

    private PortalPageContentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = PortalPageContentMapper.INSTANCE;
    }

    @Test
    void should_map_gravitee_markdown_page_content() {
        // Given
        var contentId = PortalPageContentId.of("12345678-1234-1234-1234-123456789abc");
        var markdownContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            contentId,
            "ORG",
            "ENV",
            "# Welcome\n\nThis is a test page."
        );

        // When
        var result = mapper.map(markdownContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("12345678-1234-1234-1234-123456789abc");
        assertThat(result.getContent()).isEqualTo("# Welcome\n\nThis is a test page.");
        assertThat(result.getType()).isEqualTo(PortalPageContentType.GRAVITEE_MARKDOWN);
    }

    @Test
    void should_map_portal_page_content_id_to_string() {
        // Given
        var contentId = PortalPageContentId.of("abcd1234-5678-9012-3456-789012345678");

        // When
        var result = mapper.map(contentId);

        // Then
        assertThat(result).isEqualTo("abcd1234-5678-9012-3456-789012345678");
    }

    @Test
    void should_map_portal_page_content_polymorphically() {
        // Given
        var contentId = PortalPageContentId.random();
        var markdownContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(contentId, "ORG", "ENV", "Test content");

        // When
        var result = mapper.map((io.gravitee.apim.core.portal_page.model.PortalPageContent) markdownContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(contentId.json());
        assertThat(result.getContent()).isEqualTo("Test content");
        assertThat(result.getType()).isEqualTo(PortalPageContentType.GRAVITEE_MARKDOWN);
    }

    @Test
    void should_map_asyncapi_page_content() {
        // Given
        var contentId = PortalPageContentId.of("12345678-1234-1234-1234-123456789abd");
        var asyncApiContent = PortalPageContentFixtures.anAsyncApiPageContent(
            contentId,
            "ORG",
            "ENV",
            "asyncapi: '3.0.0'\ninfo:\n  title: Test AsyncAPI"
        );

        // When
        var result = mapper.map(asyncApiContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("12345678-1234-1234-1234-123456789abd");
        assertThat(result.getContent()).isEqualTo("asyncapi: '3.0.0'\ninfo:\n  title: Test AsyncAPI");
        assertThat(result.getType()).isEqualTo(PortalPageContentType.ASYNCAPI);
    }

    @Test
    void should_map_openapi_page_content_configuration_to_dedicated_schema() {
        // Given
        var content = new OpenApiPageContent(
            PortalPageContentId.of("12345678-1234-1234-1234-123456789abc"),
            "ORG",
            "ENV",
            OpenApi.of("openapi: 3.0.0"),
            SwaggerUiConfigurationFixtures.aSwaggerUiConfiguration()
        );

        // When
        var result = mapper.map(content);

        // Then
        assertThat(result.getConfiguration())
            .isNotNull()
            .extracting(PortalPageOpenApiConfiguration::getActualInstance)
            .isInstanceOfSatisfying(PortalPageSwaggerConfiguration.class, configuration -> {
                assertThat(configuration.getViewer()).isEqualTo(BasePortalPageOpenApiConfiguration.ViewerEnum.SWAGGER);
                assertThat(configuration.getDisplayOperationId()).isTrue();
                assertThat(configuration.getDocExpansion()).isEqualTo(PortalPageSwaggerConfiguration.DocExpansionEnum.FULL);
                assertThat(configuration.getEnableFiltering()).isFalse();
                assertThat(configuration.getMaxDisplayedTags()).isEqualTo(12);
                assertThat(configuration.getShowCommonExtensions()).isTrue();
                assertThat(configuration.getShowExtensions()).isFalse();
                assertThat(configuration.getShowURL()).isTrue();
                assertThat(configuration.getTryIt()).isFalse();
                assertThat(configuration.getDisableSyntaxHighlight()).isTrue();
                assertThat(configuration.getTryItAnonymous()).isFalse();
                assertThat(configuration.getTryItURL()).isEqualTo("https://try-it.example.com");
                assertThat(configuration.getUsePkce()).isTrue();
                assertThat(configuration.getEntrypointsAsServers()).isFalse();
                assertThat(configuration.getContextPathAsServerPath()).isTrue();
            });
    }

    @Test
    void should_map_redoc_configuration_to_dedicated_schema() {
        // Given
        var content = new OpenApiPageContent(
            PortalPageContentId.of("12345678-1234-1234-1234-123456789abc"),
            "ORG",
            "ENV",
            OpenApi.of("openapi: 3.0.0"),
            new RedocConfiguration("https://redoc.example.com")
        );

        // When
        var result = mapper.map(content);

        // Then
        assertThat(result.getConfiguration()).isNotNull();
        assertThat(result.getConfiguration().getActualInstance()).isInstanceOfSatisfying(
            PortalPageRedocConfiguration.class,
            configuration -> {
                assertThat(configuration.getViewer()).isEqualTo(BasePortalPageOpenApiConfiguration.ViewerEnum.REDOC);
                assertThat(configuration.getTryItURL()).isEqualTo("https://redoc.example.com");
            }
        );
    }

    @Test
    void should_map_update_portal_page_content_configuration_to_core_configuration() {
        // Given
        var content = new UpdatePortalPageContent()
            .content("openapi: 3.0.0")
            .configuration(
                new PortalPageOpenApiConfiguration(
                    newSwaggerConfiguration()
                        .displayOperationId(true)
                        .docExpansion(PortalPageSwaggerConfiguration.DocExpansionEnum.LIST)
                        .enableFiltering(false)
                        .maxDisplayedTags(12)
                        .showCommonExtensions(true)
                        .showExtensions(false)
                        .showURL(true)
                        .tryIt(false)
                        .disableSyntaxHighlight(true)
                        .tryItAnonymous(false)
                        .tryItURL("https://try-it.example.com")
                        .usePkce(true)
                        .entrypointsAsServers(false)
                        .contextPathAsServerPath(true)
                )
            );

        // When
        var result = mapper.map(content);

        // Then
        assertThat(result.getConfiguration()).isInstanceOfSatisfying(SwaggerUiConfiguration.class, configuration -> {
            assertThat(configuration.displayOperationId()).isTrue();
            assertThat(configuration.docExpansion()).isEqualTo("list");
            assertThat(configuration.enableFiltering()).isFalse();
            assertThat(configuration.maxDisplayedTags()).isEqualTo(12);
            assertThat(configuration.showCommonExtensions()).isTrue();
            assertThat(configuration.showExtensions()).isFalse();
            assertThat(configuration.showUrl()).isTrue();
            assertThat(configuration.tryIt()).isFalse();
            assertThat(configuration.disableSyntaxHighlight()).isTrue();
            assertThat(configuration.tryItAnonymous()).isFalse();
            assertThat(configuration.tryItUrl()).isEqualTo("https://try-it.example.com");
            assertThat(configuration.usePkce()).isTrue();
            assertThat(configuration.entrypointsAsServers()).isFalse();
            assertThat(configuration.contextPathAsServerPath()).isTrue();
        });
    }

    @Test
    void should_map_update_portal_page_content_swagger_configuration_with_default_values() {
        // Given
        var content = new UpdatePortalPageContent()
            .content("openapi: 3.0.0")
            .configuration(new PortalPageOpenApiConfiguration(newSwaggerConfiguration()));

        // When
        var result = mapper.map(content);

        // Then
        assertThat(result.getConfiguration()).isInstanceOfSatisfying(SwaggerUiConfiguration.class, configuration -> {
            assertThat(configuration.displayOperationId()).isFalse();
            assertThat(configuration.docExpansion()).isEqualTo("none");
            assertThat(configuration.enableFiltering()).isFalse();
            assertThat(configuration.maxDisplayedTags()).isEqualTo(-1);
            assertThat(configuration.showCommonExtensions()).isFalse();
            assertThat(configuration.showExtensions()).isFalse();
            assertThat(configuration.showUrl()).isFalse();
            assertThat(configuration.tryIt()).isFalse();
            assertThat(configuration.disableSyntaxHighlight()).isFalse();
            assertThat(configuration.tryItAnonymous()).isFalse();
            assertThat(configuration.tryItUrl()).isEmpty();
            assertThat(configuration.usePkce()).isFalse();
            assertThat(configuration.entrypointsAsServers()).isFalse();
            assertThat(configuration.contextPathAsServerPath()).isFalse();
        });
    }

    @Test
    void should_map_update_portal_page_content_redoc_configuration_to_core_configuration() {
        // Given
        var content = new UpdatePortalPageContent()
            .content("openapi: 3.0.0")
            .configuration(new PortalPageOpenApiConfiguration(newRedocConfiguration()));

        // When
        var result = mapper.map(content);

        // Then
        assertThat(result.getConfiguration()).isInstanceOfSatisfying(RedocConfiguration.class, configuration ->
            assertThat(configuration.tryItUrl()).isEqualTo("https://redoc.example.com")
        );
    }

    private static PortalPageSwaggerConfiguration newSwaggerConfiguration() {
        var configuration = new PortalPageSwaggerConfiguration();
        configuration.setViewer(BasePortalPageOpenApiConfiguration.ViewerEnum.SWAGGER);
        return configuration;
    }

    private static PortalPageRedocConfiguration newRedocConfiguration() {
        var configuration = new PortalPageRedocConfiguration();
        configuration.setViewer(BasePortalPageOpenApiConfiguration.ViewerEnum.REDOC);
        configuration.setTryItURL("https://redoc.example.com");
        return configuration;
    }
}
