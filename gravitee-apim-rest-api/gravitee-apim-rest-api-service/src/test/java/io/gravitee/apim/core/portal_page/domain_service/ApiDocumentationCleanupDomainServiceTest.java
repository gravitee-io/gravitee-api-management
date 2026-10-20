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
package io.gravitee.apim.core.portal_page.domain_service;

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aLink;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static fixtures.core.model.PortalNavigationItemFixtures.anApiProduct;
import static fixtures.core.model.PortalPageContentFixtures.aGraviteeMarkdownPageContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ApiDocumentationCleanupDomainServiceTest {

    private static final String API_ID = "api-a";
    private static final String OTHER_API_ID = "api-b";
    private static final NavigationItemReference API_OWNER = new NavigationItemReference.ApiReference(API_ID);

    private final PortalNavigationItemsCrudServiceInMemory navigationCrud = spy(new PortalNavigationItemsCrudServiceInMemory());
    private final PortalNavigationItemsQueryServiceInMemory navigationQuery = spy(
        new PortalNavigationItemsQueryServiceInMemory(navigationCrud.storage())
    );
    private final PortalPageContentCrudServiceInMemory contentCrud = spy(new PortalPageContentCrudServiceInMemory());
    private final PortalPageContentQueryServiceInMemory contentQuery = spy(
        PortalPageContentQueryServiceInMemory.sharing(contentCrud.storage())
    );
    private final ApiDocumentationCleanupDomainService service = new ApiDocumentationCleanupDomainService(
        navigationQuery,
        navigationCrud,
        contentQuery,
        contentCrud
    );

    @Test
    void should_delete_manual_root_and_nested_documentation_and_content() {
        var folder = aFolder("Documentation").toBuilder().reference(API_OWNER).build();
        var nestedFolder = aFolder("Guides", folder.getId()).toBuilder().reference(API_OWNER).build();
        var overview = ownedPage("Overview", null);
        var nestedPage = ownedPage("Guide", nestedFolder.getId());
        var rootLink = aLink().toBuilder().reference(API_OWNER).build();
        var nestedLink = rootLink.toBuilder().id(PortalNavigationItemId.random()).parentId(folder.getId()).build();
        navigationCrud.initWith(List.of(folder, nestedFolder, overview, nestedPage, rootLink, nestedLink));
        contentCrud.initWith(List.of(contentFor(overview), contentFor(nestedPage)));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).isEmpty();
        assertThat(contentCrud.storage()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(PortalPageContentType.class)
    void should_delete_automated_documentation_and_its_content(PortalPageContentType type) {
        var metadata = automationMetadata(API_ID);
        var page = ownedPage("Imported documentation", null).toBuilder().automationMetadata(metadata).build();
        var content = PortalPageContent.of(type, page.getPortalPageContentId(), ORG_ID, ENV_ID, contentOfType(type), metadata);
        var link = aLink().toBuilder().reference(API_OWNER).automationMetadata(metadata).build();
        navigationCrud.initWith(List.of(page, link));
        contentCrud.initWith(List.of(content));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).isEmpty();
        assertThat(contentCrud.storage()).isEmpty();
        verify(contentCrud).delete(content.getId());
    }

    @Test
    void should_delete_api_automation_content_without_navigation() {
        var orphan = PortalPageContent.of(
            PortalPageContentType.GRAVITEE_MARKDOWN,
            PortalPageContentId.random(),
            ORG_ID,
            ENV_ID,
            "# Imported documentation",
            automationMetadata(API_ID)
        );
        var unrelated = aGraviteeMarkdownPageContent(PortalPageContentId.random(), ORG_ID, ENV_ID, "# Unrelated");
        contentCrud.initWith(List.of(orphan, unrelated));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(contentCrud.storage()).containsExactly(unrelated);
    }

    @Test
    void should_preserve_automation_content_used_by_a_page_that_changed_owner() {
        var survivor = aPage("Moved documentation", null);
        var content = PortalPageContent.of(
            PortalPageContentType.GRAVITEE_MARKDOWN,
            survivor.getPortalPageContentId(),
            ORG_ID,
            ENV_ID,
            "# Documentation",
            automationMetadata(API_ID)
        );
        navigationCrud.initWith(List.of(survivor));
        contentCrud.initWith(List.of(content));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).containsExactly(survivor);
        assertThat(contentCrud.storage()).containsExactly(content);
    }

    @Test
    void should_remove_disconnected_and_cyclic_owned_items_without_traversing_the_tree() {
        var missingParent = PortalNavigationItemId.random();
        var firstFolder = aFolder("First", missingParent).toBuilder().reference(API_OWNER).build();
        var secondFolder = aFolder("Second", firstFolder.getId()).toBuilder().reference(API_OWNER).build();
        firstFolder = firstFolder.toBuilder().parentId(secondFolder.getId()).build();
        var page = ownedPage("Orphan page", missingParent);
        var orphanFolder = aFolder("Orphan folder", missingParent).toBuilder().reference(API_OWNER).build();
        var link = aLink().toBuilder().reference(API_OWNER).parentId(missingParent).build();
        navigationCrud.initWith(List.of(firstFolder, secondFolder, page, orphanFolder, link));
        contentCrud.initWith(List.of(contentFor(page)));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).isEmpty();
        assertThat(contentCrud.storage()).isEmpty();
    }

    @Test
    void should_preserve_other_owners_environments_and_api_listings() {
        var folder = aFolder("Owned").toBuilder().reference(API_OWNER).build();
        var ownedPage = ownedPage("Owned page", folder.getId());
        var portalPage = aPage("Portal page", folder.getId());
        var otherApiPage = aPage("Other API page", folder.getId())
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(OTHER_API_ID))
            .build();
        var otherEnvironmentPage = ownedPage("Other environment", null).toBuilder().environmentId("other-env").build();
        var listing = anApi(PortalNavigationItemId.random().json(), "API listing", null, API_ID);
        var product = anApiProduct();
        var productPage = aPage("Product documentation", product.getId());
        var section = aFolder("APIs");
        navigationCrud.initWith(
            List.of(folder, ownedPage, portalPage, otherApiPage, otherEnvironmentPage, listing, product, productPage, section)
        );
        var portalContent = contentFor(portalPage);
        var otherApiContent = contentFor(otherApiPage);
        var otherEnvironmentContent = contentFor(otherEnvironmentPage);
        var productContent = contentFor(productPage);
        contentCrud.initWith(List.of(contentFor(ownedPage), portalContent, otherApiContent, otherEnvironmentContent, productContent));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).containsExactlyInAnyOrder(
            portalPage,
            otherApiPage,
            otherEnvironmentPage,
            listing,
            product,
            productPage,
            section
        );
        assertThat(contentCrud.storage()).containsExactlyInAnyOrder(
            portalContent,
            otherApiContent,
            otherEnvironmentContent,
            productContent
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { ENV_ID, "other-env" })
    void should_preserve_content_referenced_by_a_surviving_page(String survivingEnvironment) {
        var page = ownedPage("Owned page", null);
        var survivor = aPage("Shared page", null)
            .toBuilder()
            .environmentId(survivingEnvironment)
            .portalPageContentId(page.getPortalPageContentId())
            .build();
        var content = PortalPageContent.of(
            PortalPageContentType.GRAVITEE_MARKDOWN,
            page.getPortalPageContentId(),
            ORG_ID,
            ENV_ID,
            "# Shared",
            automationMetadata(API_ID)
        );
        navigationCrud.initWith(List.of(page, survivor));
        contentCrud.initWith(List.of(content));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).containsExactly(survivor);
        assertThat(contentCrud.storage()).containsExactly(content);
    }

    @Test
    void should_not_delete_content_from_another_environment() {
        var page = ownedPage("Inconsistent page", null);
        var foreignContent = aGraviteeMarkdownPageContent(page.getPortalPageContentId(), ORG_ID, "other-env", "# Foreign");
        navigationCrud.initWith(List.of(page));
        contentCrud.initWith(List.of(foreignContent));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).isEmpty();
        assertThat(contentCrud.storage()).containsExactly(foreignContent);
    }

    @Test
    void should_delete_content_once_when_multiple_deleted_pages_reference_it() {
        var first = ownedPage("First page", null);
        var second = ownedPage("Second page", null).toBuilder().portalPageContentId(first.getPortalPageContentId()).build();
        navigationCrud.initWith(List.of(first, second));
        contentCrud.initWith(List.of(contentFor(first)));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).isEmpty();
        assertThat(contentCrud.storage()).isEmpty();
        verify(contentCrud).delete(first.getPortalPageContentId());
    }

    @Test
    void should_succeed_when_content_is_already_missing_and_when_cleanup_is_repeated() {
        var page = ownedPage("Missing content", null);
        navigationCrud.initWith(List.of(page));

        assertThatCode(() -> {
            service.cleanupForApi(ENV_ID, API_ID);
            service.cleanupForApi(ENV_ID, API_ID);
        }).doesNotThrowAnyException();

        assertThat(navigationCrud.storage()).isEmpty();
        assertThat(contentCrud.storage()).isEmpty();
    }

    @Test
    void should_skip_global_page_search_when_only_folders_and_links_are_owned() {
        var folder = aFolder("Owned folder").toBuilder().reference(API_OWNER).build();
        var link = aLink().toBuilder().reference(API_OWNER).build();
        navigationCrud.initWith(List.of(folder, link));

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).isEmpty();
        verify(navigationQuery, never()).search(argThat(criteria -> criteria.getEnvironmentId() == null));
    }

    @Test
    void should_not_delete_any_data_when_reference_lookup_fails() {
        var page = ownedPage("Owned page", null);
        var content = contentFor(page);
        navigationCrud.initWith(List.of(page));
        contentCrud.initWith(List.of(content));
        var failure = new TechnicalDomainException("Unable to read page references");
        doThrow(failure).when(navigationQuery).search(argThat(criteria -> criteria.getEnvironmentId() == null));

        assertThatThrownBy(() -> service.cleanupForApi(ENV_ID, API_ID)).isSameAs(failure);

        assertThat(navigationCrud.storage()).containsExactly(page);
        assertThat(contentCrud.storage()).containsExactly(content);
        verify(contentCrud, never()).delete(any());
        verify(navigationCrud, never()).deleteByIds(anyList());
    }

    @Test
    void should_keep_navigation_until_content_deletion_succeeds_and_allow_retry() {
        var page = ownedPage("Owned page", null);
        var content = contentFor(page);
        navigationCrud.initWith(List.of(page));
        contentCrud.initWith(List.of(content));
        var failure = new TechnicalDomainException("Unable to delete page content");
        doThrow(failure).doCallRealMethod().when(contentCrud).delete(content.getId());

        assertThatThrownBy(() -> service.cleanupForApi(ENV_ID, API_ID)).isSameAs(failure);
        assertThat(navigationCrud.storage()).containsExactly(page);
        assertThat(contentCrud.storage()).containsExactly(content);

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).isEmpty();
        assertThat(contentCrud.storage()).isEmpty();
    }

    @Test
    void should_finish_reading_content_before_deleting_any_data() {
        var first = ownedPage("First page", null);
        var second = ownedPage("Second page", null);
        var firstContent = contentFor(first);
        var secondContent = contentFor(second);
        navigationCrud.initWith(List.of(first, second));
        contentCrud.initWith(List.of(firstContent, secondContent));
        var failure = new TechnicalDomainException("Unable to read page content");
        var reads = new AtomicInteger();
        doAnswer(invocation -> {
            if (reads.incrementAndGet() == 2) {
                throw failure;
            }
            return invocation.callRealMethod();
        })
            .when(contentQuery)
            .findById(any());

        assertThatThrownBy(() -> service.cleanupForApi(ENV_ID, API_ID)).isSameAs(failure);

        assertThat(navigationCrud.storage()).containsExactlyInAnyOrder(first, second);
        assertThat(contentCrud.storage()).containsExactlyInAnyOrder(firstContent, secondContent);
        verify(contentCrud, never()).delete(any());
        verify(navigationCrud, never()).deleteByIds(anyList());
    }

    @Test
    void should_allow_retry_after_navigation_deletion_partially_succeeds() {
        var first = ownedPage("First page", null);
        var second = ownedPage("Second page", null);
        navigationCrud.initWith(List.of(first, second));
        contentCrud.initWith(List.of(contentFor(first), contentFor(second)));
        var failure = new TechnicalDomainException("Unable to finish deleting navigation");
        doAnswer(invocation -> {
            assertThat(contentCrud.storage()).isEmpty();
            navigationCrud.delete(first.getId());
            throw failure;
        })
            .doCallRealMethod()
            .when(navigationCrud)
            .deleteByIds(anyList());

        assertThatThrownBy(() -> service.cleanupForApi(ENV_ID, API_ID)).isSameAs(failure);
        assertThat(navigationCrud.storage()).containsExactly(second);

        service.cleanupForApi(ENV_ID, API_ID);

        assertThat(navigationCrud.storage()).isEmpty();
        assertThat(contentCrud.storage()).isEmpty();
    }

    private static PortalNavigationPage ownedPage(String title, PortalNavigationItemId parentId) {
        return aPage(title, parentId).toBuilder().reference(API_OWNER).build();
    }

    private static PortalPageContent<?> contentFor(PortalNavigationPage page) {
        return aGraviteeMarkdownPageContent(
            page.getPortalPageContentId(),
            page.getOrganizationId(),
            page.getEnvironmentId(),
            "# Documentation"
        );
    }

    private static AutomationMetadata automationMetadata(String apiId) {
        return new AutomationMetadata(AutomationMetadata.ReferenceType.API, apiId, "documentation", Optional.empty(), Optional.empty());
    }

    private static String contentOfType(PortalPageContentType type) {
        return switch (type) {
            case GRAVITEE_MARKDOWN -> "# Documentation";
            case OPENAPI -> "openapi: 3.0.3\ninfo:\n  title: Test\n  version: '1.0'\npaths: {}";
            case ASYNCAPI -> "asyncapi: '3.0.0'\ninfo:\n  title: Test\n  version: '1.0'";
        };
    }
}
