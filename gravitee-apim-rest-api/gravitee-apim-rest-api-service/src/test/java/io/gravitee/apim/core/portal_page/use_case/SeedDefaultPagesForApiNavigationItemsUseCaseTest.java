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
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.definition.model.v4.ApiType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SeedDefaultPagesForApiNavigationItemsUseCaseTest {

    private SeedDefaultPagesForApiNavigationItemsUseCase useCase;
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemsCrudService;
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;
    private PortalPageContentCrudServiceInMemory portalPageContentCrudService;

    @BeforeEach
    void setUp() {
        var storage = new ArrayList<PortalNavigationItem>();
        portalNavigationItemsCrudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        portalNavigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        portalPageContentCrudService = new PortalPageContentCrudServiceInMemory();

        var apiCrudService = new ApiCrudServiceInMemory();
        apiCrudService.initWith(List.of(Api.builder().id("api-1").name("API 1").version("1.0.0").type(ApiType.PROXY).build()));

        portalNavigationItemsQueryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());

        useCase = new SeedDefaultPagesForApiNavigationItemsUseCase(
            portalNavigationItemsQueryService,
            new PortalNavigationItemDomainService(
                portalNavigationItemsCrudService,
                portalNavigationItemsQueryService,
                portalPageContentCrudService,
                apiCrudService
            ),
            portalPageContentCrudService
        );
    }

    @Test
    void should_seed_default_overview_page_for_api() {
        var output = useCase.execute(
            new SeedDefaultPagesForApiNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
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
            new SeedDefaultPagesForApiNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
        );

        assertThat(output.seededNavigationItemIds()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_skip_navigation_items_that_are_not_api_type() {
        var output = useCase.execute(
            new SeedDefaultPagesForApiNavigationItemsUseCase.Input(
                ORG_ID,
                ENV_ID,
                List.of(PortalNavigationItemId.of(APIS_ID), PortalNavigationItemId.of(API1_ID))
            )
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    private String loadTemplate(String templatePath) {
        try (
            var inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(String.format("templates/%s", templatePath))
        ) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load template " + templatePath, e);
        }
    }
}
