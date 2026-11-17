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
import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiPortalSearchQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemReadDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemRenderingDomainService;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListPortalNavigationItemsForPortalUseCaseTest {

    private final ArrayList<PortalNavigationItem> storage = new ArrayList<>();
    private final PortalNavigationItemsQueryServiceInMemory navigation = spy(new PortalNavigationItemsQueryServiceInMemory(storage));
    private final PortalNavigationItemsCrudServiceInMemory navigationWrites = new PortalNavigationItemsCrudServiceInMemory(storage);
    private final PortalPageContentCrudServiceInMemory contentWrites = new PortalPageContentCrudServiceInMemory();
    private final ApiCrudServiceInMemory apis = new ApiCrudServiceInMemory();
    private final ApiPortalSearchQueryServiceInMemory apiSearch = new ApiPortalSearchQueryServiceInMemory();
    private final NavigationPortalDomainServiceInMemory portals = new NavigationPortalDomainServiceInMemory();
    private final ApiQueryServiceInMemory apiQueries = spy(new ApiQueryServiceInMemory(apis));
    private final ApiOwnedNavigationDomainService apiNavigation = new ApiOwnedNavigationDomainService(navigation, navigationWrites);
    private final ListPortalNavigationItemsForPortalUseCase useCase = new ListPortalNavigationItemsForPortalUseCase(
        new PortalNavigationItemReadDomainService(
            navigation,
            List.of(),
            new PortalNavigationItemSourceDomainServiceInMemory(),
            apiSearch,
            apiNavigation,
            new PortalNavigationItemRenderingDomainService(navigation),
            new PortalNavigationAccessDomainService(
                portals.portalCrudService(),
                apis,
                navigation,
                PortalPageContentQueryServiceInMemory.sharing(contentWrites.storage()),
                apiNavigation,
                apiQueries
            )
        )
    );
    private NavigationItemReference.PortalReference selectedPortal;

    @BeforeEach
    void set_up() {
        portals.givenDefaultPortalForNavigationFixtures();
        selectedPortal = new NavigationItemReference.PortalReference(portals.givenAutomationPortal(ORG_ID, ENV_ID));
    }

    @Test
    void should_require_a_concrete_portal_id() {
        assertThatThrownBy(() ->
            new ListPortalNavigationItemsForPortalUseCase.Input(ORG_ID, ENV_ID, null, PortalArea.TOP_NAVBAR, Optional.empty(), true, false)
        )
            .isInstanceOf(NullPointerException.class)
            .hasMessage("portalId");
    }

    @Test
    void should_read_only_the_selected_portal_including_unpublished_private_console_items() {
        var foreign = folder("Default", PORTAL_REFERENCE);
        var selected = folder("Selected", selectedPortal).toBuilder().published(false).visibility(PortalVisibility.PRIVATE).build();
        var child = aPage("Selected child", selected.getId())
            .toBuilder()
            .reference(selectedPortal)
            .published(false)
            .visibility(PortalVisibility.PRIVATE)
            .build();
        navigation.initWith(List.of(foreign, selected, child));

        var output = useCase.execute(input(Optional.empty(), false));

        assertThat(output.items()).extracting(PortalNavigationItem::getId).containsExactlyInAnyOrder(selected.getId(), child.getId());
        assertThat(output.apis()).isEmpty();
    }

    @Test
    void should_reject_an_unknown_portal() {
        var unknown = PortalId.of(UUID.randomUUID().toString());

        assertThatThrownBy(() ->
            useCase.execute(
                new ListPortalNavigationItemsForPortalUseCase.Input(
                    ORG_ID,
                    ENV_ID,
                    unknown,
                    PortalArea.TOP_NAVBAR,
                    Optional.empty(),
                    true,
                    false
                )
            )
        ).isInstanceOf(PortalNotFoundException.class);
    }

    @Test
    void should_reject_a_parent_belonging_to_another_portal() {
        var foreign = folder("Default", PORTAL_REFERENCE);
        navigation.initWith(List.of(foreign));

        assertThatThrownBy(() -> useCase.execute(input(Optional.of(foreign.getId()), false))).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
    }

    @Test
    void should_reject_an_unknown_parent_in_the_selected_portal() {
        var unknownParentId = PortalNavigationItemId.random();

        assertThatThrownBy(() -> useCase.execute(input(Optional.of(unknownParentId), false)))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found")
            .extracting(error -> ((PortalNavigationItemNotFoundException) error).getId())
            .isEqualTo(unknownParentId.json());
    }

    @Test
    void should_reject_a_returned_item_belonging_to_another_organization() {
        var corrupt = folder("Corrupt", selectedPortal).toBuilder().organizationId("other-org").build();
        navigation.initWith(List.of(corrupt));

        assertThatThrownBy(() -> useCase.execute(input(Optional.empty(), true))).isInstanceOf(PortalNavigationItemNotFoundException.class);
    }

    @Test
    void should_include_api_metadata_only_for_items_under_the_selected_parent() {
        var foreign = folder("Default", PORTAL_REFERENCE);
        var selected = folder("Selected", selectedPortal);
        var selectedListing = listing("api-a", selected);
        var foreignListing = listing("api-b", foreign);
        var selectedApi = Api.builder().id("api-a").name("Selected API").environmentId(ENV_ID).build();
        var foreignApi = Api.builder().id("api-b").name("Foreign API").environmentId(ENV_ID).build();
        apis.initWith(List.of(selectedApi, foreignApi));
        apiSearch.initWith(List.of(selectedApi, foreignApi));
        navigation.initWith(List.of(foreign, selected, selectedListing, foreignListing));

        var output = useCase.execute(input(Optional.of(selected.getId()), true));

        assertThat(output.items()).extracting(PortalNavigationItem::getId).containsExactly(selectedListing.getId());
        assertThat(output.apis()).hasSize(1).containsEntry(selectedListing.getId(), selectedApi);
    }

    @Test
    void should_validate_a_wide_result_without_reloading_each_returned_item() {
        var pages = IntStream.range(0, 100)
            .mapToObj(index -> (PortalNavigationItem) aPage("Page " + index, null).toBuilder().reference(selectedPortal).build())
            .toList();
        navigation.initWith(pages);

        var output = useCase.execute(input(Optional.empty(), false));

        assertThat(output.items()).containsExactlyInAnyOrderElementsOf(pages);
        verify(navigation, never()).findByIdAndEnvironmentId(any(), any());
    }

    @Test
    void should_render_shared_documentation_twice_and_validate_its_canonical_owner_once() {
        var section = folder("APIs", selectedPortal);
        var firstListing = listing("api-a", section);
        var secondListing = listing("api-a", section);
        var document = aPage("Shared guide", null).toBuilder().reference(new NavigationItemReference.ApiReference("api-a")).build();
        document.markAsRoot();
        apis.initWith(List.of(Api.builder().id("api-a").environmentId(ENV_ID).build()));
        navigation.initWith(List.of(section, firstListing, secondListing, document));

        var output = useCase.execute(input(Optional.empty(), false));

        assertThat(output.items())
            .filteredOn(item -> item.getId().equals(document.getId()))
            .extracting(PortalNavigationItem::getParentId)
            .containsExactlyInAnyOrder(firstListing.getId(), secondListing.getId());
        assertThat(document.getParentId()).isNull();
        assertThat(document.getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-a"));
        verify(apiQueries).findExistingIds(Set.of("api-a"), ENV_ID);
        verify(navigation, never()).findByIdAndEnvironmentId(ENV_ID, document.getId());
    }

    @Test
    void should_reject_shared_documentation_when_the_api_belongs_to_another_environment() {
        var section = folder("APIs", selectedPortal);
        var listing = listing("api-a", section);
        var document = aPage("Shared guide", null).toBuilder().reference(new NavigationItemReference.ApiReference("api-a")).build();
        document.markAsRoot();
        apis.initWith(List.of(Api.builder().id("api-a").environmentId("other-environment").build()));
        navigation.initWith(List.of(section, listing, document));

        assertThatThrownBy(() -> useCase.execute(input(Optional.empty(), false))).isInstanceOf(PortalNavigationItemNotFoundException.class);
    }

    @Test
    void should_reuse_api_parent_access_granted_by_a_listing_outside_the_returned_area() {
        var section = folder("Homepage APIs", selectedPortal).toBuilder().area(PortalArea.HOMEPAGE).build();
        var listing = listing("api-a", section);
        listing.setArea(PortalArea.HOMEPAGE);
        var document = folder("Shared guide", new NavigationItemReference.ApiReference("api-a"));
        var page = aPage("Child", document.getId()).toBuilder().reference(document.getReference()).build();
        apis.initWith(List.of(Api.builder().id("api-a").environmentId(ENV_ID).build()));
        navigation.initWith(List.of(section, listing, document, page));

        var output = useCase.execute(input(Optional.of(document.getId()), false));

        assertThat(output.items()).containsExactly(page);
        verify(navigation).findByIdAndEnvironmentId(ENV_ID, document.getId());
        verify(apiQueries).findExistingIds(Set.of("api-a"), ENV_ID);
    }

    private ListPortalNavigationItemsForPortalUseCase.Input input(Optional<PortalNavigationItemId> parentId, boolean includeApis) {
        return new ListPortalNavigationItemsForPortalUseCase.Input(
            ORG_ID,
            ENV_ID,
            selectedPortal.portalId(),
            PortalArea.TOP_NAVBAR,
            parentId,
            true,
            includeApis
        );
    }

    private static PortalNavigationFolder folder(String title, NavigationItemReference reference) {
        var folder = aFolder(title).toBuilder().reference(reference).build();
        folder.markAsRoot();
        return folder;
    }

    private static PortalNavigationApi listing(String apiId, PortalNavigationFolder parent) {
        var listing = anApi(PortalNavigationItemId.random().json(), "Listing of " + apiId, parent.getId(), apiId);
        listing.changeOwner(parent.getReference());
        listing.updateParent(parent);
        return listing;
    }
}
