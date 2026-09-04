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

import static fixtures.core.model.PortalNavigationItemFixtures.API1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.API_PRODUCT_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.definition.model.v4.ApiType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SeedDefaultPagesForPortalNavigationItemsUseCaseTest {

    private SeedDefaultPagesForPortalNavigationItemsUseCase useCase;
    private ApiCrudServiceInMemory apiCrudService;
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemsCrudService;
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;
    private PortalPageContentCrudServiceInMemory portalPageContentCrudService;

    @BeforeEach
    void setUp() {
        var storage = new ArrayList<PortalNavigationItem>();
        portalNavigationItemsCrudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        portalNavigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        portalPageContentCrudService = new PortalPageContentCrudServiceInMemory();

        apiCrudService = new ApiCrudServiceInMemory();
        apiCrudService.initWith(List.of(Api.builder().id("api-1").name("API 1").version("1.0.0").type(ApiType.PROXY).build()));

        portalNavigationItemsQueryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());

        useCase = new SeedDefaultPagesForPortalNavigationItemsUseCase(
            new PortalNavigationDefaultPageDomainService(
                portalNavigationItemsQueryService,
                new PortalNavigationItemDomainService(
                    portalNavigationItemsCrudService,
                    portalNavigationItemsQueryService,
                    portalPageContentCrudService,
                    PortalPageContentQueryServiceInMemory.sharing(portalPageContentCrudService.storage()),
                    apiCrudService,
                    new PortalNavigationItemSourceDomainServiceInMemory()
                ),
                portalPageContentCrudService,
                apiCrudService
            )
        );
    }

    @Test
    void should_seed_default_overview_page_for_api() {
        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID)))
            .filteredOn(PortalNavigationPage.class::isInstance)
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getTitle()).isEqualTo("Overview");
                assertThat(item.getPublished()).isFalse();
            });
        assertThat(portalPageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("api-overview-page-content.md"))
            );
    }

    /** An agent is an A2A proxy: it gets an overview page like an API, worded for an agent consumer. */
    @Test
    void should_seed_agent_overview_page_for_agent() {
        var parent = (PortalNavigationFolder) portalNavigationItemsQueryService.findByIdAndEnvironmentId(
            ENV_ID,
            PortalNavigationItemId.of(APIS_ID)
        );
        var agentApiId = "00000000-0000-0000-0000-0000000000a1";
        apiCrudService.initWith(List.of(Api.builder().id(agentApiId).name("My agent").version("1.0.0").type(ApiType.A2A_PROXY).build()));
        var agent = PortalNavigationItemFixtures.anAgent("00000000-0000-0000-0000-0000000000a2", "My agent", parent.getId(), agentApiId);
        agent.updateParent(parent);
        portalNavigationItemsQueryService.initWith(
            Stream.concat(portalNavigationItemsQueryService.storage().stream(), Stream.of(agent)).toList()
        );

        var output = useCase.execute(new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(agent.getId())));

        assertThat(output.seededNavigationItemIds()).containsExactly(agent.getId());
        assertThat(portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(ENV_ID, agent.getId()))
            .filteredOn(PortalNavigationPage.class::isInstance)
            .singleElement()
            .satisfies(item -> assertThat(item.getTitle()).isEqualTo("Overview"));
        assertThat(portalPageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("agent-overview-page-content.md"))
            );
    }

    @Test
    void should_seed_default_overview_page_before_generated_apis_for_api_product() {
        var parent = (PortalNavigationFolder) portalNavigationItemsQueryService.findByIdAndEnvironmentId(
            ENV_ID,
            PortalNavigationItemId.of(APIS_ID)
        );
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            API_PRODUCT_ID,
            "My API Product",
            parent.getId(),
            "00000000-0000-0000-0000-000000000019"
        )
            .toBuilder()
            .visibility(PortalVisibility.PRIVATE)
            .build();
        apiProduct.updateParent(parent);
        var generatedApi = PortalNavigationItemFixtures.anApi(
            "00000000-0000-0000-0000-000000000020",
            "Generated API",
            apiProduct.getId(),
            "api-1"
        );
        generatedApi.setVisibility(PortalVisibility.PRIVATE);
        generatedApi.setOrder(0);
        generatedApi.updateParent(apiProduct);
        portalNavigationItemsCrudService.create(apiProduct);
        portalNavigationItemsCrudService.create(generatedApi);

        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(apiProduct.getId()))
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(apiProduct.getId());
        assertThat(
            portalNavigationItemsQueryService
                .findByParentIdAndEnvironmentId(ENV_ID, apiProduct.getId())
                .stream()
                .sorted(Comparator.comparing(PortalNavigationItem::getOrder))
                .toList()
        )
            .extracting(
                PortalNavigationItem::getTitle,
                PortalNavigationItem::getOrder,
                PortalNavigationItem::getPublished,
                PortalNavigationItem::getVisibility
            )
            .containsExactly(
                tuple("Overview", 0, false, PortalVisibility.PRIVATE),
                tuple("Generated API", 1, true, PortalVisibility.PRIVATE)
            );
        assertThat(portalPageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("api-product-overview-page-content.md"))
            );
    }

    @Test
    void should_skip_seeding_when_api_navigation_item_already_has_a_child_page() {
        var apiNavigationItem = (PortalNavigationApi) portalNavigationItemsQueryService.findByIdAndEnvironmentId(
            ENV_ID,
            PortalNavigationItemId.of(API1_ID)
        );
        var existingPage = PortalNavigationItemFixtures.aPage(
            "00000000-0000-0000-0000-000000000111",
            "Existing page",
            apiNavigationItem.getId(),
            PortalPageContentId.random()
        );
        existingPage.updateParent(apiNavigationItem);
        portalNavigationItemsCrudService.create(existingPage);

        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
        );

        assertThat(output.seededNavigationItemIds()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_skip_seeding_when_api_product_already_has_a_child_page() {
        var parent = (PortalNavigationFolder) portalNavigationItemsQueryService.findByIdAndEnvironmentId(
            ENV_ID,
            PortalNavigationItemId.of(APIS_ID)
        );
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            API_PRODUCT_ID,
            "My API Product",
            parent.getId(),
            "00000000-0000-0000-0000-000000000019"
        );
        apiProduct.updateParent(parent);
        var existingPage = PortalNavigationItemFixtures.aPage(
            "00000000-0000-0000-0000-000000000020",
            "Existing page",
            apiProduct.getId(),
            PortalPageContentId.random()
        );
        existingPage.updateParent(apiProduct);
        portalNavigationItemsCrudService.create(apiProduct);
        portalNavigationItemsCrudService.create(existingPage);

        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(apiProduct.getId()))
        );

        assertThat(output.seededNavigationItemIds()).isEmpty();
        assertThat(portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(ENV_ID, apiProduct.getId())).containsExactly(
            existingPage
        );
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_seed_mcp_proxy_overview_page_for_mcp_proxy_api() {
        apiCrudService.initWith(List.of(Api.builder().id("api-1").name("MCP API").version("1.0.0").type(ApiType.MCP_PROXY).build()));

        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalPageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("api-overview-mcp-proxy-page-content.md"))
            );
    }

    @Test
    void should_skip_navigation_items_that_do_not_support_default_pages() {
        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(
                ORG_ID,
                ENV_ID,
                List.of(PortalNavigationItemId.of(APIS_ID), PortalNavigationItemId.of(API1_ID))
            )
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_load_templates_regardless_of_thread_context_classloader() {
        // Given: a context classloader that cannot see this module's classpath resources at all,
        // reproducing the production failure mode (a Vert.x/plugin-owned thread whose context
        // classloader isn't the one that loaded gravitee-apim-rest-api-service).
        final var originalClassLoader = Thread.currentThread().getContextClassLoader();
        final var isolatedClassLoader = new URLClassLoader(new URL[0], null);
        Thread.currentThread().setContextClassLoader(isolatedClassLoader);

        try {
            // When
            var output = useCase.execute(
                new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
            );

            // Then
            assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
            assertThat(portalPageContentCrudService.storage()).hasSize(1);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private String loadTemplate(String templatePath) {
        try (
            var inputStream = SeedDefaultPagesForPortalNavigationItemsUseCaseTest.class.getClassLoader().getResourceAsStream(
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
