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
package io.gravitee.apim.core.portal.domain_service.navigation.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NavigationSyncPlannerTest {

    @Test
    void expand_to_full_paths_unfolds_nested_segments() {
        var managed = NavigationSyncPlanner.expandToFullPaths(List.of(new NavigationPath("/a/b/c", null)));

        assertThat(managed).containsExactlyInAnyOrder("/a", "/a/b", "/a/b/c");
    }

    @Test
    void conflict_check_throws_when_existing_folder_has_a_foreign_id() {
        var foreignId = PortalNavigationItemId.of("ffffffff-ffff-ffff-ffff-ffffffffffff");
        var managedId = PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var foreignFolderAtA = folder(foreignId.toString(), "a", null);
        var ownership = new NavigationOwnership(Set.of("/a"), path -> "/a".equals(path) ? managedId : null, Set.of(), Set.of());

        assertThatThrownBy(() ->
            NavigationSyncPlanner.plan(
                List.of(new NavigationPath("/a", null)),
                List.of(foreignFolderAtA),
                List.of(new NavigationPath("/a", null)),
                ownership
            )
        )
            .isInstanceOf(PathConflictException.class)
            .hasMessageContaining("/a");
    }

    @Test
    void conflict_check_passes_when_existing_folder_id_matches_expected() {
        var managedId = PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var managedFolder = folder(managedId.toString(), "a", null);
        var ownership = new NavigationOwnership(Set.of("/a"), path -> "/a".equals(path) ? managedId : null, Set.of(), Set.of());

        var plan = NavigationSyncPlanner.plan(
            List.of(new NavigationPath("/a", null)),
            List.of(managedFolder),
            List.of(new NavigationPath("/a", null)),
            ownership
        );

        assertThat(plan.actions()).hasSize(1);
    }

    private static PortalNavigationItem folder(String id, String segment, String parentId) {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.of(id))
            .organizationId("organization-id")
            .environmentId("environment-id")
            .title(segment)
            .segment(segment)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(parentId == null ? null : PortalNavigationItemId.of(parentId))
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
