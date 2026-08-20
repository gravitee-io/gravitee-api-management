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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationSyncDomainServiceTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String API_ID = "11111111-1111-1111-1111-111111111111";
    private static final PortalPageContentId DOC_ID = PortalPageContentId.of("22222222-2222-2222-2222-222222222222");

    private final PortalNavigationItemsCrudServiceInMemory navItemCrud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navItemQuery = new PortalNavigationItemsQueryServiceInMemory(
        navItemCrud.storage()
    );
    private final PortalPageContentQueryServiceInMemory pageContentQuery = new PortalPageContentQueryServiceInMemory();

    private ApiDocumentationSyncDomainService syncService;
    private PortalNavigationItemValidatorService validatorService;

    @BeforeEach
    void setUp() {
        navItemCrud.reset();
        pageContentQuery.reset();
        validatorService = mock(PortalNavigationItemValidatorService.class);
        syncService = new ApiDocumentationSyncDomainService(navItemCrud, navItemQuery, validatorService);
    }

    @Test
    void materializes_a_nav_page_when_no_portal_lists_the_api() {
        var meta = new AutomationMetadata(
            AutomationMetadata.ReferenceType.API,
            API_ID,
            "Getting Started",
            Optional.empty(),
            Optional.of(1)
        );
        var doc = new GraviteeMarkdownPageContent(
            DOC_ID,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# Hello"),
            meta
        );

        syncService.materialize(AUDIT_INFO, doc);

        assertThat(navItemCrud.storage())
            .singleElement()
            .satisfies(page -> {
                assertThat(page.getId()).isEqualTo(PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, API_ID, DOC_ID));
                assertThat(page.isRoot()).isTrue();
                assertThat(page.getReference()).isEqualTo(new NavigationItemReference.ApiReference(API_ID));
            });
    }

    @Test
    void should_create_one_page_for_the_api_however_many_nav_api_rows_exist() {
        seedNavApi(PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        seedNavApi(PortalNavigationItemId.of("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

        syncService.materialize(AUDIT_INFO, aDocumentation());

        var pageId = PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, API_ID, DOC_ID);
        assertThat(
            navItemCrud.storage().stream().filter(PortalNavigationPage.class::isInstance).map(PortalNavigationItem::getId)
        ).containsExactly(pageId);

        var page = (PortalNavigationPage) navItemCrud
            .storage()
            .stream()
            .filter(item -> item.getId().equals(pageId))
            .findFirst()
            .orElseThrow();
        assertThat(page.getAutomationMetadata()).isNotNull();
        assertThat(page.getAutomationMetadata().referenceType()).isEqualTo(AutomationMetadata.ReferenceType.API);
        assertThat(page.getAutomationMetadata().referenceId()).isEqualTo(API_ID);
        assertThat(page.getAutomationMetadata().location()).isEqualTo(Optional.of("/getting-started"));
        // trimmed: name/order already live natively on the nav item as title/order
        assertThat(page.getAutomationMetadata().name()).isNull();
        assertThat(page.getAutomationMetadata().order()).isEmpty();
    }

    @Test
    void should_ignore_nav_api_rows_belonging_to_other_apis() {
        var ours = seedNavApi(PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        var theirs = seedNavApi(PortalNavigationItemId.of("cccccccc-cccc-cccc-cccc-cccccccccccc"), "another-api-id");

        syncService.materialize(AUDIT_INFO, aDocumentation());

        var pageIdOurs = PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, API_ID, DOC_ID);
        var pageIdTheirs = PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, "another-api-id", DOC_ID);
        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).contains(pageIdOurs).doesNotContain(pageIdTheirs);
    }

    @Test
    void materialize_invokes_nav_item_validator_on_create_path() {
        seedNavApi(PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        syncService.materialize(AUDIT_INFO, aDocumentation());

        verify(validatorService).validateOne(any(), eq(AUDIT_INFO.environmentId()));
    }

    @Test
    void materialize_invokes_nav_item_validator_on_update_path() {
        seedNavApi(PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        syncService.materialize(AUDIT_INFO, aDocumentation());

        syncService.materialize(AUDIT_INFO, aDocumentation());

        verify(validatorService).validateToUpdate(any(), any());
    }

    @Test
    void should_be_idempotent_when_materializing_twice() {
        seedNavApi(PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        syncService.materialize(AUDIT_INFO, aDocumentation());
        var afterFirst = navItemCrud.storage().size();
        syncService.materialize(AUDIT_INFO, aDocumentation());

        assertThat(navItemCrud.storage()).hasSize(afterFirst);
    }

    @Test
    void should_remove_pages_on_dematerialize() {
        seedNavApi(PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        syncService.materialize(AUDIT_INFO, aDocumentation());

        syncService.dematerialize(AUDIT_INFO, API_ID, DOC_ID);

        var pageId = PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, API_ID, DOC_ID);
        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).doesNotContain(pageId);
    }

    @Test
    void dematerialize_is_idempotent_when_nothing_was_materialized() {
        seedNavApi(PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        syncService.dematerialize(AUDIT_INFO, API_ID, DOC_ID);

        // Only the seeded nav-api row remains; no page rows were ever created.
        assertThat(navItemCrud.storage()).hasSize(1);
    }

    @Test
    void cleanupForApi_removes_child_pages_but_keeps_nav_api_rows() {
        var navApiIdA = PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var navApiIdB = PortalNavigationItemId.of("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        seedNavApi(navApiIdA);
        seedNavApi(navApiIdB);
        var doc = aDocumentation();
        pageContentQuery.initWith(java.util.List.of(doc));
        syncService.materialize(AUDIT_INFO, doc);

        syncService.cleanupForApi(AUDIT_INFO, API_ID);

        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).containsExactlyInAnyOrder(navApiIdA, navApiIdB);
    }

    @Test
    void cleanupForApi_also_removes_the_apis_folder_subtree() {
        var folderId = PortalNavigationItemId.forApiFolder(AUDIT_INFO, API_ID, "/guides");
        var folder = PortalNavigationFolder.builder()
            .id(folderId)
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("guides")
            .segment("guides")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .reference(new NavigationItemReference.ApiReference(API_ID))
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        navItemCrud.create(folder);

        syncService.cleanupForApi(AUDIT_INFO, API_ID);

        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).doesNotContain(folderId);
    }

    @Test
    void cleanupForApi_leaves_other_api_rows_untouched() {
        var ownNavApiId = PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var otherApiNavId = PortalNavigationItemId.of("cccccccc-cccc-cccc-cccc-cccccccccccc");
        seedNavApi(ownNavApiId);
        seedNavApi(otherApiNavId, "another-api-id");

        syncService.cleanupForApi(AUDIT_INFO, API_ID);

        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).containsExactlyInAnyOrder(ownNavApiId, otherApiNavId);
    }

    @Test
    void cleanupNavApi_removes_only_the_row_and_leaves_the_apis_page_intact() {
        var keptNavApiId = PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var removedNavApiId = PortalNavigationItemId.of("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        seedNavApi(keptNavApiId);
        seedNavApi(removedNavApiId);
        var doc = aDocumentation();
        pageContentQuery.initWith(java.util.List.of(doc));
        syncService.materialize(AUDIT_INFO, doc);

        syncService.cleanupNavApi(AUDIT_INFO, removedNavApiId);

        // The API's page is not a descendant of the row — it belongs to the API — so removing one of two
        // nav-api rows takes only that row (I6).
        var pageId = PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, API_ID, DOC_ID);
        assertThat(navItemCrud.storage())
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(keptNavApiId, pageId)
            .doesNotContain(removedNavApiId);
    }

    private PortalNavigationApi seedNavApi(PortalNavigationItemId id) {
        return seedNavApi(id, API_ID);
    }

    private PortalNavigationApi seedNavApi(PortalNavigationItemId id, String apiId) {
        var create = CreatePortalNavigationItem.builder()
            .id(id)
            .title("api")
            .segment("api")
            .area(PortalArea.TOP_NAVBAR)
            .type(PortalNavigationItemType.API)
            .order(0)
            .apiId(apiId)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
        var navApi = (PortalNavigationApi) PortalNavigationItem.from(create, AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId(), null);
        navItemCrud.create(navApi);
        return navApi;
    }

    private static PortalPageContent<?> aDocumentation() {
        var meta = new AutomationMetadata(
            AutomationMetadata.ReferenceType.API,
            API_ID,
            "Getting Started",
            Optional.of("/getting-started"),
            Optional.of(1)
        );
        return new GraviteeMarkdownPageContent(
            DOC_ID,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# Hello"),
            meta
        );
    }
}
