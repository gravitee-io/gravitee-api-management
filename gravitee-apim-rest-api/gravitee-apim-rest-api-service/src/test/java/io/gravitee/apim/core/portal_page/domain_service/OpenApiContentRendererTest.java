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

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiExposedEntrypointDomainServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api.model.ExposedEntrypoint;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenApiContentRendererTest {

    private static final String ORGANIZATION_ID = PortalPageContentFixtures.ORGANIZATION_ID;
    private static final String ENVIRONMENT_ID = ENV_ID;

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private ApiCrudServiceInMemory apiCrudService;
    private ApiExposedEntrypointDomainServiceInMemory apiExposedEntrypointDomainService;
    private OpenApiContentTransformerSpy openApiContentTransformer;
    private OpenApiContentRenderer renderer;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        apiCrudService = new ApiCrudServiceInMemory();
        apiExposedEntrypointDomainService = new ApiExposedEntrypointDomainServiceInMemory();
        openApiContentTransformer = new OpenApiContentTransformerSpy();
        renderer = new OpenApiContentRenderer(
            new PortalNavigationEnclosingApiDomainService(navigationItemsQueryService),
            apiCrudService,
            apiExposedEntrypointDomainService,
            openApiContentTransformer
        );
    }

    @Test
    void should_render_openapi_page_content_with_viewer_configuration_when_server_resolution_is_disabled() {
        var contentId = PortalPageContentId.random();
        var pageId = "00000000-0000-0000-0000-000000000096";
        var content = "openapi: 3.0.3\ninfo:\n  title: Test";
        var configuration = configuration(false, false);
        var page = PortalNavigationItemFixtures.aPage(pageId, "OpenAPI page", null, contentId);
        page.markAsRoot();
        var openApiContent = PortalPageContentFixtures.anOpenApiPageContent(
            contentId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            content,
            configuration
        );

        var rendered = renderer.render(page, openApiContent);

        assertThat(rendered.value()).isEqualTo(content);
        assertThat(rendered.type()).isEqualTo(PortalPageContentType.OPENAPI);
        assertThat(rendered.configuration()).isEqualTo(configuration);
        assertThat(openApiContentTransformer.calls).isZero();
    }

    @Test
    void should_transform_openapi_page_content_with_api_context_when_server_resolution_is_enabled() {
        var apiTechnicalId = "api-technical-id";
        var contentId = PortalPageContentId.random();
        var pageId = "00000000-0000-0000-0000-000000000095";
        var content = "openapi: 3.0.3\ninfo:\n  title: Test";
        var configuration = configuration(false, true);
        var apiNav = PortalNavigationItemFixtures.anApi(PortalNavigationItemFixtures.API1_ID, "API One", null, apiTechnicalId);
        apiNav.markAsRoot();
        var pageUnderApi = PortalNavigationItemFixtures.aPage(pageId, "OpenAPI page", apiNav.getId(), contentId);
        pageUnderApi.updateParent(apiNav);
        var openApiContent = PortalPageContentFixtures.anOpenApiPageContent(
            contentId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            content,
            configuration
        );
        var api = ApiFixtures.aProxyApiV4().toBuilder().id(apiTechnicalId).environmentId(ENVIRONMENT_ID).build();
        apiCrudService.initWith(List.of(api));
        apiExposedEntrypointDomainService.initWith(List.of(new ExposedEntrypoint("https://apis.gravitee.io/http_proxy")));
        navigationItemsQueryService.initWith(List.of(apiNav, pageUnderApi));
        openApiContentTransformer.transformedContent = "transformed openapi";

        var rendered = renderer.render(pageUnderApi, openApiContent);

        assertThat(rendered.value()).isEqualTo("transformed openapi");
        assertThat(openApiContentTransformer.calls).isOne();
        assertThat(openApiContentTransformer.content).isEqualTo(content);
        assertThat(openApiContentTransformer.configuration).isEqualTo(configuration);
        assertThat(openApiContentTransformer.apiContext).isPresent();
        assertThat(openApiContentTransformer.apiContext.orElseThrow().entrypoints()).containsExactly("https://apis.gravitee.io/http_proxy");
        assertThat(openApiContentTransformer.apiContext.orElseThrow().contextPath()).contains("/http_proxy/");
    }

    @Test
    void should_transform_openapi_page_content_without_api_context_when_page_has_no_api_ancestor() {
        var contentId = PortalPageContentId.random();
        var pageId = "00000000-0000-0000-0000-000000000094";
        var content = "openapi: 3.0.3\ninfo:\n  title: Test";
        var configuration = configuration(true, true);
        var page = PortalNavigationItemFixtures.aPage(pageId, "OpenAPI page", null, contentId);
        page.markAsRoot();
        var openApiContent = PortalPageContentFixtures.anOpenApiPageContent(
            contentId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            content,
            configuration
        );
        navigationItemsQueryService.initWith(List.of(page));
        openApiContentTransformer.transformedContent = content;

        var rendered = renderer.render(page, openApiContent);

        assertThat(rendered.value()).isEqualTo(content);
        assertThat(openApiContentTransformer.calls).isOne();
        assertThat(openApiContentTransformer.apiContext).isEmpty();
    }

    private static SwaggerUiConfiguration configuration(boolean entrypointsAsServers, boolean contextPathAsServerPath) {
        return new SwaggerUiConfiguration(
            true,
            "list",
            true,
            12,
            true,
            true,
            true,
            true,
            true,
            true,
            "https://sandbox.example.com",
            true,
            entrypointsAsServers,
            contextPathAsServerPath
        );
    }

    private static class OpenApiContentTransformerSpy implements OpenApiContentTransformer {

        private String content;
        private SwaggerUiConfiguration configuration;
        private Optional<ApiContext> apiContext = Optional.empty();
        private String transformedContent = "transformed";
        private int calls;

        @Override
        public String transform(String content, SwaggerUiConfiguration configuration, Optional<ApiContext> apiContext) {
            this.content = content;
            this.configuration = configuration;
            this.apiContext = apiContext;
            this.calls++;
            return transformedContent;
        }
    }
}
