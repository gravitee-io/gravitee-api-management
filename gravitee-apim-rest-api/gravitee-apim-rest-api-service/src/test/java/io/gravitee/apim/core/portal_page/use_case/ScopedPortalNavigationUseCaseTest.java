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
import static fixtures.core.model.PortalNavigationItemFixtures.anApiProduct;
import static fixtures.core.model.PortalPageContentFixtures.aGraviteeMarkdownPageContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiPortalSearchQueryServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemReadDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemRenderingDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopedPortalNavigationUseCaseTest {

    private static final NavigationItemReference.ApiReference API = new NavigationItemReference.ApiReference("api-a");

    private final ArrayList<PortalNavigationItem> storage = new ArrayList<>();
    private final PortalNavigationItemsCrudServiceInMemory writes = new PortalNavigationItemsCrudServiceInMemory(storage);
    private final PortalNavigationItemsQueryServiceInMemory navigation = new PortalNavigationItemsQueryServiceInMemory(storage);
    private final PortalPageContentCrudServiceInMemory contentWrites = new PortalPageContentCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory contents = PortalPageContentQueryServiceInMemory.sharing(contentWrites.storage());
    private final ApiCrudServiceInMemory apis = new ApiCrudServiceInMemory();
    private final NavigationPortalDomainServiceInMemory portals = new NavigationPortalDomainServiceInMemory();
    private final PortalNavigationItemSourceDomainServiceInMemory source = new PortalNavigationItemSourceDomainServiceInMemory();
    private final ApiOwnedNavigationDomainService apiNavigation = new ApiOwnedNavigationDomainService(navigation, writes);
    private final PortalNavigationAccessDomainService access = new PortalNavigationAccessDomainService(
        portals.portalCrudService(),
        apis,
        navigation,
        contents,
        apiNavigation,
        new ApiQueryServiceInMemory(apis)
    );
    private final PortalNavigationItemDomainService domain = new PortalNavigationItemDomainService(
        writes,
        navigation,
        contentWrites,
        contents,
        apis,
        source,
        apiNavigation,
        portals
    );
    private final ListPortalNavigationItemsForPortalUseCase list = new ListPortalNavigationItemsForPortalUseCase(
        new PortalNavigationItemReadDomainService(
            navigation,
            List.of(),
            source,
            new ApiPortalSearchQueryServiceInMemory(),
            apiNavigation,
            new PortalNavigationItemRenderingDomainService(navigation),
            access
        )
    );
    private final UpdatePortalNavigationItemUseCase update = new UpdatePortalNavigationItemUseCase(
        access,
        new PortalNavigationItemValidatorService(navigation, contents, new ApiProductQueryServiceInMemory(), source, apis),
        domain,
        source
    );
    private final DeletePortalNavigationItemUseCase delete = new DeletePortalNavigationItemUseCase(
        access,
        domain,
        new PortalNavigationSourcedItemsDomainService(navigation)
    );
    private NavigationItemReference.PortalReference selectedPortal;

    @BeforeEach
    void set_up() {
        portals.givenDefaultPortalForNavigationFixtures();
        selectedPortal = new NavigationItemReference.PortalReference(portals.givenAutomationPortal(ORG_ID, ENV_ID));
        apis.initWith(List.of(Api.builder().id(API.apiId()).environmentId(ENV_ID).build()));
    }

    @Test
    void should_list_only_the_selected_portals_physical_tree() {
        var defaultFolder = portalFolder("Default", PORTAL_REFERENCE);
        var selectedFolder = portalFolder("Selected", selectedPortal);
        var child = aPage("Selected page", selectedFolder.getId()).toBuilder().reference(selectedPortal).build();
        navigation.initWith(List.of(defaultFolder, selectedFolder, child));

        assertThat(list(selectedPortal.portalId()).items())
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(selectedFolder.getId(), child.getId());
        assertThat(list(((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId()).items())
            .extracting(PortalNavigationItem::getId)
            .containsExactly(defaultFolder.getId());
    }

    @Test
    void should_render_shared_api_documentation_under_each_portals_own_listing() {
        var defaultFolder = portalFolder("Default APIs", PORTAL_REFERENCE);
        var selectedFolder = portalFolder("Selected APIs", selectedPortal);
        var defaultListing = apiListing(defaultFolder.getId(), PORTAL_REFERENCE);
        var selectedListing = apiListing(selectedFolder.getId(), selectedPortal);
        var document = aFolder("Shared guide").toBuilder().reference(API).build();
        document.markAsRoot();
        navigation.initWith(List.of(defaultFolder, defaultListing, selectedFolder, selectedListing, document));

        var selectedResult = list(selectedPortal.portalId()).items();
        assertThat(selectedResult)
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(selectedFolder.getId(), selectedListing.getId(), document.getId());
        assertThat(selectedResult)
            .filteredOn(item -> item.getId().equals(document.getId()))
            .singleElement()
            .satisfies(rendered -> assertThat(rendered.getParentId()).isEqualTo(selectedListing.getId()));
        var defaultResult = list(((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId()).items();
        assertThat(defaultResult)
            .filteredOn(item -> item.getId().equals(document.getId()))
            .singleElement()
            .satisfies(rendered -> assertThat(rendered.getParentId()).isEqualTo(defaultListing.getId()));
        assertThat(navigation.findByIdAndEnvironmentId(ENV_ID, document.getId()).getParentId()).isNull();
    }

    @Test
    void should_delete_only_the_selected_listing_and_its_physical_children_while_preserving_shared_documentation() {
        var defaultFolder = portalFolder("Default APIs", PORTAL_REFERENCE);
        var selectedFolder = portalFolder("Selected APIs", selectedPortal);
        var defaultListing = apiListing(defaultFolder.getId(), PORTAL_REFERENCE);
        defaultListing.updateParent(defaultFolder);
        var selectedListing = apiListing(selectedFolder.getId(), selectedPortal);
        selectedListing.updateParent(selectedFolder);
        var physicalPage = aPage("Local guide", defaultListing.getId());
        physicalPage.updateParent(defaultListing);
        var document = aPage("Shared guide", null).toBuilder().reference(API).build();
        document.markAsRoot();
        var documentBefore = document.toBuilder().build();
        var sharedContent = aGraviteeMarkdownPageContent(document.getPortalPageContentId(), ORG_ID, ENV_ID, "# Shared guide");
        contentWrites.initWith(
            List.of(sharedContent, aGraviteeMarkdownPageContent(physicalPage.getPortalPageContentId(), ORG_ID, ENV_ID, "# Local guide"))
        );
        navigation.initWith(List.of(defaultFolder, defaultListing, physicalPage, selectedFolder, selectedListing, document));

        assertRenderedUnder(defaultPortalId(), document.getId(), defaultListing.getId());
        assertRenderedUnder(selectedPortal.portalId(), document.getId(), selectedListing.getId());

        delete.execute(new DeletePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, defaultListing.getId(), PORTAL_REFERENCE));

        assertThat(navigation.findByIdAndEnvironmentId(ENV_ID, defaultListing.getId())).isNull();
        assertThat(navigation.findByIdAndEnvironmentId(ENV_ID, physicalPage.getId())).isNull();
        assertThat(contents.findById(physicalPage.getPortalPageContentId())).isEmpty();
        assertThat(navigation.findByIdAndEnvironmentId(ENV_ID, selectedListing.getId())).isInstanceOfSatisfying(
            PortalNavigationApi.class,
            stored -> {
                assertThat(stored.getApiId()).isEqualTo(API.apiId());
                assertThat(stored.getReference()).isEqualTo(selectedPortal);
                assertThat(stored.getParentId()).isEqualTo(selectedFolder.getId());
                assertThat(stored.getRootId()).isEqualTo(selectedFolder.getId());
                assertThat(stored.getOrder()).isZero();
            }
        );
        assertThat(navigation.findByIdAndEnvironmentId(ENV_ID, document.getId())).usingRecursiveComparison().isEqualTo(documentBefore);
        assertThat(document.getReference()).isEqualTo(API);
        assertThat(document.getParentId()).isNull();
        assertThat(document.getRootId()).isEqualTo(document.getId());
        assertThat(contentWrites.storage()).extracting(PortalPageContent::getId).containsExactly(sharedContent.getId());
        assertThat(contents.findById(sharedContent.getId())).contains(sharedContent);
        assertThat(sharedContent.getContent().value()).isEqualTo("# Shared guide");
        assertThat(list(defaultPortalId()).items()).extracting(PortalNavigationItem::getId).containsExactly(defaultFolder.getId());
        assertThat(list(selectedPortal.portalId()).items())
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(selectedFolder.getId(), selectedListing.getId(), document.getId());
        assertRenderedUnder(selectedPortal.portalId(), document.getId(), selectedListing.getId());
        assertThat(portals.resolveCalls()).isZero();
    }

    @Test
    void should_keep_product_documentation_separate_from_the_shared_api_documentation() {
        var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-a")
            .toBuilder()
            .reference(selectedPortal)
            .build();
        var listing = apiListing(product.getId(), selectedPortal);
        var productPage = aPage("Product guide", listing.getId()).toBuilder().reference(selectedPortal).build();
        var sharedDocument = aFolder("Shared guide").toBuilder().reference(API).build();
        navigation.initWith(List.of(product, listing, productPage, sharedDocument));

        assertThat(list(selectedPortal.portalId()).items())
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(product.getId(), listing.getId(), productPage.getId());
    }

    @Test
    void should_move_api_documentation_to_the_selected_non_default_portal() {
        var defaultFolder = portalFolder("Default", PORTAL_REFERENCE);
        var selectedFolder = portalFolder("Selected", selectedPortal);
        var listing = apiListing(selectedFolder.getId(), selectedPortal);
        var document = aFolder("Shared guide").toBuilder().reference(API).build();
        document.markAsRoot();
        navigation.initWith(List.of(defaultFolder, selectedFolder, listing, document));

        var result = update.execute(updateInput(document, selectedFolder.getId(), selectedPortal)).updatedItem();

        assertThat(result.getReference()).isEqualTo(selectedPortal);
        assertThat(result.getReference()).isNotEqualTo(PORTAL_REFERENCE);
        assertThat(result.getParentId()).isEqualTo(selectedFolder.getId());
        assertThat(result.getRootId()).isEqualTo(selectedFolder.getId());
        assertThat(portals.resolveCalls()).isZero();
        assertThat(defaultFolder.getOrder()).isZero();
    }

    @Test
    void should_refuse_moving_shared_documentation_into_one_portal_when_the_api_is_listed_in_both_portals() {
        var defaultFolder = portalFolder("Default APIs", PORTAL_REFERENCE);
        var selectedFolder = portalFolder("Selected APIs", selectedPortal);
        var defaultListing = apiListing(defaultFolder.getId(), PORTAL_REFERENCE);
        defaultListing.updateParent(defaultFolder);
        var selectedListing = apiListing(selectedFolder.getId(), selectedPortal);
        selectedListing.updateParent(selectedFolder);
        var document = aPage("Shared guide", null).toBuilder().reference(API).build();
        document.markAsRoot();
        var documentBefore = document.toBuilder().build();
        var sharedContent = aGraviteeMarkdownPageContent(document.getPortalPageContentId(), ORG_ID, ENV_ID, "# Shared guide");
        contentWrites.initWith(List.of(sharedContent));
        navigation.initWith(List.of(defaultFolder, defaultListing, selectedFolder, selectedListing, document));
        var defaultProjectionBefore = list(defaultPortalId()).items();
        var selectedProjectionBefore = list(selectedPortal.portalId()).items();
        assertRenderedUnder(defaultPortalId(), document.getId(), defaultListing.getId());
        assertRenderedUnder(selectedPortal.portalId(), document.getId(), selectedListing.getId());

        assertThatThrownBy(() -> update.execute(updateInput(document, selectedFolder.getId(), selectedPortal)))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("listed in more than one place");

        assertThat(navigation.findByIdAndEnvironmentId(ENV_ID, document.getId())).usingRecursiveComparison().isEqualTo(documentBefore);
        assertThat(document.getReference()).isEqualTo(API);
        assertThat(document.getParentId()).isNull();
        assertThat(document.getRootId()).isEqualTo(document.getId());
        assertThat(contentWrites.storage()).containsExactly(sharedContent);
        assertThat(sharedContent.getContent().value()).isEqualTo("# Shared guide");
        assertThat(list(defaultPortalId()).items()).usingRecursiveComparison().isEqualTo(defaultProjectionBefore);
        assertThat(list(selectedPortal.portalId()).items()).usingRecursiveComparison().isEqualTo(selectedProjectionBefore);
        assertRenderedUnder(defaultPortalId(), document.getId(), defaultListing.getId());
        assertRenderedUnder(selectedPortal.portalId(), document.getId(), selectedListing.getId());
        assertThat(portals.resolveCalls()).isZero();
    }

    @Test
    void should_refuse_moving_reachable_api_documentation_into_another_portal() {
        var foreignFolder = portalFolder("Default", PORTAL_REFERENCE);
        var selectedFolder = portalFolder("Selected", selectedPortal);
        var listing = apiListing(selectedFolder.getId(), selectedPortal);
        var document = aFolder("Shared guide").toBuilder().reference(API).build();
        document.markAsRoot();
        navigation.initWith(List.of(foreignFolder, selectedFolder, listing, document));

        assertThatThrownBy(() -> update.execute(updateInput(document, foreignFolder.getId(), selectedPortal))).isInstanceOf(
            PortalNavigationItemNotFoundException.class
        );
        assertThat(document.getReference()).isEqualTo(API);
        assertThat(document.getParentId()).isNull();
        assertThat(portals.resolveCalls()).isZero();
    }

    private ListPortalNavigationItemsForPortalUseCase.Output list(PortalId portalId) {
        return list.execute(
            new ListPortalNavigationItemsForPortalUseCase.Input(
                ORG_ID,
                ENV_ID,
                portalId,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                true,
                false
            )
        );
    }

    private void assertRenderedUnder(PortalId portalId, PortalNavigationItemId documentId, PortalNavigationItemId listingId) {
        assertThat(list(portalId).items())
            .filteredOn(item -> item.getId().equals(documentId))
            .singleElement()
            .satisfies(rendered -> assertThat(rendered.getParentId()).isEqualTo(listingId));
    }

    private static PortalId defaultPortalId() {
        return ((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId();
    }

    private static PortalNavigationFolder portalFolder(String title, NavigationItemReference reference) {
        var folder = aFolder(title).toBuilder().reference(reference).build();
        folder.markAsRoot();
        return folder;
    }

    private static UpdatePortalNavigationItemUseCase.Input updateInput(
        PortalNavigationItem item,
        PortalNavigationItemId parentId,
        NavigationItemReference scope
    ) {
        return new UpdatePortalNavigationItemUseCase.Input(
            ORG_ID,
            ENV_ID,
            item.getId().json(),
            UpdatePortalNavigationItem.builder()
                .type(item.getType())
                .title(item.getTitle())
                .order(0)
                .parentId(parentId)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build(),
            false,
            scope
        );
    }

    private static PortalNavigationApi apiListing(PortalNavigationItemId parentId, NavigationItemReference reference) {
        var listing = anApi(PortalNavigationItemId.random().json(), "API", parentId, API.apiId());
        listing.changeOwner(reference);
        return listing;
    }
}
