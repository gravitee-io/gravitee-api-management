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
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static fixtures.core.model.PortalNavigationItemFixtures.anApiProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UnpublishApiFromPortalUseCaseTest {

    private static final String API_ID = "api-a";
    private static final String OTHER_API_ID = "api-b";

    private PortalNavigationItemId failingUpdateOf;

    private final PortalNavigationItemsCrudServiceInMemory crudService = new PortalNavigationItemsCrudServiceInMemory() {
        @Override
        public PortalNavigationItem update(PortalNavigationItem portalNavigationItem) {
            if (portalNavigationItem.getId().equals(failingUpdateOf)) {
                throw new IllegalStateException("update failed");
            }
            return super.update(portalNavigationItem);
        }
    };
    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory(
        crudService.storage()
    );
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private UnpublishApiFromPortalUseCase useCase;
    private PublishApiToPortalUseCase publishUseCase;

    @BeforeEach
    void setUp() {
        var pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        var pageContentQueryService = PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage());
        var sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();
        var apiOwnedNavigationDomainService = new ApiOwnedNavigationDomainService(queryService, crudService);
        var domainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            pageContentQueryService,
            apiCrudService,
            sourceDomainService
        );
        var validatorService = new PortalNavigationItemValidatorService(
            queryService,
            pageContentQueryService,
            new ApiProductQueryServiceInMemory(),
            sourceDomainService
        );
        useCase = new UnpublishApiFromPortalUseCase(apiOwnedNavigationDomainService, validatorService, domainService);
        publishUseCase = new PublishApiToPortalUseCase(apiOwnedNavigationDomainService, validatorService, domainService);
        apiCrudService.initWith(List.of(Api.builder().id(API_ID).name("Api A").environmentId(ENV_ID).build()));
    }

    @Test
    void should_hide_the_listing_row_instead_of_deleting_it() {
        var section = aSection();
        var listing = aListing(section, API_ID);
        queryService.initWith(List.of(section, listing));

        unpublish();

        assertThat(stored(listing.getId())).satisfies(hidden -> {
            assertThat(hidden.getPublished()).isFalse();
            assertThat(hidden.getParentId()).isEqualTo(section.getId());
            assertThat(hidden.getOrder()).isEqualTo(listing.getOrder());
            assertThat(hidden.getSegment()).isEqualTo(listing.getSegment());
        });
        assertThat(stored(section.getId()).getPublished()).isTrue();
    }

    @Test
    void should_unpublish_every_item_owned_by_the_api_without_deleting_any() {
        var section = aSection();
        var listing = aListing(section, API_ID);
        var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build();
        folder.markAsRoot();
        var nestedPage = aPage("Setup", folder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
        nestedPage.updateParent(folder);
        queryService.initWith(List.of(section, listing, folder, nestedPage));

        unpublish();

        assertThat(crudService.storage()).hasSize(4);
        assertThat(stored(folder.getId()).getPublished()).isFalse();
        assertThat(stored(nestedPage.getId()).getPublished()).isFalse();
        assertThat(stored(nestedPage.getId()).getParentId()).isEqualTo(folder.getId());
        assertThat(stored(folder.getId()).getReference()).isEqualTo(ownedBy(API_ID));
    }

    /**
     * Pages the portal editor stored as real children of the listing row are descendants of it. They are
     * hidden with the row and stay where they are.
     */
    @Test
    void should_keep_and_hide_what_is_stored_under_the_listing_row() {
        var section = aSection();
        var listing = aListing(section, API_ID);
        var storedUnderListing = aPage("Legacy page", listing.getId());
        storedUnderListing.updateParent(listing);
        queryService.initWith(List.of(section, listing, storedUnderListing));

        unpublish();

        assertThat(stored(storedUnderListing.getId())).satisfies(page -> {
            assertThat(page.getPublished()).isFalse();
            assertThat(page.getParentId()).isEqualTo(listing.getId());
        });
    }

    @Test
    void should_not_touch_the_listing_or_the_items_of_another_api() {
        var section = aSection();
        var listing = aListing(section, API_ID);
        var otherListing = aListing(section, OTHER_API_ID);
        var foreign = aPage("Other overview", null).toBuilder().reference(ownedBy(OTHER_API_ID)).build();
        foreign.markAsRoot();
        queryService.initWith(List.of(section, listing, otherListing, foreign));

        unpublish();

        assertThat(stored(otherListing.getId()).getPublished()).isTrue();
        assertThat(stored(foreign.getId()).getPublished()).isTrue();
    }

    @Test
    void should_not_touch_a_listing_row_under_an_api_product() {
        var section = aSection();
        var listing = aListing(section, API_ID);
        var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
        product.markAsRoot();
        var productListing = anApi(PortalNavigationItemId.random().json(), "Api A", product.getId(), API_ID);
        productListing.updateParent(product);
        queryService.initWith(List.of(section, listing, product, productListing));

        unpublish();

        assertThat(stored(productListing.getId()).getPublished()).isTrue();
        assertThat(stored(listing.getId()).getPublished()).isFalse();
    }

    @Test
    void should_reject_when_api_is_not_listed() {
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).build();
        page.markAsRoot();
        queryService.initWith(List.of(page));

        assertThatThrownBy(this::unpublish).isInstanceOf(InvalidPortalNavigationItemDataException.class);

        assertThat(stored(page.getId()).getPublished()).isTrue();
    }

    /**
     * A hidden listing means the API is not published, whoever hid it.
     */
    @Test
    void should_reject_when_the_listing_row_is_already_hidden() {
        var section = aSection();
        var listing = aListing(section, API_ID);
        listing.setPublished(false);
        queryService.initWith(List.of(section, listing));

        assertThatThrownBy(this::unpublish).isInstanceOf(InvalidPortalNavigationItemDataException.class);

        assertThat(stored(listing.getId())).isNotNull();
    }

    @Test
    void should_reject_when_api_is_listed_only_under_an_api_product() {
        var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
        product.markAsRoot();
        var productListing = anApi(PortalNavigationItemId.random().json(), "Api A", product.getId(), API_ID);
        productListing.updateParent(product);
        queryService.initWith(List.of(product, productListing));

        assertThatThrownBy(this::unpublish).isInstanceOf(InvalidPortalNavigationItemDataException.class);

        assertThat(stored(productListing.getId()).getPublished()).isTrue();
    }

    @Test
    void should_publish_again_by_showing_the_same_listing_row() {
        var section = aSection();
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).published(false).build();
        page.markAsRoot();
        queryService.initWith(List.of(section, page));
        var input = new PublishApiToPortalUseCase.Input(ORG_ID, ENV_ID, API_ID, section.getId());

        var published = publishUseCase.execute(input);
        unpublish();
        var republished = publishUseCase.execute(input);

        assertThat(republished.listing().getId()).isEqualTo(published.listing().getId());
        assertThat(stored(republished.listing().getId()).getPublished()).isTrue();
        assertThat(stored(page.getId()).getPublished()).isTrue();
        assertThat(crudService.storage()).hasSize(3);
    }

    /**
     * Nothing here is transactional. The items are hidden before the listing, so that a failure leaves
     * the API still published, and unpublishing again finishes the job.
     */
    @Test
    void should_leave_the_listing_visible_when_hiding_the_items_fails_so_that_a_retry_succeeds() {
        var section = aSection();
        var listing = aListing(section, API_ID);
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).build();
        page.markAsRoot();
        queryService.initWith(List.of(section, listing, page));
        failingUpdateOf = page.getId();

        assertThatThrownBy(this::unpublish).isInstanceOf(IllegalStateException.class);
        assertThat(stored(listing.getId()).getPublished()).isTrue();

        failingUpdateOf = null;
        unpublish();

        assertThat(stored(listing.getId()).getPublished()).isFalse();
        assertThat(stored(page.getId()).getPublished()).isFalse();
    }

    private void unpublish() {
        useCase.execute(new UnpublishApiFromPortalUseCase.Input(ENV_ID, API_ID));
    }

    private PortalNavigationItem stored(PortalNavigationItemId id) {
        return queryService.findByIdAndEnvironmentId(ENV_ID, id);
    }

    private static PortalNavigationFolder aSection() {
        var section = aFolder("APIs");
        section.markAsRoot();
        return section;
    }

    private static PortalNavigationApi aListing(PortalNavigationFolder section, String apiId) {
        var listing = anApi(PortalNavigationItemId.random().json(), apiId, section.getId(), apiId);
        listing.updateParent(section);
        return listing;
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }
}
