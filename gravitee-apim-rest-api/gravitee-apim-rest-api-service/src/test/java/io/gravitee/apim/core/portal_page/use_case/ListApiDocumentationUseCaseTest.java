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

import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListApiDocumentationUseCaseTest {

    private static final String API_ID = "api-a";
    private static final String OTHER_API_ID = "api-b";

    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory();
    private final ListApiDocumentationUseCase useCase = new ListApiDocumentationUseCase(
        new ApiOwnedNavigationDomainService(queryService, new PortalNavigationItemsCrudServiceInMemory()),
        queryService,
        new PortalNavigationItemSourceDomainServiceInMemory()
    );

    @Test
    void should_return_empty_when_api_has_no_documentation() {
        queryService.initWith(List.of(aFolder("Guides")));

        var output = execute();

        assertThat(output.items()).isEmpty();
        assertThat(output.publications()).isEmpty();
    }

    @Test
    void should_return_pages_folders_and_links_owned_by_the_api() {
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).build();
        var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build();
        var link = PortalNavigationLink.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("Status page")
            .segment("status-page")
            .area(PortalArea.TOP_NAVBAR)
            .order(2)
            .url("https://status.example.com")
            .published(false)
            .visibility(PortalVisibility.PUBLIC)
            .reference(ownedBy(API_ID))
            .build();
        queryService.initWith(List.of(page, folder, link));

        var output = execute();

        assertThat(output.items())
            .extracting(PortalNavigationItem::getType)
            .containsExactlyInAnyOrder(PortalNavigationItemType.PAGE, PortalNavigationItemType.FOLDER, PortalNavigationItemType.LINK);
    }

    @Test
    void should_return_nested_items_at_any_depth() {
        var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build();
        var nestedFolder = aFolder("OAuth", folder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
        var nestedPage = aPage("Setup", nestedFolder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
        queryService.initWith(List.of(folder, nestedFolder, nestedPage));

        var output = execute();

        assertThat(output.items())
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(folder.getId(), nestedFolder.getId(), nestedPage.getId());
    }

    @Test
    void should_return_the_stored_parent_not_the_rendered_one() {
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).build();
        var section = aFolder("APIs");
        var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
        queryService.initWith(List.of(page, section, listing));

        var output = execute();

        assertThat(output.items()).singleElement().extracting(PortalNavigationItem::getParentId).isNull();
    }

    @Test
    void should_not_return_items_owned_by_another_api() {
        var foreign = aPage("Other overview", null).toBuilder().reference(ownedBy(OTHER_API_ID)).build();
        queryService.initWith(List.of(foreign));

        assertThat(execute().items()).isEmpty();
    }

    @Test
    void should_not_return_portal_owned_items() {
        var section = aFolder("APIs");
        var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
        var storedUnderListing = aPage("Legacy page", listing.getId());
        queryService.initWith(List.of(section, listing, storedUnderListing));

        assertThat(execute().items()).isEmpty();
    }

    @Test
    void should_mask_the_sensitive_data_of_a_source() {
        var source = PortalNavigationItemSource.builder()
            .sourceType("github")
            .sourceConfiguration(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA)
            .build();
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).source(source).build();
        queryService.initWith(List.of(page));

        var output = execute();

        assertThat(output.items().getFirst().getSource().getSourceConfiguration()).isEqualTo(
            PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT
        );
    }

    @Test
    void should_report_not_listed_when_api_has_no_listing_row() {
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).build();
        queryService.initWith(List.of(page));

        assertThat(execute().publications()).isEmpty();
    }

    @Test
    void should_report_the_listing_row_its_section_and_its_portal() {
        var section = aFolder("APIs");
        var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
        queryService.initWith(List.of(section, listing));

        assertThat(execute().publications())
            .singleElement()
            .satisfies(publication -> {
                assertThat(publication.listing()).isEqualTo(listing);
                assertThat(publication.listing().getPublished()).isTrue();
                assertThat(publication.section()).isEqualTo(section);
                assertThat(publication.portalId()).isEqualTo(PortalId.ZERO);
            });
    }

    @Test
    void should_report_listed_but_unpublished_after_an_editor_unpublish() {
        var section = aFolder("APIs");
        var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
        listing.setPublished(false);
        queryService.initWith(List.of(section, listing));

        assertThat(execute().publications())
            .singleElement()
            .satisfies(publication -> assertThat(publication.listing().getPublished()).isFalse());
    }

    @Test
    void should_report_one_publication_per_listing_row() {
        var section = aFolder("APIs");
        var otherSection = aFolder("Partners");
        var listing = anApi(PortalNavigationItemId.random().json(), "Api A", section.getId(), API_ID);
        var otherListing = anApi(PortalNavigationItemId.random().json(), "Api A", otherSection.getId(), API_ID);
        queryService.initWith(List.of(section, otherSection, listing, otherListing));

        assertThat(execute().publications())
            .extracting(publication -> publication.section().getTitle())
            .containsExactlyInAnyOrder("APIs", "Partners");
    }

    @Test
    void should_report_not_listed_when_api_is_listed_only_under_an_api_product() {
        var product = anApiProduct(PortalNavigationItemId.random().json(), "Product", null, "product-id");
        var listing = anApi(PortalNavigationItemId.random().json(), "Api A", product.getId(), API_ID);
        queryService.initWith(List.of(product, listing));

        assertThat(execute().publications()).isEmpty();
    }

    private ListApiDocumentationUseCase.Output execute() {
        return useCase.execute(new ListApiDocumentationUseCase.Input(ENV_ID, API_ID));
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }
}
