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
package io.gravitee.apim.core.portal_page.query_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PortalNavigationItemsQueryServiceTest {

    @Test
    void should_search_only_requested_ids_in_the_environment_across_owners() {
        var first = PortalNavigationItemFixtures.aPage("Portal page", null);
        var second = PortalNavigationItemFixtures.aPage("API page", null);
        second.changeOwner(new NavigationItemReference.ApiReference("api-id"));
        var unrequested = PortalNavigationItemFixtures.aPage("Unrequested", null);
        var foreign = PortalNavigationItemFixtures.aPage("Other environment", null);
        foreign.setEnvironmentId("other-environment");
        var query = new PortalNavigationItemsQueryServiceInMemory(List.of(first, second, unrequested, foreign));
        var criteria = PortalNavigationItemQueryCriteria.builder()
            .environmentId(PortalNavigationItemFixtures.ENV_ID)
            .ids(Set.of(first.getId(), second.getId(), foreign.getId(), PortalNavigationItemId.random()))
            .build();

        assertThat(query.search(criteria)).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void should_return_no_items_when_navigation_ids_are_empty() {
        var query = new PortalNavigationItemsQueryServiceInMemory(List.of(PortalNavigationItemFixtures.aPage("Page", null)));
        var criteria = PortalNavigationItemQueryCriteria.builder().environmentId(PortalNavigationItemFixtures.ENV_ID).ids(Set.of()).build();

        assertThat(query.search(criteria)).isEmpty();
    }

    @Test
    void should_keep_environment_filter_when_navigation_ids_are_null() {
        var first = PortalNavigationItemFixtures.aPage("First", null);
        var second = PortalNavigationItemFixtures.aPage("Second", null);
        var foreign = PortalNavigationItemFixtures.aPage("Other environment", null);
        foreign.setEnvironmentId("other-environment");
        var query = new PortalNavigationItemsQueryServiceInMemory(List.of(first, second, foreign));
        var criteria = PortalNavigationItemQueryCriteria.builder().environmentId(PortalNavigationItemFixtures.ENV_ID).ids(null).build();

        assertThat(query.search(criteria)).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void should_return_every_page_associated_with_content_in_the_environment() {
        var contentId = PortalPageContentId.random();
        var first = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().json(), "First", null, contentId);
        var second = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().json(), "Second", null, contentId);
        second.changeOwner(new NavigationItemReference.ApiReference("api-id"));
        var differentContent = PortalNavigationItemFixtures.aPage("Different content", null);
        var differentEnvironment = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().json(), "Foreign", null, contentId);
        differentEnvironment.setEnvironmentId("other-environment");
        var query = new PortalNavigationItemsQueryServiceInMemory(List.of(first, second, differentContent, differentEnvironment));

        assertThat(query.findNavigationPagesByPortalPageContentId(PortalNavigationItemFixtures.ENV_ID, contentId)).containsExactly(
            first,
            second
        );
        assertThat(query.findNavigationPageByPortalPageContentId(PortalNavigationItemFixtures.ENV_ID, contentId)).contains(first);
    }

    @Test
    void should_search_only_the_requested_reference() {
        var first = PortalNavigationItemFixtures.aPage("First portal", null);
        var second = PortalNavigationItemFixtures.aPage("Second portal", null);
        second.changeOwner(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")));
        var api = PortalNavigationItemFixtures.aPage("API documentation", null);
        api.changeOwner(new NavigationItemReference.ApiReference("api-id"));
        var query = new PortalNavigationItemsQueryServiceInMemory(List.of(first, second, api));
        var criteria = PortalNavigationItemQueryCriteria.builder()
            .environmentId(PortalNavigationItemFixtures.ENV_ID)
            .reference(first.getReference())
            .build();

        assertThat(query.search(criteria)).containsExactly(first);
    }

    @Test
    void should_not_find_a_segment_owned_by_another_portal_under_the_same_parent() {
        var parentId = PortalNavigationItemId.random();
        var foreign = PortalNavigationItemFixtures.aFolder("Guide", parentId);
        foreign.changeOwner(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")));
        var query = new PortalNavigationItemsQueryServiceInMemory(List.of(foreign));

        assertThat(
            query.findByParentIdAndSegment(
                PortalNavigationItemFixtures.ENV_ID,
                parentId,
                foreign.getSegment(),
                PortalNavigationItemFixtures.PORTAL_REFERENCE
            )
        ).isEmpty();
    }
}
