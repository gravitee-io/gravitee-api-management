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
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PublishApiToPortalUseCaseTest {

    private static final String API_ID = "api-a";
    private static final String OTHER_API_ID = "api-b";

    private final PortalNavigationItemsCrudServiceInMemory crudService = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory(
        crudService.storage()
    );
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private PublishApiToPortalUseCase useCase;

    @BeforeEach
    void setUp() {
        var pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        var pageContentQueryService = PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage());
        var sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();
        useCase = new PublishApiToPortalUseCase(
            new ApiOwnedNavigationDomainService(queryService, crudService),
            new PortalNavigationItemValidatorService(
                queryService,
                pageContentQueryService,
                new ApiProductQueryServiceInMemory(),
                sourceDomainService
            ),
            new PortalNavigationItemDomainService(
                crudService,
                queryService,
                pageContentCrudService,
                pageContentQueryService,
                apiCrudService,
                sourceDomainService
            )
        );
        apiCrudService.initWith(List.of(Api.builder().id(API_ID).name("Api A").environmentId(ENV_ID).build()));
    }

    @Test
    void should_create_a_published_listing_row_under_the_chosen_section() {
        var section = aFolder("APIs");
        queryService.initWith(List.of(section));

        var output = publishTo(section.getId());

        assertThat(output.section()).isEqualTo(section);
        assertThat(stored(output.listing().getId())).isInstanceOfSatisfying(PortalNavigationApi.class, listing -> {
            assertThat(listing.getApiId()).isEqualTo(API_ID);
            assertThat(listing.getTitle()).isEqualTo("Api A");
            assertThat(listing.getParentId()).isEqualTo(section.getId());
            assertThat(listing.getPublished()).isTrue();
            assertThat(listing.getReference()).isEqualTo(NavigationItemReference.defaultReference());
        });
    }

    @Test
    void should_publish_every_item_owned_by_the_api_including_those_hidden_individually() {
        var section = aFolder("APIs");
        var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).published(false).build();
        var nestedPage = aPage("Setup", folder.getId()).toBuilder().reference(ownedBy(API_ID)).published(false).build();
        var alreadyPublished = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).published(true).build();
        queryService.initWith(List.of(section, folder, nestedPage, alreadyPublished));

        publishTo(section.getId());

        assertThat(List.of(stored(folder.getId()), stored(nestedPage.getId()), stored(alreadyPublished.getId()))).allSatisfy(item ->
            assertThat(item.getPublished()).isTrue()
        );
    }

    @Test
    void should_not_set_a_stored_parent_on_the_api_items() {
        var section = aFolder("APIs");
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).published(false).build();
        queryService.initWith(List.of(section, page));

        publishTo(section.getId());

        assertThat(stored(page.getId()).getParentId()).isNull();
        assertThat(stored(page.getId()).getReference()).isEqualTo(ownedBy(API_ID));
    }

    @Test
    void should_not_touch_items_owned_by_another_api() {
        var section = aFolder("APIs");
        var foreign = aPage("Other overview", null).toBuilder().reference(ownedBy(OTHER_API_ID)).published(false).build();
        queryService.initWith(List.of(section, foreign));

        publishTo(section.getId());

        assertThat(stored(foreign.getId()).getPublished()).isFalse();
    }

    @Test
    void should_publish_an_api_with_no_documentation() {
        var section = aFolder("APIs");
        queryService.initWith(List.of(section));

        publishTo(section.getId());

        assertThat(crudService.storage()).hasSize(2);
    }

    @Test
    void should_publish_standalone_when_api_is_listed_only_under_an_api_product() {
        var section = aFolder("APIs");
        var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
        var productListing = anApi(PortalNavigationItemId.random().json(), "Api A", product.getId(), API_ID);
        queryService.initWith(List.of(section, product, productListing));

        var output = publishTo(section.getId());

        assertThat(output.listing().getParentId()).isEqualTo(section.getId());
        assertThat(stored(productListing.getId())).isNotNull();
    }

    @Test
    void should_reject_when_api_is_already_listed() {
        var section = aFolder("APIs");
        var otherSection = aFolder("Partners");
        var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
        queryService.initWith(List.of(section, otherSection, listing));

        assertThatThrownBy(() -> publishTo(otherSection.getId())).isInstanceOf(InvalidPortalNavigationItemDataException.class);
    }

    @Test
    void should_reject_when_the_listing_row_exists_but_is_unpublished_and_leave_everything_unchanged() {
        var section = aFolder("APIs");
        var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
        listing.setPublished(false);
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).published(false).build();
        queryService.initWith(List.of(section, listing, page));

        assertThatThrownBy(() -> publishTo(section.getId())).isInstanceOf(InvalidPortalNavigationItemDataException.class);

        assertThat(crudService.storage()).hasSize(3);
        assertThat(stored(listing.getId()).getPublished()).isFalse();
        assertThat(stored(page.getId()).getPublished()).isFalse();
    }

    @Test
    void should_reject_when_no_section_is_given() {
        assertThatThrownBy(() -> publishTo(null)).isInstanceOf(InvalidPortalNavigationItemDataException.class);
    }

    @Test
    void should_reject_an_unknown_section_without_creating_one() {
        assertThatThrownBy(() -> publishTo(PortalNavigationItemId.random())).isInstanceOf(ParentNotFoundException.class);

        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_reject_a_target_that_is_not_a_publish_location_and_leave_documentation_unpublished() {
        var section = aFolder("APIs");
        var nested = aFolder("Payments", section.getId());
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).published(false).build();
        queryService.initWith(List.of(section, nested, page));

        assertThatThrownBy(() -> publishTo(nested.getId())).isInstanceOf(InvalidPortalNavigationItemDataException.class);

        assertThat(stored(page.getId()).getPublished()).isFalse();
        assertThat(crudService.storage()).hasSize(3);
    }

    private PublishApiToPortalUseCase.Output publishTo(PortalNavigationItemId sectionId) {
        return useCase.execute(new PublishApiToPortalUseCase.Input(ORG_ID, ENV_ID, API_ID, sectionId));
    }

    private PortalNavigationItem stored(PortalNavigationItemId id) {
        return queryService.findByIdAndEnvironmentId(ENV_ID, id);
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }
}
