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

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.service.common.HRIDToUUID;
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

    private PortalDocumentationSyncDomainService syncService;

    @BeforeEach
    void setUp() {
        navItemCrud.reset();
        syncService = new PortalDocumentationSyncDomainService(navItemCrud, navItemQuery);
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
