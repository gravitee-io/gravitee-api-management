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

import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiNavigationSubtreePathsTest {

    private static final String ORG = "org";
    private static final String ENV = "env";
    private static final String NAV_API_ROW_ID = "11111111-1111-1111-1111-111111111111";
    private static final String API_ID = "55555555-5555-5555-5555-555555555555";

    @Test
    void collects_a_single_folder_directly_under_the_row() {
        var guides = automationFolder("/guides");

        var result = ApiNavigationSubtreePaths.collect(ORG, ENV, NAV_API_ROW_ID, API_ID, Map.of(NAV_API_ROW_ID, List.of(guides)));

        assertThat(result).extracting(ApiNavigationSubtreePaths.PathedFolder::path).containsExactly("/guides");
        assertThat(result)
            .extracting(f -> f.folder().getId())
            .containsExactly(guides.getId());
    }

    @Test
    void collects_a_folder_nested_two_deep() {
        var guides = automationFolder("/guides");
        var advanced = automationFolder("/guides/advanced");
        advanced.setParentId(guides.getId());

        var byParent = new HashMap<String, List<PortalNavigationItem>>();
        byParent.put(NAV_API_ROW_ID, List.of(guides));
        byParent.put(guides.getId(), List.of(advanced));

        var result = ApiNavigationSubtreePaths.collect(ORG, ENV, NAV_API_ROW_ID, API_ID, byParent);

        assertThat(result).extracting(ApiNavigationSubtreePaths.PathedFolder::path).containsExactly("/guides", "/guides/advanced");
    }

    @Test
    void does_not_claim_or_descend_into_a_hand_created_folder() {
        // Its id is not the deterministic automation id for "/notes" — an ordinary folder someone made by
        // hand happens to sit where automation would have written one. The child beneath it would only
        // be reachable if the walk (wrongly) descended anyway.
        var handCreated = PortalNavigationItem.builder()
            .id("22222222-2222-2222-2222-222222222222")
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.FOLDER)
            .parentId(NAV_API_ROW_ID)
            .segment("notes")
            .rootId("22222222-2222-2222-2222-222222222222")
            .build();
        var wouldBeChild = automationFolder("/notes/nested");
        wouldBeChild.setParentId(handCreated.getId());

        var byParent = new HashMap<String, List<PortalNavigationItem>>();
        byParent.put(NAV_API_ROW_ID, List.of(handCreated));
        byParent.put(handCreated.getId(), List.of(wouldBeChild));

        var result = ApiNavigationSubtreePaths.collect(ORG, ENV, NAV_API_ROW_ID, API_ID, byParent);

        assertThat(result).isEmpty();
    }

    @Test
    void gives_up_on_a_branch_with_a_blank_segment_instead_of_emitting_a_double_slash() {
        var blank = PortalNavigationItem.builder()
            .id("33333333-3333-3333-3333-333333333333")
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.FOLDER)
            .parentId(NAV_API_ROW_ID)
            .segment("")
            .rootId("33333333-3333-3333-3333-333333333333")
            .build();

        var result = ApiNavigationSubtreePaths.collect(ORG, ENV, NAV_API_ROW_ID, API_ID, Map.of(NAV_API_ROW_ID, List.of(blank)));

        assertThat(result).isEmpty();
    }

    @Test
    void ignores_non_folder_children() {
        var page = PortalNavigationItem.builder()
            .id("44444444-4444-4444-4444-444444444444")
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.PAGE)
            .parentId(NAV_API_ROW_ID)
            .segment("getting-started")
            .rootId("44444444-4444-4444-4444-444444444444")
            .build();

        var result = ApiNavigationSubtreePaths.collect(ORG, ENV, NAV_API_ROW_ID, API_ID, Map.of(NAV_API_ROW_ID, List.of(page)));

        assertThat(result).isEmpty();
    }

    @Test
    void terminates_on_a_cyclic_parent_chain_instead_of_recursing_forever() {
        // Malformed data: the folder is listed as its own child. The walk stops because the ownership
        // test is keyed on the path used to reach the folder, and "/loop/loop" is not "/loop".
        var loop = automationFolder("/loop");
        var byParent = new HashMap<String, List<PortalNavigationItem>>();
        byParent.put(NAV_API_ROW_ID, List.of(loop));
        byParent.put(loop.getId(), List.of(loop));

        var result = ApiNavigationSubtreePaths.collect(ORG, ENV, NAV_API_ROW_ID, API_ID, byParent);

        assertThat(result).extracting(ApiNavigationSubtreePaths.PathedFolder::path).containsExactly("/loop");
    }

    /** Keyed on the API, never on the nav-api row — the shape {@code forApiFolder} has always written. */
    private static PortalNavigationItem automationFolder(String path) {
        var segment = path.substring(path.lastIndexOf('/') + 1);
        var id = HRIDToUUID.navigation().context(ORG, ENV).api(API_ID).folder(path).id();
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.FOLDER)
            .segment(segment)
            .rootId(NAV_API_ROW_ID)
            .build();
    }
}
