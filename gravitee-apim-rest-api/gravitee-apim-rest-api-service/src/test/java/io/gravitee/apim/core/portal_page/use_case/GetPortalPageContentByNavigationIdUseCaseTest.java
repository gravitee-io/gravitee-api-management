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

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_CONTENT_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiExposedEntrypointDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.service_provider.ApiTemplateModelProvider;
import io.gravitee.apim.core.environment.service_provider.EnvironmentTemplateModelProvider;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal_page.domain_service.DefaultContentRenderer;
import io.gravitee.apim.core.portal_page.domain_service.GraviteeMarkdownContentRenderer;
import io.gravitee.apim.core.portal_page.domain_service.OpenApiContentRenderer;
import io.gravitee.apim.core.portal_page.domain_service.OpenApiContentTransformer;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationEnclosingApiDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.RenderedPageContent;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetPortalPageContentByNavigationIdUseCaseTest {

    private static final String ORGANIZATION_ID = PortalPageContentFixtures.ORGANIZATION_ID;
    private static final String ENVIRONMENT_ID = ENV_ID;
    private static final String UNPUBLISHED_ID = PortalNavigationItemFixtures.UNPUBLISHED_ID;
    private static final String PRIVATE_ID = PortalNavigationItemFixtures.PRIVATE_ID;

    private GetPortalPageContentByNavigationIdUseCase useCase;
    private PortalPageContentQueryServiceInMemory pageContentQueryService;
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private ApiCrudServiceInMemory apiCrudService;
    private ApiExposedEntrypointDomainServiceInMemory apiExposedEntrypointDomainService;
    private OpenApiContentTransformerSpy openApiContentTransformer;

    @Mock
    private PortalNavigationTemplatingService portalNavigationTemplatingService;

    @Mock
    private ApiTemplateModelProvider apiTemplateModelProvider;

    @Mock
    private EnvironmentTemplateModelProvider environmentTemplateModelProvider;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        apiCrudService = new ApiCrudServiceInMemory();
        apiExposedEntrypointDomainService = new ApiExposedEntrypointDomainServiceInMemory();
        openApiContentTransformer = new OpenApiContentTransformerSpy();
        var apiVisibilityDomainService = new PortalNavigationApiVisibilityDomainService(
            navigationItemsQueryService,
            new ApiPortalMembershipDomainService(
                new MembershipQueryServiceInMemory(),
                new SubscriptionQueryServiceInMemory(),
                new ApiQueryServiceInMemory()
            )
        );
        var enclosingApiDomainService = new PortalNavigationEnclosingApiDomainService(navigationItemsQueryService);
        when(portalNavigationTemplatingService.renderGraviteeMarkdown(any())).thenAnswer(invocation -> {
            var in = (PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput) invocation.getArgument(0);
            return RenderedPageContent.of(in.rawMarkdown().value(), PortalPageContentType.GRAVITEE_MARKDOWN);
        });
        when(environmentTemplateModelProvider.getEnvironmentMetadata(any())).thenReturn(Map.of());
        var gmdRenderer = new GraviteeMarkdownContentRenderer(
            enclosingApiDomainService,
            portalNavigationTemplatingService,
            apiTemplateModelProvider,
            environmentTemplateModelProvider
        );
        useCase = new GetPortalPageContentByNavigationIdUseCase(
            navigationItemsQueryService,
            pageContentQueryService,
            apiVisibilityDomainService,
            List.of(
                gmdRenderer,
                new OpenApiContentRenderer(
                    enclosingApiDomainService,
                    apiCrudService,
                    apiExposedEntrypointDomainService,
                    openApiContentTransformer
                ),
                new DefaultContentRenderer()
            )
        );

        clearInvocations(portalNavigationTemplatingService);
        var supportContentId = PortalPageContentId.of(PortalNavigationItemFixtures.SUPPORT_CONTENT_ID);
        var page11ContentId = PortalPageContentId.of(PortalNavigationItemFixtures.PAGE11_CONTENT_ID);

        var supportContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            supportContentId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            "Support page content"
        );

        var page11Content = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            page11ContentId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            "Page 11 content"
        );

        pageContentQueryService.initWith(List.of(supportContent, page11Content));
        navigationItemsQueryService.initWith(PortalNavigationItemFixtures.navigationItemsForContentTest());
    }

    @Test
    void should_return_portal_page_content_when_navigation_page_found() {
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        var output = useCase.execute(input);

        assertThat(output.renderedContent()).isNotNull();
        assertThat(output.renderedContent().type()).isEqualTo(PortalPageContentType.GRAVITEE_MARKDOWN);
        assertThat(output.renderedContent().value()).isEqualTo("Page 11 content");
        assertThat(output.portalNavigationItem()).isNotNull();
        assertThat(output.portalNavigationItem()).isInstanceOf(PortalNavigationPage.class);
        assertThat(output.portalNavigationItem().getId().toString()).isEqualTo(PAGE11_ID);
    }

    @Test
    void should_return_async_api_page_content_when_navigation_page_found() {
        var contentId = PortalPageContentId.random();
        var pageId = "00000000-0000-0000-0000-000000000098";
        var content = "asyncapi: '3.0.0'\ninfo:\n  title: Test";
        var page = PortalNavigationItemFixtures.aPage(pageId, "AsyncAPI page", null, contentId);
        page.markAsRoot();
        var asyncApiContent = PortalPageContentFixtures.anAsyncApiPageContent(contentId, ORGANIZATION_ID, ENVIRONMENT_ID, content);

        pageContentQueryService.initWith(List.of(asyncApiContent));
        navigationItemsQueryService.initWith(List.of(page));

        var output = useCase.execute(
            new GetPortalPageContentByNavigationIdUseCase.Input(
                pageId,
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                PortalNavigationItemViewerContext.forConsole()
            )
        );

        assertThat(output.renderedContent()).isNotNull();
        assertThat(output.renderedContent().type()).isEqualTo(PortalPageContentType.ASYNCAPI);
        assertThat(output.renderedContent().value()).isEqualTo(content);
    }

    @Test
    void should_return_openapi_page_content_with_viewer_configuration_when_navigation_page_found() {
        var contentId = PortalPageContentId.random();
        var pageId = "00000000-0000-0000-0000-000000000096";
        var content = "openapi: 3.0.3\ninfo:\n  title: Test";
        var configuration = new SwaggerUiConfiguration(
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
            false,
            false
        );
        var page = PortalNavigationItemFixtures.aPage(pageId, "OpenAPI page", null, contentId);
        page.markAsRoot();
        var openApiContent = PortalPageContentFixtures.anOpenApiPageContent(
            contentId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            content,
            configuration
        );

        pageContentQueryService.initWith(List.of(openApiContent));
        navigationItemsQueryService.initWith(List.of(page));

        var output = useCase.execute(
            new GetPortalPageContentByNavigationIdUseCase.Input(
                pageId,
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                PortalNavigationItemViewerContext.forConsole()
            )
        );

        assertThat(output.renderedContent()).isNotNull();
        assertThat(output.renderedContent().type()).isEqualTo(PortalPageContentType.OPENAPI);
        assertThat(output.renderedContent().value()).isEqualTo(content);
        assertThat(output.renderedContent().configuration()).isEqualTo(configuration);
        assertThat(openApiContentTransformer.calls).isZero();
    }

    @Test
    void should_apply_portal_navigation_templating_to_gravitee_markdown() {
        doReturn(RenderedPageContent.of("templated", PortalPageContentType.GRAVITEE_MARKDOWN))
            .when(portalNavigationTemplatingService)
            .renderGraviteeMarkdown(any());

        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        var output = useCase.execute(input);

        assertThat(output.renderedContent().value()).isEqualTo("templated");
        assertThat(
            ((GraviteeMarkdownPageContent) pageContentQueryService
                    .findById(PortalPageContentId.of(PAGE11_CONTENT_ID))
                    .orElseThrow()).getContent().value()
        ).isEqualTo("Page 11 content");
        var captor = ArgumentCaptor.forClass(PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput.class);
        verify(portalNavigationTemplatingService).renderGraviteeMarkdown(captor.capture());
        assertThat(captor.getValue().rawMarkdown().value()).isEqualTo("Page 11 content");
        assertThat(captor.getValue().model()).containsKey("metadata");
    }

    @Test
    void should_pass_environment_metadata_when_page_has_no_api_ancestor() {
        var envMetadata = Map.of("supportEmail", "support@example.com");
        when(environmentTemplateModelProvider.getEnvironmentMetadata(eq(ENVIRONMENT_ID))).thenReturn(envMetadata);

        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        useCase.execute(input);

        var captor = ArgumentCaptor.forClass(PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput.class);
        verify(portalNavigationTemplatingService).renderGraviteeMarkdown(captor.capture());
        assertThat(captor.getValue().model()).containsEntry("metadata", envMetadata);
    }

    @Test
    void should_throw_when_template_rendering_fails() {
        doThrow(new PortalPageContentTemplateException("Invalid expression or value is missing"))
            .when(portalNavigationTemplatingService)
            .renderGraviteeMarkdown(any());

        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(PortalPageContentTemplateException.class);
    }

    @Test
    void should_pass_api_model_when_page_is_under_api_branch() {
        var apiNav = PortalNavigationItemFixtures.anApi(PortalNavigationItemFixtures.API1_ID, "API One", null, "api-technical-id");
        apiNav.markAsRoot();
        var folderUnderApi = PortalNavigationItemFixtures.aFolder(PortalNavigationItemFixtures.API1_FOLDER_ID, "Docs", apiNav.getId());
        folderUnderApi.updateParent(apiNav);
        var contentId = PortalPageContentId.random();
        var pageId = "00000000-0000-0000-0000-000000000097";
        var pageUnderFolder = PortalNavigationItemFixtures.aPage(pageId, "API doc page", folderUnderApi.getId(), contentId);
        pageUnderFolder.updateParent(folderUnderApi);

        var md = PortalPageContentFixtures.aGraviteeMarkdownPageContent(contentId, ORGANIZATION_ID, ENVIRONMENT_ID, "Hello");
        pageContentQueryService.initWith(List.of(md));
        navigationItemsQueryService.initWith(List.of(apiNav, folderUnderApi, pageUnderFolder));

        var fakeApiModel = new Object();
        when(apiTemplateModelProvider.getApiTemplateModel(eq(ORGANIZATION_ID), eq(ENVIRONMENT_ID), eq("api-technical-id"))).thenReturn(
            fakeApiModel
        );

        useCase.execute(
            new GetPortalPageContentByNavigationIdUseCase.Input(
                pageId,
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                PortalNavigationItemViewerContext.forConsole()
            )
        );

        var captor = ArgumentCaptor.forClass(PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput.class);
        verify(portalNavigationTemplatingService).renderGraviteeMarkdown(captor.capture());
        assertThat(captor.getValue().model()).containsEntry("api", fakeApiModel);
    }

    @Test
    void should_throw_when_navigation_item_not_found() {
        var unknownId = "00000000-0000-0000-0000-000000000099";
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            unknownId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_navigation_item_is_not_a_page() {
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PortalNavigationItemFixtures.APIS_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("Navigation item type cannot be changed or is mismatched");
    }

    @Test
    void should_throw_when_page_content_not_found() {
        pageContentQueryService.initWith(List.of());

        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
    }

    @Test
    void should_throw_when_navigation_item_exists_in_different_environment() {
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ORGANIZATION_ID,
            "different-env",
            PortalNavigationItemViewerContext.forConsole()
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_navigation_item_not_visible_in_portal() {
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            UNPUBLISHED_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forPortal(true)
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_navigation_item_private_and_user_not_authenticated() {
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PRIVATE_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forPortal(false)
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
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
