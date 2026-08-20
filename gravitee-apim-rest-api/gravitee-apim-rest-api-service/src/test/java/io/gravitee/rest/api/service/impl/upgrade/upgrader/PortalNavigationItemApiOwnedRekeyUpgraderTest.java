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
    // apiLink()'s builder has no String-context overload — that overload exists only on
    // NavigationBuilder, for production code that never computes a link id at all (a legacy link's id
    // never changes). Test fixtures still need to derive it, so they use a real AuditInfo like every
    // other navigation test.
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
    void rekeys_a_two_level_legacy_folder_tree_and_keeps_its_paths() throws UpgraderException {
        var navApiRowId = "nav-api-row-1";
        var apiRow = apiRow(navApiRowId, API_ID);
        var guides = legacyFolder(navApiRowId, "/guides");
        guides.setParentId(navApiRowId);
        var advanced = legacyFolder(navApiRowId, "/guides/advanced");
        advanced.setParentId(guides.getId());
        repository.initWith(List.of(apiRow, guides, advanced));

        assertThat(upgrader.upgrade()).isTrue();

        var newGuidesId = newFolderId(API_ID, "/guides");
        var newAdvancedId = newFolderId(API_ID, "/guides/advanced");

        var newGuides = repository.get(newGuidesId);
        assertThat(newGuides.getParentId()).isNull();
        assertThat(newGuides.getRootId()).isEqualTo(newGuidesId);
        assertThat(newGuides.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(newGuides.getReferenceId()).isEqualTo(API_ID);

        var newAdvanced = repository.get(newAdvancedId);
        assertThat(newAdvanced.getParentId()).isEqualTo(newGuidesId);
        assertThat(newAdvanced.getRootId()).isEqualTo(newGuidesId);
        assertThat(newAdvanced.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);

        assertThat(repository.findById(guides.getId())).isEmpty();
        assertThat(repository.findById(advanced.getId())).isEmpty();
    }

    @Test
    void rekeys_a_legacy_doc_page_onto_the_api_and_keeps_its_phantom_parent_when_the_folder_is_absent() throws UpgraderException {
        var contentId = "content-1";
        var page = PortalNavigationItem.builder()
            .id("old-page-id")
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.PAGE)
            .parentId("some-legacy-parent-placeholder")
            .rootId("old-page-id")
            .configuration("{\"portalPageContentId\":\"" + contentId + "\"}")
            .automationMetadata(
                AutomationMetadata.builder()
                    .referenceType(AutomationTargetReferenceType.API)
                    .referenceId(API_ID)
                    .location("/missing")
                    .build()
            )
            .build();
        repository.initWith(List.of(page));

        assertThat(upgrader.upgrade()).isTrue();

        var newPageId = HRIDToUUID.navigation().context(ORG, ENV).api(API_ID).documentation(contentId).id();
        var phantomFolderId = newFolderId(API_ID, "/missing");

        var newPage = repository.get(newPageId);
        assertThat(newPage.getParentId()).isEqualTo(phantomFolderId);
        assertThat(newPage.getRootId()).isEqualTo(phantomFolderId);
        assertThat(newPage.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(newPage.getReferenceId()).isEqualTo(API_ID);

        assertThat(repository.findById("old-page-id")).isEmpty();
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
    void two_portals_listing_the_same_api_converge_on_one_subtree() throws UpgraderException {
        var apiRowA = apiRow("nav-api-row-a", API_ID);
        var apiRowB = apiRow("nav-api-row-b", API_ID);
        var guidesA = legacyFolder("nav-api-row-a", "/guides");
        guidesA.setParentId("nav-api-row-a");
        var guidesB = legacyFolder("nav-api-row-b", "/guides");
        guidesB.setParentId("nav-api-row-b");
        repository.initWith(List.of(apiRowA, apiRowB, guidesA, guidesB));

        assertThat(upgrader.upgrade()).isTrue();

        var newGuidesId = newFolderId(API_ID, "/guides");

        assertThat(repository.storage())
            .filteredOn(item -> item.getId().equals(newGuidesId))
            .hasSize(1);
        assertThat(repository.findById(guidesA.getId())).isEmpty();
        assertThat(repository.findById(guidesB.getId())).isEmpty();
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

    private static PortalNavigationItem legacyFolder(String navApiRowId, String path) {
        var segment = path.substring(path.lastIndexOf('/') + 1);
        var id = HRIDToUUID.navigation().context(ORG, ENV).api(navApiRowId).folder(path).id();
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.FOLDER)
            .segment(segment)
            .rootId(navApiRowId)
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
