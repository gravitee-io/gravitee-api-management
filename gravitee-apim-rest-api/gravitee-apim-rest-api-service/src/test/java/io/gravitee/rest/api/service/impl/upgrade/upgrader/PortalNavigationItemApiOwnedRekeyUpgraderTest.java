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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.api.search.PortalNavigationItemCriteria;
import io.gravitee.repository.management.model.AutomationMetadata;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemApiOwnedRekeyUpgraderTest {

    private static final String ORG = "org";
    private static final String ENV = "env";
    private static final String API_ID = "api-1";
    // apiLink()'s builder has no String-context overload — only NavigationBuilder got one, for
    // production code that never computes a link id at all (a legacy link's id never changes). Test
    // fixtures still need to derive it, so they use a real AuditInfo like every other navigation test.
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORG)
        .environmentId(ENV)
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private FakePortalNavigationItemRepository repository;
    private PortalNavigationItemApiOwnedRekeyUpgrader upgrader;

    @BeforeEach
    void setUp() {
        repository = new FakePortalNavigationItemRepository();
        upgrader = new PortalNavigationItemApiOwnedRekeyUpgrader(repository);
    }

    @Test
    void re_parents_a_legacy_api_link_without_changing_its_id() throws UpgraderException {
        var linkId = HRIDToUUID.apiLink().context(AUDIT_INFO).api(API_ID).hrid("external-docs").id();
        var link = PortalNavigationItem.builder()
            .id(linkId)
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.LINK)
            .parentId("stale-parent-id")
            .rootId("stale-parent-id")
            // referenceType/referenceId left at their PORTAL/ZERO builder defaults, as a legacy row would have them.
            .automationMetadata(AutomationMetadata.builder().referenceType(AutomationTargetReferenceType.API).referenceId(API_ID).build())
            .build();
        repository.initWith(List.of(link));

        assertThat(upgrader.upgrade()).isTrue();

        var updated = repository.get(linkId);
        assertThat(updated.getParentId()).isNull();
        assertThat(updated.getRootId()).isEqualTo(linkId);
        assertThat(updated.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(updated.getReferenceId()).isEqualTo(API_ID);
        assertThat(repository.storage()).hasSize(1);
    }

    @Test
    void leaves_a_hand_created_folder_under_the_nav_api_row_untouched() throws UpgraderException {
        var navApiRowId = "nav-api-row-1";
        var apiRow = apiRow(navApiRowId, API_ID);
        var handCreated = PortalNavigationItem.builder()
            .id("hand-created-folder")
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.FOLDER)
            .parentId(navApiRowId)
            .segment("notes")
            .rootId("hand-created-folder")
            .build();
        repository.initWith(List.of(apiRow, handCreated));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.storage()).containsExactlyInAnyOrder(apiRow, handCreated);
    }

    @Test
    void a_second_run_changes_nothing() throws UpgraderException {
        var navApiRowId = "nav-api-row-1";
        var apiRow = apiRow(navApiRowId, API_ID);

        var folderNewId = newFolderId(API_ID, "/guides");
        var folder = PortalNavigationItem.builder()
            .id(folderNewId)
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.FOLDER)
            .segment("guides")
            .rootId(folderNewId)
            .referenceType(PortalNavigationReferenceType.API)
            .referenceId(API_ID)
            .build();

        var contentId = "content-1";
        var pageNewId = HRIDToUUID.navigation().context(ORG, ENV).api(API_ID).documentation(contentId).id();
        var page = PortalNavigationItem.builder()
            .id(pageNewId)
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.PAGE)
            .parentId(folderNewId)
            .rootId(folderNewId)
            .referenceType(PortalNavigationReferenceType.API)
            .referenceId(API_ID)
            .configuration("{\"portalPageContentId\":\"" + contentId + "\"}")
            .automationMetadata(
                AutomationMetadata.builder()
                    .referenceType(AutomationTargetReferenceType.API)
                    .referenceId(API_ID)
                    .location("/guides")
                    .build()
            )
            .build();

        var linkId = HRIDToUUID.apiLink().context(AUDIT_INFO).api(API_ID).hrid("external-docs").id();
        var link = PortalNavigationItem.builder()
            .id(linkId)
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.LINK)
            .rootId(linkId)
            .referenceType(PortalNavigationReferenceType.API)
            .referenceId(API_ID)
            .automationMetadata(AutomationMetadata.builder().referenceType(AutomationTargetReferenceType.API).referenceId(API_ID).build())
            .build();

        repository.initWith(List.of(apiRow, folder, page, link));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.storage()).containsExactlyInAnyOrder(apiRow, folder, page, link);
        assertThat(repository.updateCount()).isZero();
    }

    @Test
    void realigns_a_legacy_root_folder_that_already_carries_its_api_owned_id() throws UpgraderException {
        var navApiRowId = "nav-api-row-1";
        var folderId = newFolderId(API_ID, "/guides");
        repository.initWith(List.of(apiRow(navApiRowId, API_ID), legacyApiOwnedFolder("/guides", navApiRowId, navApiRowId)));

        assertThat(upgrader.upgrade()).isTrue();

        var folder = repository.get(folderId);
        assertThat(folder.getParentId()).isNull();
        assertThat(folder.getRootId()).isEqualTo(folderId);
        assertThat(folder.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(folder.getReferenceId()).isEqualTo(API_ID);
    }

    @Test
    void realigns_a_nested_legacy_folder_onto_the_subtree_root() throws UpgraderException {
        var navApiRowId = "nav-api-row-1";
        var guidesId = newFolderId(API_ID, "/guides");
        var advancedId = newFolderId(API_ID, "/guides/advanced");
        repository.initWith(
            List.of(
                apiRow(navApiRowId, API_ID),
                legacyApiOwnedFolder("/guides", navApiRowId, navApiRowId),
                legacyApiOwnedFolder("/guides/advanced", guidesId, navApiRowId)
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        var advanced = repository.get(advancedId);
        assertThat(advanced.getParentId()).isEqualTo(guidesId);
        assertThat(advanced.getRootId()).isEqualTo(guidesId);
        assertThat(advanced.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(advanced.getReferenceId()).isEqualTo(API_ID);
    }

    @Test
    void realigns_a_legacy_root_doc_page_off_the_nav_api_row() throws UpgraderException {
        var navApiRowId = "nav-api-row-1";
        var contentId = "content-1";
        var pageId = docPageId(contentId);
        repository.initWith(List.of(apiRow(navApiRowId, API_ID), legacyApiOwnedDocPage(contentId, null, navApiRowId, navApiRowId)));

        assertThat(upgrader.upgrade()).isTrue();

        var page = repository.get(pageId);
        assertThat(page.getParentId()).isNull();
        assertThat(page.getRootId()).isEqualTo(pageId);
        assertThat(page.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(page.getReferenceId()).isEqualTo(API_ID);
    }

    @Test
    void realigns_a_nested_legacy_doc_page_under_its_folder() throws UpgraderException {
        var navApiRowId = "nav-api-row-1";
        var contentId = "content-1";
        var guidesId = newFolderId(API_ID, "/guides");
        repository.initWith(
            List.of(
                apiRow(navApiRowId, API_ID),
                legacyApiOwnedFolder("/guides", navApiRowId, navApiRowId),
                legacyApiOwnedDocPage(contentId, "/guides", guidesId, navApiRowId)
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        var page = repository.get(docPageId(contentId));
        assertThat(page.getParentId()).isEqualTo(guidesId);
        assertThat(page.getRootId()).isEqualTo(guidesId);
        assertThat(page.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(page.getReferenceId()).isEqualTo(API_ID);
    }

    @Test
    void realigns_a_legacy_doc_page_onto_a_phantom_parent_when_its_folder_is_absent() throws UpgraderException {
        var contentId = "content-1";
        repository.initWith(List.of(legacyApiOwnedDocPage(contentId, "/missing", "nav-api-row-1", "nav-api-row-1")));

        assertThat(upgrader.upgrade()).isTrue();

        var phantomFolderId = newFolderId(API_ID, "/missing");
        var page = repository.get(docPageId(contentId));
        assertThat(page.getParentId()).isEqualTo(phantomFolderId);
        assertThat(page.getRootId()).isEqualTo(phantomFolderId);
        assertThat(page.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(page.getReferenceId()).isEqualTo(API_ID);
    }

    @Test
    void gives_a_deeply_nested_doc_page_the_subtree_root_not_its_immediate_folder() throws UpgraderException {
        var navApiRowId = "nav-api-row-1";
        var contentId = "content-1";
        var guidesId = newFolderId(API_ID, "/guides");
        var advancedId = newFolderId(API_ID, "/guides/advanced");
        repository.initWith(
            List.of(
                apiRow(navApiRowId, API_ID),
                legacyApiOwnedFolder("/guides", navApiRowId, navApiRowId),
                legacyApiOwnedFolder("/guides/advanced", guidesId, navApiRowId),
                legacyApiOwnedDocPage(contentId, "/guides/advanced", advancedId, navApiRowId)
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        var page = repository.get(docPageId(contentId));
        assertThat(page.getParentId()).isEqualTo(advancedId);
        assertThat(page.getRootId()).isEqualTo(guidesId);
    }

    @Test
    void realigns_the_single_folder_row_two_portals_listing_the_same_api_share() throws UpgraderException {
        // Both listings derive the same forApiFolder id, so there was only ever one folder row —
        // parented under whichever nav-api row reconciled last.
        var folderId = newFolderId(API_ID, "/guides");
        repository.initWith(
            List.of(
                apiRow("nav-api-row-a", API_ID),
                apiRow("nav-api-row-b", API_ID),
                legacyApiOwnedFolder("/guides", "nav-api-row-b", "nav-api-row-b")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.storage())
            .filteredOn(item -> item.getId().equals(folderId))
            .hasSize(1);
        var folder = repository.get(folderId);
        assertThat(folder.getParentId()).isNull();
        assertThat(folder.getRootId()).isEqualTo(folderId);
        assertThat(folder.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
    }

    /**
     * The real pre-migration shape: {@code forApiFolder} was always keyed on the API, never on the
     * nav-api row, so a legacy folder already sits at its API-owned id and only its parent, root and
     * reference are stale.
     */
    private static PortalNavigationItem legacyApiOwnedFolder(String path, String parentId, String navApiRowId) {
        return PortalNavigationItem.builder()
            .id(newFolderId(API_ID, path))
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.FOLDER)
            .segment(path.substring(path.lastIndexOf('/') + 1))
            .parentId(parentId)
            .rootId(navApiRowId)
            .build();
    }

    /** As {@link #legacyApiOwnedFolder}, for a doc page: {@code forApiDocumentation} was API-keyed too. */
    private static PortalNavigationItem legacyApiOwnedDocPage(String contentId, String location, String parentId, String navApiRowId) {
        return PortalNavigationItem.builder()
            .id(docPageId(contentId))
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.PAGE)
            .parentId(parentId)
            .rootId(navApiRowId)
            .configuration("{\"portalPageContentId\":\"" + contentId + "\"}")
            .automationMetadata(
                AutomationMetadata.builder().referenceType(AutomationTargetReferenceType.API).referenceId(API_ID).location(location).build()
            )
            .build();
    }

    private static String docPageId(String contentId) {
        return HRIDToUUID.navigation().context(ORG, ENV).api(API_ID).documentation(contentId).id();
    }

    private static PortalNavigationItem apiRow(String id, String apiId) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.API)
            .apiId(apiId)
            .rootId(id)
            .build();
    }

    private static String newFolderId(String apiId, String path) {
        return HRIDToUUID.navigation().context(ORG, ENV).api(apiId).folder(path).id();
    }

    private static class FakePortalNavigationItemRepository implements PortalNavigationItemRepository {

        private final List<PortalNavigationItem> items = new ArrayList<>();
        private int updateCount;

        void initWith(List<PortalNavigationItem> initial) {
            items.clear();
            items.addAll(initial);
            updateCount = 0;
        }

        List<PortalNavigationItem> storage() {
            return items;
        }

        int updateCount() {
            return updateCount;
        }

        PortalNavigationItem get(String id) {
            return findById(id).orElseThrow(() -> new AssertionError("No item at id " + id));
        }

        @Override
        public Set<PortalNavigationItem> findAll() {
            return Set.copyOf(items);
        }

        @Override
        public Optional<PortalNavigationItem> findById(String id) {
            return items
                .stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
        }

        @Override
        public PortalNavigationItem create(PortalNavigationItem item) {
            items.add(item);
            return item;
        }

        @Override
        public PortalNavigationItem update(PortalNavigationItem item) {
            updateCount++;
            items.removeIf(existing -> existing.getId().equals(item.getId()));
            items.add(item);
            return item;
        }

        @Override
        public void delete(String id) {
            items.removeIf(item -> item.getId().equals(id));
        }

        @Override
        public List<PortalNavigationItem> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId) {
            return List.of();
        }

        @Override
        public List<PortalNavigationItem> findByAutomationReference(
            String environmentId,
            AutomationTargetReferenceType referenceType,
            String referenceId
        ) {
            return List.of();
        }

        @Override
        public List<PortalNavigationItem> findAllByParentIdAndEnvironmentId(String parentId, String environmentId) {
            return List.of();
        }

        @Override
        public List<PortalNavigationItem> findAllByAreaAndEnvironmentIdAndParentIdIsNull(
            PortalNavigationItem.Area area,
            String environmentId
        ) {
            return List.of();
        }

        @Override
        public List<PortalNavigationItem> findAllTopLevelByAreaAndEnvironmentAndReference(
            PortalNavigationItem.Area area,
            String environmentId,
            PortalNavigationReferenceType referenceType,
            String referenceId
        ) {
            return List.of();
        }

        @Override
        public List<PortalNavigationItem> findAllByAreaAndEnvironmentId(PortalNavigationItem.Area area, String environmentId) {
            return List.of();
        }

        @Override
        public List<PortalNavigationItem> searchByCriteria(PortalNavigationItemCriteria criteria) {
            return List.of();
        }

        @Override
        public List<PortalNavigationItem> findAllByRootId(String rootId, String environmentId) {
            return List.of();
        }

        @Override
        public void deleteByIds(List<String> ids) {
            items.removeIf(item -> ids.contains(item.getId()));
        }

        @Override
        public void deleteByOrganizationId(String organizationId) {
            items.removeIf(item -> item.getOrganizationId().equals(organizationId));
        }

        @Override
        public void deleteByEnvironmentId(String environmentId) {
            items.removeIf(item -> item.getEnvironmentId().equals(environmentId));
        }
    }
}
