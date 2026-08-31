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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.reconciliation.HomepageReconciler;
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalDocumentationSyncDomainServiceTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final PortalId PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalPageContentId DOC_ID = PortalPageContentId.of("22222222-2222-2222-2222-222222222222");

    private final PortalNavigationItemsCrudServiceInMemory navItemCrud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navItemQuery = new PortalNavigationItemsQueryServiceInMemory(
        navItemCrud.storage()
    );
    private final PortalPageContentCrudServiceInMemory pageContentCrud = new PortalPageContentCrudServiceInMemory();

    private PortalDocumentationSyncDomainService syncService;
    private PortalNavigationItemValidatorService validatorService;

    @BeforeEach
    void setUp() {
        navItemCrud.reset();
        pageContentCrud.reset();
        validatorService = mock(PortalNavigationItemValidatorService.class);
        syncService = new PortalDocumentationSyncDomainService(
            navItemCrud,
            navItemQuery,
            new HomepageReconciler(navItemQuery, navItemCrud, pageContentCrud),
            validatorService
        );
    }

    @Test
    void materialize_creates_nav_page_with_deterministic_id() {
        syncService.materialize(AUDIT_INFO, markdownDoc("Getting Started", "/projects/alpha", 1));

        assertThat(navItemCrud.storage()).hasSize(1);

        var page = (PortalNavigationPage) navItemCrud.storage().get(0);
        assertThat(page.getId()).isEqualTo(expectedNavItemId());
        assertThat(page.getTitle()).isEqualTo("Getting Started");
        assertThat(page.getSegment()).isEqualTo("getting-started");
        assertThat(page.getOrder()).isEqualTo(1);
        assertThat(page.getPortalPageContentId()).isEqualTo(DOC_ID);
        assertThat(page.getParentId()).isEqualTo(expectedFolderId("/projects/alpha"));
        assertThat(page.getAutomationMetadata()).isNotNull();
        assertThat(page.getAutomationMetadata().referenceType()).isEqualTo(AutomationMetadata.ReferenceType.PORTAL);
        assertThat(page.getAutomationMetadata().referenceId()).isEqualTo(PORTAL_ID.toString());
        assertThat(page.getAutomationMetadata().location()).isEqualTo(Optional.of("/projects/alpha"));
        // trimmed: name/order already live natively on the nav item as title/order
        assertThat(page.getAutomationMetadata().name()).isNull();
        assertThat(page.getAutomationMetadata().order()).isEmpty();
    }

    @Test
    void materialize_invokes_nav_item_validator_on_create_path() {
        syncService.materialize(AUDIT_INFO, markdownDoc("Getting Started", "/projects/alpha", 1));

        verify(validatorService).validateOne(any(), eq(AUDIT_INFO.environmentId()));
    }

    @Test
    void materialize_invokes_nav_item_validator_on_update_path() {
        syncService.materialize(AUDIT_INFO, markdownDoc("Getting Started", "/projects/alpha", 1));

        syncService.materialize(AUDIT_INFO, markdownDoc("Renamed", "/projects/alpha", 2));

        verify(validatorService).validateToUpdate(any(), any());
    }

    @Test
    void materialize_is_idempotent() {
        var doc = markdownDoc("Getting Started", "/projects/alpha", 1);

        syncService.materialize(AUDIT_INFO, doc);
        syncService.materialize(AUDIT_INFO, doc);

        assertThat(navItemCrud.storage()).hasSize(1);
    }

    @Test
    void materialize_updates_nav_page_when_doc_changes() {
        syncService.materialize(AUDIT_INFO, markdownDoc("Getting Started", "/projects/alpha", 1));

        syncService.materialize(AUDIT_INFO, markdownDoc("Renamed", "/projects/beta", 2));

        var page = (PortalNavigationPage) navItemCrud.storage().get(0);
        assertThat(page.getTitle()).isEqualTo("Renamed");
        assertThat(page.getSegment()).isEqualTo("renamed");
        assertThat(page.getOrder()).isEqualTo(2);
        assertThat(page.getParentId()).isEqualTo(expectedFolderId("/projects/beta"));
        assertThat(page.getAutomationMetadata()).isNotNull();
        assertThat(page.getAutomationMetadata().location()).isEqualTo(Optional.of("/projects/beta"));
    }

    @Test
    void dematerialize_removes_nav_page() {
        syncService.materialize(AUDIT_INFO, markdownDoc("Getting Started", "/projects/alpha", 1));

        syncService.dematerialize(AUDIT_INFO, PORTAL_ID.toString(), DOC_ID);

        assertThat(navItemCrud.storage()).isEmpty();
    }

    @Test
    void dematerialize_is_idempotent_when_nothing_materialized() {
        syncService.dematerialize(AUDIT_INFO, PORTAL_ID.toString(), DOC_ID);

        assertThat(navItemCrud.storage()).isEmpty();
    }

    @Test
    void materialize_with_null_location_marks_page_as_root() {
        syncService.materialize(AUDIT_INFO, markdownDoc("Getting Started", null, 1));

        var page = (PortalNavigationPage) navItemCrud.storage().get(0);
        assertThat(page.getParentId()).isNull();
        assertThat(page.getRootId()).isEqualTo(page.getId());
    }

    @Test
    void materialize_inherits_private_visibility_from_persisted_parent_folder() {
        var parentFolderId = expectedFolderId("/private-guides");
        navItemCrud.initWith(
            List.of(
                PortalNavigationFolder.builder()
                    .id(parentFolderId)
                    .organizationId(AUDIT_INFO.organizationId())
                    .environmentId(AUDIT_INFO.environmentId())
                    .title("private-guides")
                    .segment("private-guides")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .published(true)
                    .visibility(PortalVisibility.PRIVATE)
                    .build()
            )
        );

        syncService.materialize(AUDIT_INFO, markdownDoc("Setup", "/private-guides", 0));

        var page = (PortalNavigationPage) navItemCrud
            .storage()
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .findFirst()
            .orElseThrow();
        assertThat(page.getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
    }

    @Test
    void materialize_points_at_deterministic_folder_id_even_when_folder_missing() {
        syncService.materialize(AUDIT_INFO, markdownDoc("Getting Started", "/unknown", 1));

        var page = (PortalNavigationPage) navItemCrud.storage().get(0);
        assertThat(page.getParentId()).isEqualTo(expectedFolderId("/unknown"));
    }

    @Test
    void materialize_uses_zero_when_order_is_null() {
        syncService.materialize(AUDIT_INFO, markdownDoc("Getting Started", "/projects/alpha", null));

        var page = (PortalNavigationPage) navItemCrud.storage().get(0);
        assertThat(page.getOrder()).isZero();
    }

    @Test
    void materialize_creates_homepage_page_when_area_is_homepage() {
        syncService.materialize(AUDIT_INFO, homepageDoc("Home", "/homepage"), PortalArea.HOMEPAGE);

        assertThat(navItemCrud.storage()).hasSize(1);
        var page = (PortalNavigationPage) navItemCrud.storage().get(0);
        assertThat(page.getArea()).isEqualTo(PortalArea.HOMEPAGE);
        assertThat(page.getReference()).isEqualTo(new NavigationItemReference.PortalReference(PortalId.of(PORTAL_ID.toString())));
    }

    @Test
    void materialize_replaces_stale_homepage_with_different_id_for_same_portal() {
        var stale = staleHomepagePage("stale-content-id");
        navItemCrud.create(stale);
        pageContentCrud.create(staleContent(stale));

        syncService.materialize(AUDIT_INFO, homepageDoc("Home", "/homepage"), PortalArea.HOMEPAGE);

        assertThat(navItemCrud.storage()).hasSize(1);
        assertThat(navItemCrud.storage().get(0).getId()).isEqualTo(expectedNavItemId());
        assertThat(pageContentCrud.storage()).noneMatch(c ->
            c.getId().equals(PortalPageContentId.of("00000000-0000-0000-0000-00000000c0de"))
        );
    }

    @Test
    void materialize_replaces_unattached_sentinel_homepage() {
        var seeded = unattachedHomepagePage("seeded-content-id");
        navItemCrud.create(seeded);
        pageContentCrud.create(staleContent(seeded));

        syncService.materialize(AUDIT_INFO, homepageDoc("Home", "/homepage"), PortalArea.HOMEPAGE);

        assertThat(navItemCrud.storage()).hasSize(1);
        assertThat(navItemCrud.storage().get(0).getReference()).isEqualTo(
            new NavigationItemReference.PortalReference(PortalId.of(PORTAL_ID.toString()))
        );
    }

    @Test
    void materialize_rejects_second_automation_owned_homepage_for_same_portal() {
        var syncWithRealValidator = new PortalDocumentationSyncDomainService(
            navItemCrud,
            navItemQuery,
            new HomepageReconciler(navItemQuery, navItemCrud, pageContentCrud),
            new PortalNavigationItemValidatorService(
                navItemQuery,
                new PortalPageContentQueryServiceInMemory(pageContentCrud.storage()),
                new ApiProductQueryServiceInMemory(),
                new PortalNavigationItemSourceDomainServiceInMemory()
            )
        );
        var existing = automationOwnedHomepagePage();
        navItemCrud.create(existing);
        pageContentCrud.create(staleContent(existing));

        assertThatThrownBy(() ->
            syncWithRealValidator.materialize(AUDIT_INFO, homepageDoc("Home", "/homepage"), PortalArea.HOMEPAGE)
        ).isInstanceOf(HomepageAlreadyExistsException.class);

        assertThat(navItemCrud.storage()).hasSize(1);
        assertThat(navItemCrud.storage().get(0).getId()).isEqualTo(existing.getId());
    }

    @Test
    void materialize_does_not_touch_homepage_of_a_different_portal() {
        var otherPortalId = "00000000-0000-0000-0000-0000000000ff";
        var otherHomepage = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .reference(new NavigationItemReference.PortalReference(PortalId.of(otherPortalId)))
            .title("Other")
            .segment("other")
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .published(true)
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .build();
        navItemCrud.create(otherHomepage);

        syncService.materialize(AUDIT_INFO, homepageDoc("Home", "/homepage"), PortalArea.HOMEPAGE);

        assertThat(navItemCrud.storage()).hasSize(2);
    }

    private static PortalPageContent<?> homepageDoc(String name, String location) {
        return new GraviteeMarkdownPageContent(
            DOC_ID,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# Hello"),
            new AutomationMetadata(
                AutomationMetadata.ReferenceType.PORTAL,
                PORTAL_ID.toString(),
                name,
                Optional.ofNullable(location),
                Optional.of(0)
            )
        );
    }

    private static PortalNavigationPage staleHomepagePage(String contentId) {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-00000000c0d1"))
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .reference(new NavigationItemReference.PortalReference(PortalId.of(PORTAL_ID.toString())))
            .title("Stale")
            .segment("stale")
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .portalPageContentId(PortalPageContentId.of("00000000-0000-0000-0000-00000000c0de"))
            .published(true)
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .build();
    }

    private static PortalNavigationPage automationOwnedHomepagePage() {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-00000000c0d3"))
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .reference(new NavigationItemReference.PortalReference(PortalId.of(PORTAL_ID.toString())))
            .title("Existing")
            .segment("existing")
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .portalPageContentId(PortalPageContentId.of("00000000-0000-0000-0000-00000000c0da"))
            .published(true)
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .automationMetadata(
                new AutomationMetadata(
                    AutomationMetadata.ReferenceType.PORTAL,
                    PORTAL_ID.toString(),
                    null,
                    Optional.empty(),
                    Optional.empty()
                )
            )
            .build();
    }

    private static PortalNavigationPage unattachedHomepagePage(String contentId) {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-00000000c0d2"))
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .reference(NavigationItemReference.DEFAULT)
            .title("Seeded")
            .segment("seeded")
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .portalPageContentId(PortalPageContentId.of("00000000-0000-0000-0000-00000000c0df"))
            .published(true)
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .build();
    }

    private static GraviteeMarkdownPageContent staleContent(PortalNavigationPage page) {
        return new GraviteeMarkdownPageContent(
            page.getPortalPageContentId(),
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("stale")
        );
    }

    private static PortalPageContent<?> markdownDoc(String name, String location, Integer order) {
        return new GraviteeMarkdownPageContent(
            DOC_ID,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# Hello"),
            new AutomationMetadata(
                AutomationMetadata.ReferenceType.PORTAL,
                PORTAL_ID.toString(),
                name,
                Optional.ofNullable(location),
                Optional.ofNullable(order)
            )
        );
    }

    private static PortalNavigationItemId expectedNavItemId() {
        return PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).documentation(DOC_ID.toString()).id()
        );
    }

    private static PortalNavigationItemId expectedFolderId(String path) {
        return PortalNavigationItemId.of(HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).folder(path).id());
    }
}
