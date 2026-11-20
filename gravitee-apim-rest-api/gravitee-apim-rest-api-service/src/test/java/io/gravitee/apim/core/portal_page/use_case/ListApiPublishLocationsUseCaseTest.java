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
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApiProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.ListApiPublishLocationsUseCase.PublishLocation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListApiPublishLocationsUseCaseTest {

    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory();
    private final NavigationPortalDomainServiceInMemory navigationPortals = new NavigationPortalDomainServiceInMemory();
    private final ListApiPublishLocationsUseCase useCase = new ListApiPublishLocationsUseCase(
        new ApiOwnedNavigationDomainService(queryService, new PortalNavigationItemsCrudServiceInMemory()),
        navigationPortals
    );

    @BeforeEach
    void setUp() {
        navigationPortals.givenDefaultPortalForNavigationFixtures();
    }

    @Test
    void should_return_id_and_name_of_each_top_level_section_in_display_order() {
        var guides = aFolder("Guides").toBuilder().order(1).build();
        var apis = aFolder("APIs").toBuilder().order(0).build();
        queryService.initWith(List.of(guides, apis));

        assertThat(locations()).containsExactly(new PublishLocation(apis.getId(), "APIs"), new PublishLocation(guides.getId(), "Guides"));
    }

    @Test
    void should_return_empty_when_no_top_level_sections_exist() {
        assertThat(locations()).isEmpty();
    }

    @Test
    void should_not_return_nested_folders() {
        var section = aFolder("APIs");
        var nested = aFolder("Payments", section.getId());
        queryService.initWith(List.of(section, nested));

        assertThat(locations()).extracting(PublishLocation::id).containsExactly(section.getId());
    }

    @Test
    void should_not_return_top_level_items_that_are_not_sections() {
        var page = aPage("Home", null);
        var link = aLink(PortalNavigationItemId.random().json(), "Status", null);
        var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
        queryService.initWith(List.of(page, link, product));

        assertThat(locations()).isEmpty();
    }

    @Test
    void should_not_return_sections_outside_the_main_navigation() {
        var homepageFolder = aFolder("Homepage blocks").toBuilder().area(PortalArea.HOMEPAGE).build();
        queryService.initWith(List.of(homepageFolder));

        assertThat(locations()).isEmpty();
    }

    @Test
    void should_not_return_folders_owned_by_an_api() {
        var apiFolder = aFolder("Auth").toBuilder().reference(new NavigationItemReference.ApiReference("api-a")).build();
        queryService.initWith(List.of(apiFolder));

        assertThat(locations()).isEmpty();
    }

    @Test
    void should_not_return_unpublished_sections() {
        var draft = aFolder("Draft").toBuilder().published(false).build();
        queryService.initWith(List.of(draft));

        assertThat(locations()).isEmpty();
    }

    @Test
    void should_return_only_the_sections_of_the_named_portal() {
        var otherPortalId = navigationPortals.givenAutomationPortal(ORG_ID, ENV_ID);
        var defaultPortalSection = aFolder("APIs");
        var otherPortalSection = aFolder("Partners")
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(otherPortalId))
            .build();
        queryService.initWith(List.of(defaultPortalSection, otherPortalSection));

        assertThat(locationsOf(otherPortalId)).containsExactly(new PublishLocation(otherPortalSection.getId(), "Partners"));
    }

    @Test
    void should_return_only_the_default_portal_sections_when_no_portal_is_named_and_the_environment_has_several() {
        var otherPortalId = navigationPortals.givenAutomationPortal(ORG_ID, ENV_ID);
        var defaultPortalSection = aFolder("APIs");
        var otherPortalSection = aFolder("Partners")
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(otherPortalId))
            .build();
        queryService.initWith(List.of(defaultPortalSection, otherPortalSection));

        assertThat(locations()).containsExactly(new PublishLocation(defaultPortalSection.getId(), "APIs"));
    }

    @Test
    void should_return_empty_without_creating_a_portal_when_the_environment_has_no_default_portal() {
        var noPortal = new NavigationPortalDomainServiceInMemory();
        var useCaseWithoutPortal = new ListApiPublishLocationsUseCase(
            new ApiOwnedNavigationDomainService(queryService, new PortalNavigationItemsCrudServiceInMemory()),
            noPortal
        );
        queryService.initWith(List.of(aFolder("APIs")));

        assertThat(useCaseWithoutPortal.execute(new ListApiPublishLocationsUseCase.Input(ENV_ID, null)).locations()).isEmpty();
        assertThat(noPortal.portalCrudService().findByEnvironmentId(ENV_ID)).isEmpty();
    }

    @Test
    void should_return_the_sections_of_the_only_portal_when_it_is_not_the_default_one() {
        var automationOnly = new NavigationPortalDomainServiceInMemory();
        var automationPortalId = automationOnly.givenAutomationPortal(ORG_ID, ENV_ID);
        var useCaseOfAutomationOnly = new ListApiPublishLocationsUseCase(
            new ApiOwnedNavigationDomainService(queryService, new PortalNavigationItemsCrudServiceInMemory()),
            automationOnly
        );
        var section = aFolder("APIs").toBuilder().reference(new NavigationItemReference.PortalReference(automationPortalId)).build();
        queryService.initWith(List.of(section));

        assertThat(useCaseOfAutomationOnly.execute(new ListApiPublishLocationsUseCase.Input(ENV_ID, null)).locations())
            .extracting(PublishLocation::id)
            .containsExactly(section.getId());
    }

    @Test
    void should_reject_an_unknown_portal() {
        assertThatThrownBy(() -> locationsOf(PortalId.random())).isInstanceOf(PortalNotFoundException.class);
    }

    private List<PublishLocation> locations() {
        return locationsOf(null);
    }

    private List<PublishLocation> locationsOf(PortalId portalId) {
        return useCase.execute(new ListApiPublishLocationsUseCase.Input(ENV_ID, portalId)).locations();
    }
}
