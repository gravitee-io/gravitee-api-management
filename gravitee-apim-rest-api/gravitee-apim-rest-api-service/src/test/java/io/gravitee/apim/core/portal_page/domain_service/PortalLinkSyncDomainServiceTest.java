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

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalLinkSyncDomainServiceTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_ID = "11111111-1111-1111-1111-111111111111";

    private final PortalNavigationItemsCrudServiceInMemory navItemCrud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navItemQuery = new PortalNavigationItemsQueryServiceInMemory(
        navItemCrud.storage()
    );

    private PortalLinkSyncDomainService syncService;

    @BeforeEach
    void setUp() {
        navItemCrud.reset();
        syncService = new PortalLinkSyncDomainService(navItemCrud, navItemQuery);
    }

    @Test
    void materialize_creates_a_link_with_deterministic_id() {
        var link = syncService.materialize(
            AUDIT_INFO,
            PORTAL_ID,
            "external-docs",
            "External Docs",
            "https://docs.example.com",
            "/projects/alpha",
            3
        );

        assertThat(navItemCrud.storage()).hasSize(1);
        assertThat(link.getId()).isEqualTo(expectedLinkId());
        assertThat(link.getTitle()).isEqualTo("External Docs");
        assertThat(link.getUrl()).isEqualTo("https://docs.example.com");
        assertThat(link.getOrder()).isEqualTo(3);
        assertThat(link.getParentId()).isEqualTo(expectedFolderId("/projects/alpha"));
        assertThat(link.getAutomationMetadata()).isNotNull();
        assertThat(link.getAutomationMetadata().referenceType()).isEqualTo(AutomationMetadata.ReferenceType.PORTAL);
        assertThat(link.getAutomationMetadata().referenceId()).isEqualTo(PORTAL_ID);
        assertThat(link.getAutomationMetadata().location()).isEqualTo(Optional.of("/projects/alpha"));
    }

    @Test
    void materialize_points_at_deterministic_folder_id_even_when_folder_missing() {
        var link = syncService.materialize(
            AUDIT_INFO,
            PORTAL_ID,
            "external-docs",
            "External Docs",
            "https://docs.example.com",
            "/unknown",
            1
        );

        assertThat(link.getParentId()).isEqualTo(expectedFolderId("/unknown"));
    }

    @Test
    void materialize_is_idempotent_on_reapply() {
        var first = syncService.materialize(AUDIT_INFO, PORTAL_ID, "external-docs", "External Docs", "https://docs.example.com", null, 1);
        var second = syncService.materialize(AUDIT_INFO, PORTAL_ID, "external-docs", "External Docs", "https://docs.example.com", null, 1);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(navItemCrud.storage()).hasSize(1);
    }

    @Test
    void materialize_updates_the_link_when_it_changes() {
        syncService.materialize(AUDIT_INFO, PORTAL_ID, "external-docs", "External Docs", "https://docs.example.com", "/projects/alpha", 1);

        var updated = syncService.materialize(
            AUDIT_INFO,
            PORTAL_ID,
            "external-docs",
            "Renamed",
            "https://renamed.example.com",
            "/projects/beta",
            2
        );

        assertThat(navItemCrud.storage()).hasSize(1);
        assertThat(updated.getTitle()).isEqualTo("Renamed");
        assertThat(updated.getUrl()).isEqualTo("https://renamed.example.com");
        assertThat(updated.getOrder()).isEqualTo(2);
        assertThat(updated.getParentId()).isEqualTo(expectedFolderId("/projects/beta"));
        assertThat(updated.getAutomationMetadata()).isNotNull();
        assertThat(updated.getAutomationMetadata().location()).isEqualTo(Optional.of("/projects/beta"));
    }

    @Test
    void materialize_rejects_a_segment_already_taken_by_a_foreign_item() {
        navItemCrud.initWith(
            List.of(
                PortalNavigationFolder.builder()
                    .id(PortalNavigationItemId.of("22222222-2222-2222-2222-222222222222"))
                    .organizationId(AUDIT_INFO.organizationId())
                    .environmentId(AUDIT_INFO.environmentId())
                    .title("external-docs")
                    .segment("external-docs")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .published(true)
                    .visibility(PortalVisibility.PUBLIC)
                    .build()
            )
        );

        assertThatThrownBy(() ->
            syncService.materialize(AUDIT_INFO, PORTAL_ID, "external-docs", "External Docs", "https://docs.example.com", null, 1)
        ).isInstanceOf(PathConflictException.class);
    }

    @Test
    void materialize_rejects_relocation_to_a_segment_already_taken_by_a_foreign_item() {
        syncService.materialize(AUDIT_INFO, PORTAL_ID, "external-docs", "External Docs", "https://docs.example.com", "/projects/alpha", 1);

        var squatter = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.of("33333333-3333-3333-3333-333333333333"))
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("external-docs")
            .segment("external-docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(expectedFolderId("/projects/beta"))
            .build();
        navItemCrud.create(squatter);

        assertThatThrownBy(() ->
            syncService.materialize(
                AUDIT_INFO,
                PORTAL_ID,
                "external-docs",
                "External Docs",
                "https://docs.example.com",
                "/projects/beta",
                1
            )
        ).isInstanceOf(PathConflictException.class);

        // the relocation was rejected before anything was written: the link is still at its
        // original location, and the squatter it collided with is untouched
        assertThat(navItemCrud.storage()).hasSize(2);
    }

    @Test
    void materialize_updating_a_link_without_moving_does_not_conflict_with_itself() {
        syncService.materialize(AUDIT_INFO, PORTAL_ID, "external-docs", "External Docs", "https://docs.example.com", "/projects/alpha", 1);

        var updated = syncService.materialize(
            AUDIT_INFO,
            PORTAL_ID,
            "external-docs",
            "Renamed",
            "https://renamed.example.com",
            "/projects/alpha",
            2
        );

        assertThat(navItemCrud.storage()).hasSize(1);
        assertThat(updated.getTitle()).isEqualTo("Renamed");
    }

    @Test
    void dematerialize_removes_the_link() {
        syncService.materialize(AUDIT_INFO, PORTAL_ID, "external-docs", "External Docs", "https://docs.example.com", null, 1);

        syncService.dematerialize(AUDIT_INFO, expectedLinkId());

        assertThat(navItemCrud.storage()).isEmpty();
    }

    @Test
    void dematerialize_is_idempotent_when_nothing_materialized() {
        syncService.dematerialize(AUDIT_INFO, expectedLinkId());

        assertThat(navItemCrud.storage()).isEmpty();
    }

    private static PortalNavigationItemId expectedLinkId() {
        return PortalNavigationItemId.of(HRIDToUUID.portalLink().context(AUDIT_INFO).portal(PORTAL_ID).hrid("external-docs").id());
    }

    private static PortalNavigationItemId expectedFolderId(String path) {
        return PortalNavigationItemId.forPortalFolder(AUDIT_INFO, PORTAL_ID, path);
    }
}
