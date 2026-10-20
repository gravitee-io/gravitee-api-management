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
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aLink;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemCreationExpansionDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.definition.model.v4.ApiType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateApiOverviewPageUseCaseTest {

    private static final String API_ID = "api-a";

    private final List<PortalNavigationItem> storage = new ArrayList<>();
    private final PortalNavigationItemsCrudServiceInMemory crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
    private final PortalPageContentCrudServiceInMemory pageContentCrudService = new PortalPageContentCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private CreateApiOverviewPageUseCase useCase;

    @BeforeEach
    void setUp() {
        var apiProductQueryService = new ApiProductQueryServiceInMemory();
        var sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();
        var pageContentQueryService = PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage());
        var apiOwnedNavigationDomainService = new ApiOwnedNavigationDomainService(queryService, crudService);
        useCase = new CreateApiOverviewPageUseCase(
            new PortalNavigationDefaultPageDomainService(
                queryService,
                new PortalNavigationItemDomainService(
                    crudService,
                    queryService,
                    pageContentCrudService,
                    pageContentQueryService,
                    apiCrudService,
                    sourceDomainService,
                    apiOwnedNavigationDomainService
                ),
                pageContentCrudService,
                apiCrudService,
                new PortalNavigationItemCreationExpansionDomainService(apiProductQueryService, apiCrudService, queryService),
                new PortalNavigationItemValidatorService(
                    queryService,
                    pageContentQueryService,
                    apiProductQueryService,
                    sourceDomainService,
                    apiCrudService
                ),
                apiOwnedNavigationDomainService
            )
        );
        givenAnApiOfType(ApiType.PROXY);
    }

    @Test
    void should_create_an_unpublished_public_overview_page_at_the_top_of_the_api_documentation() {
        var output = createOverviewPage();

        assertThat(storage)
            .singleElement()
            .isInstanceOfSatisfying(PortalNavigationPage.class, page -> {
                assertThat(page.getId()).isEqualTo(output.page().getId());
                assertThat(page.getTitle()).isEqualTo("Overview");
                assertThat(page.getReference()).isEqualTo(ownedBy(API_ID));
                assertThat(page.getParentId()).isNull();
                assertThat(page.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
                assertThat(page.getPublished()).isFalse();
                assertThat(page.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
                assertThat(page.getOrder()).isZero();
                assertThat(page.getPortalPageContentId()).isEqualTo(pageContentCrudService.storage().getFirst().getId());
            });
    }

    @Test
    void should_fill_it_with_the_overview_template_left_for_the_portal_to_fill_in() {
        createOverviewPage();

        assertThat(pageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("api-overview-page-content.md"))
            );
    }

    @Test
    void should_fill_it_with_the_mcp_proxy_overview_template_for_an_mcp_proxy_api() {
        givenAnApiOfType(ApiType.MCP_PROXY);

        createOverviewPage();

        assertThat(pageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("api-overview-mcp-proxy-page-content.md"))
            );
    }

    @Test
    void should_create_it_first_when_the_api_has_only_folders_and_links() {
        var folder = aFolder("Guides").toBuilder().reference(ownedBy(API_ID)).order(0).build();
        var link = aLink(PortalNavigationItemId.random().json(), "Status", null).toBuilder().reference(ownedBy(API_ID)).order(1).build();
        queryService.initWith(List.of(folder, link));

        var output = createOverviewPage();

        assertThat(stored(output.page().getId()).getOrder()).isZero();
        assertThat(stored(folder.getId()).getOrder()).isEqualTo(1);
        assertThat(stored(link.getId()).getOrder()).isEqualTo(2);
        assertThat(storage).hasSize(3);
    }

    private CreateApiOverviewPageUseCase.Output createOverviewPage() {
        return useCase.execute(new CreateApiOverviewPageUseCase.Input(ORG_ID, ENV_ID, API_ID));
    }

    private void givenAnApiOfType(ApiType type) {
        apiCrudService.initWith(List.of(Api.builder().id(API_ID).environmentId(ENV_ID).name("Api A").version("1.0").type(type).build()));
    }

    private PortalNavigationItem stored(PortalNavigationItemId id) {
        return queryService.findByIdAndEnvironmentId(ENV_ID, id);
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }

    private static String loadTemplate(String templatePath) {
        try (
            var inputStream = CreateApiOverviewPageUseCaseTest.class.getClassLoader().getResourceAsStream(
                String.format("templates/%s", templatePath)
            )
        ) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load template " + templatePath, e);
        }
    }
}
