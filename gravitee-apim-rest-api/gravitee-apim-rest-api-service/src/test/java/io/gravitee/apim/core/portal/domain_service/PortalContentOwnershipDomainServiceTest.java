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
package io.gravitee.apim.core.portal.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.portal.domain_service.PortalContentOwnershipDomainService.Moved;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalContentOwnershipDomainServiceTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final PortalId DEFAULT_PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalId AUTOMATION_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");
    private static final PortalId DELETED_PORTAL_ID = PortalId.of("99999999-9999-9999-9999-999999999999");
    private static final NavigationItemReference ON_ZERO_UUID = NavigationItemReference.defaultReference();
    private static final URL PORTAL_URL = url("https://portal.acme.io/next");

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navigationItemCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalListingCrudServiceInMemory listingCrudService = new PortalListingCrudServiceInMemory();
    private final PortalContentOwnershipDomainService cut = new PortalContentOwnershipDomainService(
        portalCrudService,
        navigationItemCrudService,
        listingCrudService
    );

    @SneakyThrows
    private static URL url(String value) {
        return URI.create(value).toURL();
    }

    private static NavigationItemReference onPortal(PortalId portalId) {
        return new NavigationItemReference.PortalReference(portalId);
    }

    private static Portal aPortal(PortalId id, boolean defaultPortal) {
        return Portal.of(id, ENV_ID, ORG_ID, "Portal " + id, PortalNavigationStructure.empty(), null, defaultPortal, PORTAL_URL);
    }

    private PortalNavigationFolder storedFolder(String environmentId, NavigationItemReference reference) {
        var folder = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(environmentId)
            .reference(reference)
            .title("Docs")
            .segment("docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        navigationItemCrudService.create(folder);
        return folder;
    }

    private void storedListing(String environmentId, PortalId portalId) {
        listingCrudService.create(PortalListing.of(PortalListingId.random(), environmentId, ORG_ID, portalId, List.of()));
    }

    private List<PortalId> listingPortals(String environmentId) {
        return listingCrudService
            .storage()
            .stream()
            .filter(listing -> environmentId.equals(listing.getEnvironmentId()))
            .map(PortalListing::getPortalId)
            .toList();
    }

    @Test
    void should_move_zero_uuid_items_and_orphan_listings_to_the_default_portal() {
        portalCrudService.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, true)));
        var topNavbarItem = storedFolder(ENV_ID, ON_ZERO_UUID);
        var homepage = storedFolder(ENV_ID, ON_ZERO_UUID);
        homepage.setArea(PortalArea.HOMEPAGE);
        storedListing(ENV_ID, PortalId.ZERO);
        storedListing(ENV_ID, DELETED_PORTAL_ID);

        var moved = cut.moveOrphans(ENV_ID);

        assertThat(moved).contains(new Moved(2, 2));
        assertThat(topNavbarItem.getReference()).isEqualTo(onPortal(DEFAULT_PORTAL_ID));
        assertThat(homepage.getReference()).isEqualTo(onPortal(DEFAULT_PORTAL_ID));
        assertThat(listingPortals(ENV_ID)).containsOnly(DEFAULT_PORTAL_ID);
    }

    @Test
    void should_move_them_to_the_only_portal_when_it_comes_from_the_automation_api() {
        portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, false)));
        var item = storedFolder(ENV_ID, ON_ZERO_UUID);
        storedListing(ENV_ID, PortalId.ZERO);

        var moved = cut.moveOrphans(ENV_ID);

        assertThat(moved).contains(new Moved(1, 1));
        assertThat(item.getReference()).isEqualTo(onPortal(AUTOMATION_PORTAL_ID));
        assertThat(listingPortals(ENV_ID)).containsOnly(AUTOMATION_PORTAL_ID);
    }

    @Test
    void should_not_mark_an_automation_portal_as_default() {
        portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, false)));
        storedFolder(ENV_ID, ON_ZERO_UUID);

        cut.moveOrphans(ENV_ID);

        assertThat(portalCrudService.storage())
            .singleElement()
            .satisfies(portal -> assertThat(portal.isDefault()).isFalse());
    }

    @Test
    void should_change_nothing_when_the_environment_has_no_portal() {
        var item = storedFolder(ENV_ID, ON_ZERO_UUID);
        storedListing(ENV_ID, PortalId.ZERO);

        var moved = cut.moveOrphans(ENV_ID);

        assertThat(moved).isEmpty();
        assertThat(item.getReference()).isEqualTo(ON_ZERO_UUID);
        assertThat(listingPortals(ENV_ID)).containsOnly(PortalId.ZERO);
    }

    @Test
    void should_change_nothing_when_several_portals_have_no_default() {
        portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, false), aPortal(DEFAULT_PORTAL_ID, false)));
        var item = storedFolder(ENV_ID, ON_ZERO_UUID);

        var moved = cut.moveOrphans(ENV_ID);

        assertThat(moved).isEmpty();
        assertThat(item.getReference()).isEqualTo(ON_ZERO_UUID);
    }

    @Test
    void should_target_the_default_portal_among_several() {
        portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, false), aPortal(DEFAULT_PORTAL_ID, true)));
        var item = storedFolder(ENV_ID, ON_ZERO_UUID);
        var onAutomationPortal = storedFolder(ENV_ID, onPortal(AUTOMATION_PORTAL_ID));

        cut.moveOrphans(ENV_ID);

        assertThat(item.getReference()).isEqualTo(onPortal(DEFAULT_PORTAL_ID));
        assertThat(onAutomationPortal.getReference()).isEqualTo(onPortal(AUTOMATION_PORTAL_ID));
    }

    @Test
    void should_move_an_item_on_a_deleted_portal() {
        portalCrudService.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, true)));
        var item = storedFolder(ENV_ID, onPortal(DELETED_PORTAL_ID));

        cut.moveOrphans(ENV_ID);

        assertThat(item.getReference()).isEqualTo(onPortal(DEFAULT_PORTAL_ID));
    }

    @Test
    void should_leave_api_items_and_items_on_an_existing_portal_untouched() {
        portalCrudService.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, true), aPortal(AUTOMATION_PORTAL_ID, false)));
        var apiOwned = storedFolder(ENV_ID, new NavigationItemReference.ApiReference("some-api"));
        var onExistingPortal = storedFolder(ENV_ID, onPortal(AUTOMATION_PORTAL_ID));
        storedListing(ENV_ID, AUTOMATION_PORTAL_ID);

        var moved = cut.moveOrphans(ENV_ID);

        assertThat(moved).contains(new Moved(0, 0));
        assertThat(apiOwned.getReference()).isEqualTo(new NavigationItemReference.ApiReference("some-api"));
        assertThat(onExistingPortal.getReference()).isEqualTo(onPortal(AUTOMATION_PORTAL_ID));
        assertThat(listingPortals(ENV_ID)).containsOnly(AUTOMATION_PORTAL_ID);
    }

    @Test
    void should_change_only_the_reference() {
        portalCrudService.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, true)));
        var automationMetadata = new AutomationMetadata(
            AutomationMetadata.ReferenceType.PORTAL,
            AUTOMATION_PORTAL_ID.toString(),
            null,
            Optional.empty(),
            Optional.empty()
        );
        navigationItemCrudService.create(
            PortalNavigationPage.builder()
                .id(PortalNavigationItemId.random())
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .reference(ON_ZERO_UUID)
                .title("Home")
                .segment("home")
                .area(PortalArea.HOMEPAGE)
                .order(3)
                .parentId(PortalNavigationItemId.random())
                .rootId(PortalNavigationItemId.zero())
                .portalPageContentId(PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1"))
                .published(false)
                .visibility(PortalVisibility.PRIVATE)
                .automationMetadata(automationMetadata)
                .build()
        );
        navigationItemCrudService.create(
            PortalNavigationApi.builder()
                .id(PortalNavigationItemId.random())
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .reference(ON_ZERO_UUID)
                .title("My API")
                .segment("my-api")
                .area(PortalArea.TOP_NAVBAR)
                .order(5)
                .apiId("api-1")
                .categoryIds(List.of(PortalCategoryId.of("00000000-0000-0000-0000-0000000000d1")))
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build()
        );
        var before = navigationItemCrudService
            .storage()
            .stream()
            .map(PortalContentOwnershipDomainServiceTest::everythingButTheReference)
            .toList();

        cut.moveOrphans(ENV_ID);

        assertThat(navigationItemCrudService.storage())
            .allSatisfy(item -> assertThat(item.getReference()).isEqualTo(onPortal(DEFAULT_PORTAL_ID)))
            .extracting(PortalContentOwnershipDomainServiceTest::everythingButTheReference)
            .containsExactlyElementsOf(before);
    }

    private static String everythingButTheReference(PortalNavigationItem item) {
        var content = item instanceof PortalNavigationPage page ? page.getPortalPageContentId() : null;
        var categories = item instanceof PortalNavigationApi api ? api.getCategoryIds() : null;
        return String.join(
            "|",
            String.valueOf(item.getId()),
            item.getEnvironmentId(),
            item.getOrganizationId(),
            item.getTitle(),
            item.getSegment(),
            String.valueOf(item.getArea()),
            String.valueOf(item.getOrder()),
            String.valueOf(item.getParentId()),
            String.valueOf(item.getRootId()),
            String.valueOf(item.getPublished()),
            String.valueOf(item.getVisibility()),
            String.valueOf(item.getAutomationMetadata()),
            String.valueOf(content),
            String.valueOf(categories)
        );
    }

    @Test
    void should_not_touch_another_environment() {
        portalCrudService.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, true)));
        var otherEnvironmentItem = storedFolder(OTHER_ENV_ID, ON_ZERO_UUID);
        storedListing(OTHER_ENV_ID, PortalId.ZERO);

        cut.moveOrphans(ENV_ID);

        assertThat(otherEnvironmentItem.getReference()).isEqualTo(ON_ZERO_UUID);
        assertThat(listingPortals(OTHER_ENV_ID)).containsOnly(PortalId.ZERO);
    }

    /** Another node creates a portal and writes its content right after the environment's portals are read. */
    private PortalCrudServiceInMemory portalsRacingWith(PortalId concurrentPortalId) {
        return new PortalCrudServiceInMemory() {
            private boolean raced;

            @Override
            public List<Portal> findByEnvironmentId(String environmentId) {
                var portals = super.findByEnvironmentId(environmentId);
                if (!raced) {
                    raced = true;
                    create(aPortal(concurrentPortalId, false));
                    storedFolder(ENV_ID, onPortal(concurrentPortalId));
                    storedListing(ENV_ID, concurrentPortalId);
                }
                return portals;
            }
        };
    }

    private List<NavigationItemReference> navigationReferences() {
        return navigationItemCrudService.storage().stream().map(PortalNavigationItem::getReference).toList();
    }

    @Test
    void should_leave_the_content_of_a_portal_created_meanwhile_on_that_portal() {
        var concurrentPortalId = PortalId.of("33333333-3333-3333-3333-333333333333");
        var portals = portalsRacingWith(concurrentPortalId);
        portals.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, true)));
        var racingCut = new PortalContentOwnershipDomainService(portals, navigationItemCrudService, listingCrudService);
        storedFolder(ENV_ID, ON_ZERO_UUID);
        storedListing(ENV_ID, PortalId.ZERO);

        var moved = racingCut.moveOrphans(ENV_ID);

        assertThat(moved).contains(new Moved(1, 1));
        assertThat(navigationReferences()).containsExactlyInAnyOrder(onPortal(DEFAULT_PORTAL_ID), onPortal(concurrentPortalId));
        assertThat(listingPortals(ENV_ID)).containsExactlyInAnyOrder(DEFAULT_PORTAL_ID, concurrentPortalId);
    }

    @Test
    void should_leave_the_content_of_a_portal_created_meanwhile_on_that_portal_when_given_the_target() {
        var concurrentPortalId = PortalId.of("33333333-3333-3333-3333-333333333333");
        var target = aPortal(DEFAULT_PORTAL_ID, true);
        var portals = portalsRacingWith(concurrentPortalId);
        portals.initWith(List.of(target));
        var racingCut = new PortalContentOwnershipDomainService(portals, navigationItemCrudService, listingCrudService);
        storedFolder(ENV_ID, ON_ZERO_UUID);
        storedListing(ENV_ID, PortalId.ZERO);

        var moved = racingCut.moveOrphansTo(target);

        assertThat(moved).isEqualTo(new Moved(1, 1));
        assertThat(navigationReferences()).containsExactlyInAnyOrder(onPortal(DEFAULT_PORTAL_ID), onPortal(concurrentPortalId));
        assertThat(listingPortals(ENV_ID)).containsExactlyInAnyOrder(DEFAULT_PORTAL_ID, concurrentPortalId);
    }

    @Test
    void should_change_nothing_on_a_second_run() {
        portalCrudService.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, true)));
        storedFolder(ENV_ID, ON_ZERO_UUID);
        storedListing(ENV_ID, PortalId.ZERO);
        cut.moveOrphans(ENV_ID);

        var secondRun = cut.moveOrphans(ENV_ID);

        assertThat(secondRun).contains(new Moved(0, 0));
    }
}
