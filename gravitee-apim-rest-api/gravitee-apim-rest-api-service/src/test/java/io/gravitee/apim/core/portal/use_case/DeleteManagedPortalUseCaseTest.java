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
package io.gravitee.apim.core.portal.use_case;

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE12_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalFixtures;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.domain_service.PortalContentOwnershipDomainService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteManagedPortalUseCaseTest {

    private static final PortalId TARGET = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalId OTHER = PortalId.of("22222222-2222-2222-2222-222222222222");
    private final PortalCrudServiceInMemory portals = new PortalCrudServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navigation = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory query = new PortalNavigationItemsQueryServiceInMemory(navigation.storage());
    private final PortalPageContentCrudServiceInMemory contents = new PortalPageContentCrudServiceInMemory();
    private final PortalListingCrudServiceInMemory listings = new PortalListingCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory contentQuery = PortalPageContentQueryServiceInMemory.sharing(contents.storage());
    private final ApiOwnedNavigationDomainService apiNavigation = new ApiOwnedNavigationDomainService(query, navigation);
    private final DeleteManagedPortalUseCase useCase = new DeleteManagedPortalUseCase(
        portals,
        query,
        navigation,
        contents,
        contentQuery,
        apiNavigation,
        listings
    );

    @Test
    void should_remove_owned_items_and_content_but_keep_other_portal_and_shared_content() {
        var target = PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target");
        var other = PortalFixtures.aPortal(OTHER, ENV_ID, ORG_ID, "Other");
        portals.initWith(List.of(target, other));
        var exclusive = contents.createDefault(ORG_ID, ENV_ID, PortalPageContentType.GRAVITEE_MARKDOWN);
        var shared = contents.createDefault(ORG_ID, ENV_ID, PortalPageContentType.GRAVITEE_MARKDOWN);
        var folder = aFolder("folder");
        var page = aPage(PAGE_ID, "page", folder.getId(), exclusive.getId());
        var sharedPage = aPage(PAGE11_ID, "shared", folder.getId(), shared.getId());
        var otherPage = aPage(PAGE12_ID, "other", null, shared.getId())
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(OTHER))
            .build();
        page.setPublished(false);
        navigation.initWith(List.of(folder, page, sharedPage, otherPage));

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(portals.storage()).containsExactly(other);
        assertThat(navigation.storage()).containsExactly(otherPage);
        assertThat(contents.storage()).containsExactly(shared);
        new PortalContentOwnershipDomainService(portals, navigation).moveOrphans(ENV_ID);
        assertThat(navigation.storage()).containsExactly(otherPage);
    }

    @Test
    void should_refuse_default_and_unknown_portals_without_mutations() {
        var defaultPortal = Portal.of(
            TARGET,
            ENV_ID,
            ORG_ID,
            "Default",
            PortalNavigationStructure.empty(),
            null,
            true,
            PortalFixtures.PORTAL_URL
        );
        portals.initWith(List.of(defaultPortal));
        var folder = aFolder("folder");
        navigation.initWith(List.of(folder));

        assertThatThrownBy(() -> useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET)))
            .isInstanceOf(ValidationDomainException.class)
            .extracting("technicalCode")
            .isEqualTo("portal.default.notDeletable");
        assertThatThrownBy(() -> useCase.execute(new DeleteManagedPortalUseCase.Input("other-env", TARGET))).isInstanceOf(
            PortalNotFoundException.class
        );
        assertThat(portals.storage()).containsExactly(defaultPortal);
        assertThat(navigation.storage()).containsExactly(folder);
    }

    @Test
    void should_leave_portal_when_content_delete_fails_and_allow_retry() {
        var target = PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target");
        portals.initWith(List.of(target));
        var content = contents.createDefault(ORG_ID, ENV_ID, PortalPageContentType.GRAVITEE_MARKDOWN);
        var page = aPage(PAGE_ID, "page", null, content.getId());
        navigation.initWith(List.of(page));
        var failingContent = new PortalPageContentCrudServiceInMemory() {
            @Override
            public void delete(PortalPageContentId id) {
                throw new IllegalStateException("unavailable");
            }
        };
        var failing = new DeleteManagedPortalUseCase(portals, query, navigation, failingContent, contentQuery, apiNavigation, listings);

        assertThatThrownBy(() -> failing.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET))).hasMessage("unavailable");
        assertThat(portals.storage()).containsExactly(target);
        assertThat(navigation.storage()).containsExactly(page);
        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));
        assertThat(portals.storage()).isEmpty();
        assertThat(navigation.storage()).isEmpty();
        assertThat(contents.storage()).isEmpty();
    }

    @Test
    void should_hide_listing_before_unpublishing_and_finish_after_cleanup_failure() {
        var target = PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target");
        portals.initWith(List.of(target));
        var content = contents.createDefault(ORG_ID, ENV_ID, PortalPageContentType.GRAVITEE_MARKDOWN);
        var page = aPage(PAGE_ID, "page", null, content.getId());
        var listing = anApi(PAGE11_ID, "API", null, "api");
        var apiPage = aPage(PAGE12_ID, "docs", null).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        navigation.initWith(List.of(page, listing, apiPage));
        var failingContent = new PortalPageContentCrudServiceInMemory() {
            @Override
            public void delete(PortalPageContentId id) {
                throw new IllegalStateException("unavailable");
            }
        };
        var failing = new DeleteManagedPortalUseCase(portals, query, navigation, failingContent, contentQuery, apiNavigation, listings);

        assertThatThrownBy(() -> failing.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET))).hasMessage("unavailable");
        assertThat(listing.getPublished()).isFalse();
        assertThat(apiPage.getPublished()).isFalse();
        assertThat(portals.storage()).containsExactly(target);

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));
        assertThat(portals.storage()).isEmpty();
        assertThat(navigation.storage()).containsExactly(apiPage);
        assertThat(apiPage.getPublished()).isFalse();
    }

    @Test
    void should_finish_unpublishing_after_failure_on_previously_hidden_listing() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var listing = anApi(PAGE_ID, "API", null, "api");
        listing.setPublished(false);
        var apiPage = aPage(PAGE12_ID, "docs", null).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        navigation.initWith(List.of(listing, apiPage));

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(apiPage.getPublished()).isFalse();
    }

    @Test
    void should_restore_documentation_when_another_portal_publishes_during_deletion() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var listing = anApi(PAGE_ID, "API", null, "api");
        var apiPage = aPage(PAGE12_ID, "docs", null).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        var otherListing = PortalNavigationApi.builder()
            .id(PortalNavigationItemId.of(PAGE11_ID))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(OTHER))
            .title("API")
            .segment("api")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId("api")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        navigation.initWith(List.of(listing, apiPage));
        var racing = new ApiOwnedNavigationDomainService(query, navigation) {
            @Override
            public void setPublished(String environmentId, String apiId, boolean published) {
                super.setPublished(environmentId, apiId, published);
                if (!published) navigation.create(otherListing);
            }
        };
        var subject = new DeleteManagedPortalUseCase(portals, query, navigation, contents, contentQuery, racing, listings);

        subject.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(navigation.storage()).containsExactly(apiPage, otherListing);
        assertThat(apiPage.getPublished()).isTrue();
    }

    @Test
    void should_preserve_api_documentation_and_other_portal_listing() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var targetListing = anApi(PAGE_ID, "API", null, "api");
        var section = aFolder("other section").toBuilder().reference(new NavigationItemReference.PortalReference(OTHER)).build();
        var otherListing = PortalNavigationApi.builder()
            .id(PortalNavigationItemId.of(PAGE11_ID))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(OTHER))
            .title("API")
            .segment("api")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId("api")
            .parentId(section.getId())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        var apiPage = aPage(PAGE12_ID, "docs", null).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        navigation.initWith(List.of(targetListing, section, otherListing, apiPage));

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(navigation.storage()).containsExactly(section, otherListing, apiPage);
        assertThat(apiPage.getPublished()).isTrue();
    }

    @Test
    void should_unpublish_but_not_delete_api_documentation_when_removing_the_last_listing() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var listing = anApi(PAGE_ID, "API", null, "api");
        var apiPage = aPage(PAGE12_ID, "docs", null).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        navigation.initWith(List.of(listing, apiPage));

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(navigation.storage()).containsExactly(apiPage);
        assertThat(apiPage.getPublished()).isFalse();
    }

    @Test
    void should_refuse_deletion_when_a_foreign_item_is_stored_under_the_portal() {
        var target = PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target");
        portals.initWith(List.of(target));
        var folder = aFolder("folder");
        var apiPage = aPage(PAGE_ID, "docs", folder.getId()).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        navigation.initWith(List.of(folder, apiPage));

        assertThatThrownBy(() -> useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET))).isInstanceOf(
            ValidationDomainException.class
        );
        assertThat(portals.storage()).containsExactly(target);
        assertThat(navigation.storage()).containsExactly(folder, apiPage);
    }

    @Test
    void should_preserve_legacy_api_documentation_nested_below_listing() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var listing = anApi(PAGE_ID, "API", null, "api");
        var root = aFolder("docs", listing.getId()).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        var page = aPage(PAGE12_ID, "page", root.getId()).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        navigation.initWith(List.of(listing, root, page));

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(navigation.storage()).containsExactly(root, page);
        assertThat(root.getParentId()).isNull();
        assertThat(root.getRootId()).isEqualTo(root.getId());
        assertThat(page.getParentId()).isEqualTo(root.getId());
        assertThat(page.getRootId()).isEqualTo(root.getId());
    }

    @Test
    void should_reject_foreign_page_content_before_mutation() {
        var target = PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target");
        portals.initWith(List.of(target));
        var foreign = contents.createDefault(ORG_ID, "other-env", PortalPageContentType.GRAVITEE_MARKDOWN);
        var page = aPage(PAGE_ID, "page", null, foreign.getId());
        navigation.initWith(List.of(page));

        assertThatThrownBy(() -> useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET))).isInstanceOf(
            ValidationDomainException.class
        );
        assertThat(portals.storage()).containsExactly(target);
        assertThat(contents.storage()).containsExactly(foreign);
        assertThat(navigation.storage()).containsExactly(page);
    }

    @Test
    void should_reject_content_from_another_organization_without_mutation() {
        var target = PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target");
        portals.initWith(List.of(target));
        var foreign = contents.createDefault("other-org", ENV_ID, PortalPageContentType.GRAVITEE_MARKDOWN);
        var page = aPage(PAGE_ID, "page", null, foreign.getId());
        navigation.initWith(List.of(page));
        assertThatThrownBy(() -> useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET))).isInstanceOf(
            ValidationDomainException.class
        );
        assertThat(portals.storage()).containsExactly(target);
        assertThat(contents.storage()).containsExactly(foreign);
        assertThat(navigation.storage()).containsExactly(page);
    }

    @Test
    void should_keep_automation_content_referenced_by_another_portal() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var metadata = new AutomationMetadata(
            AutomationMetadata.ReferenceType.PORTAL,
            TARGET.toString(),
            null,
            Optional.empty(),
            Optional.empty()
        );
        var content = new GraviteeMarkdownPageContent(PortalPageContentId.random(), ORG_ID, ENV_ID, GraviteeMarkdown.of("body"), metadata);
        contents.create(content);
        var sharedPage = aPage(PAGE_ID, "shared", null, content.getId())
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(OTHER))
            .build();
        navigation.initWith(List.of(sharedPage));
        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));
        assertThat(contents.storage()).containsExactly(content);
        assertThat(navigation.storage()).containsExactly(sharedPage);
    }

    @Test
    void should_delete_portal_automation_records_even_without_navigation_items() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var listing = PortalListing.of(PortalListingId.random(), ENV_ID, ORG_ID, TARGET, List.of());
        var otherListing = PortalListing.of(PortalListingId.random(), ENV_ID, ORG_ID, OTHER, List.of());
        listings.initWith(List.of(listing, otherListing));
        var metadata = new AutomationMetadata(
            AutomationMetadata.ReferenceType.PORTAL,
            TARGET.toString(),
            null,
            Optional.empty(),
            Optional.empty()
        );
        var content = new GraviteeMarkdownPageContent(PortalPageContentId.random(), ORG_ID, ENV_ID, GraviteeMarkdown.of("body"), metadata);
        contents.create(content);

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(listings.storage()).containsExactly(otherListing);
        assertThat(contents.storage()).isEmpty();
    }

    @Test
    void should_keep_api_owned_content_even_if_its_only_navigation_page_is_on_deleted_portal() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var metadata = new AutomationMetadata(AutomationMetadata.ReferenceType.API, "api", null, Optional.empty(), Optional.empty());
        var content = new GraviteeMarkdownPageContent(PortalPageContentId.random(), ORG_ID, ENV_ID, GraviteeMarkdown.of("body"), metadata);
        contents.create(content);
        var page = aPage(PAGE_ID, "page", null, content.getId());
        navigation.initWith(List.of(page));

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(contents.storage()).containsExactly(content);
        assertThat(navigation.storage()).isEmpty();
    }
}
