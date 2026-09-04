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
package io.gravitee.apim.core.portal_page.domain_service.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HomepageReconcilerTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String PORTAL_ID = "11111111-1111-1111-1111-1111111111a1";
    private static final String OTHER_PORTAL_UUID = "11111111-1111-1111-1111-1111111111b1";
    private static final PortalNavigationItemId ACTIVE_ID = PortalNavigationItemId.of("00000000-0000-0000-0000-0000000000a1");
    private static final PortalNavigationItemId STALE_ID = PortalNavigationItemId.of("00000000-0000-0000-0000-0000000000a2");
    private static final PortalNavigationItemId UNATTACHED_ID = PortalNavigationItemId.of("00000000-0000-0000-0000-0000000000a3");
    private static final PortalNavigationItemId OTHER_PORTAL_ID = PortalNavigationItemId.of("00000000-0000-0000-0000-0000000000a4");

    private PortalNavigationItemsCrudServiceInMemory navItemCrud;
    private PortalNavigationItemsQueryServiceInMemory navItemQuery;
    private PortalPageContentCrudServiceInMemory pageContentCrud;
    private HomepageReconciler reconciler;

    @BeforeEach
    void setUp() {
        navItemCrud = new PortalNavigationItemsCrudServiceInMemory();
        navItemQuery = new PortalNavigationItemsQueryServiceInMemory(navItemCrud.storage());
        pageContentCrud = new PortalPageContentCrudServiceInMemory();
        reconciler = new HomepageReconciler(navItemQuery, navItemCrud, pageContentCrud);
    }

    @Test
    void is_a_noop_when_no_homepages_exist() {
        assertThatCode(() -> reconciler.dropStaleHomepages(ENV_ID, PORTAL_ID, ACTIVE_ID)).doesNotThrowAnyException();
        assertThat(navItemCrud.storage()).isEmpty();
    }

    @Test
    void keeps_the_active_homepage_untouched() {
        var contentId = PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1");
        navItemCrud.create(homepage(ACTIVE_ID, new NavigationItemReference.PortalReference(PortalId.of(PORTAL_ID)), contentId));
        pageContentCrud.create(content(contentId));

        reconciler.dropStaleHomepages(ENV_ID, PORTAL_ID, ACTIVE_ID);

        assertThat(navItemCrud.storage()).hasSize(1);
        assertThat(pageContentCrud.storage()).hasSize(1);
    }

    @Test
    void drops_stale_homepage_of_same_portal_and_its_content() {
        var staleContentId = PortalPageContentId.of("00000000-0000-0000-0000-0000000000c2");
        navItemCrud.create(homepage(STALE_ID, new NavigationItemReference.PortalReference(PortalId.of(PORTAL_ID)), staleContentId));
        pageContentCrud.create(content(staleContentId));

        reconciler.dropStaleHomepages(ENV_ID, PORTAL_ID, ACTIVE_ID);

        assertThat(navItemCrud.storage()).isEmpty();
        assertThat(pageContentCrud.storage()).isEmpty();
    }

    @Test
    void drops_unattached_sentinel_homepage_and_its_content() {
        var seededContentId = PortalPageContentId.of("00000000-0000-0000-0000-0000000000c3");
        navItemCrud.create(homepage(UNATTACHED_ID, NavigationItemReference.defaultReference(), seededContentId));
        pageContentCrud.create(content(seededContentId));

        reconciler.dropStaleHomepages(ENV_ID, PORTAL_ID, ACTIVE_ID);

        assertThat(navItemCrud.storage()).isEmpty();
        assertThat(pageContentCrud.storage()).isEmpty();
    }

    @Test
    void does_not_touch_homepage_of_a_different_portal() {
        var otherContentId = PortalPageContentId.of("00000000-0000-0000-0000-0000000000c4");
        navItemCrud.create(
            homepage(OTHER_PORTAL_ID, new NavigationItemReference.PortalReference(PortalId.of(OTHER_PORTAL_UUID)), otherContentId)
        );
        pageContentCrud.create(content(otherContentId));

        reconciler.dropStaleHomepages(ENV_ID, PORTAL_ID, ACTIVE_ID);

        assertThat(navItemCrud.storage()).hasSize(1);
        assertThat(pageContentCrud.storage()).hasSize(1);
    }

    @Test
    void drops_non_page_homepage_without_touching_page_contents() {
        var folder = PortalNavigationFolder.builder()
            .id(STALE_ID)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(PortalId.of(PORTAL_ID)))
            .title("Stale Folder")
            .segment("stale-folder")
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        navItemCrud.create(folder);
        var untouchedContentId = PortalPageContentId.of("00000000-0000-0000-0000-0000000000c5");
        pageContentCrud.create(content(untouchedContentId));

        reconciler.dropStaleHomepages(ENV_ID, PORTAL_ID, ACTIVE_ID);

        assertThat(navItemCrud.storage()).isEmpty();
        assertThat(pageContentCrud.storage()).hasSize(1);
    }

    private static PortalNavigationPage homepage(
        PortalNavigationItemId id,
        NavigationItemReference reference,
        PortalPageContentId contentId
    ) {
        return PortalNavigationPage.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(reference)
            .title("Home")
            .segment("home")
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .portalPageContentId(contentId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static GraviteeMarkdownPageContent content(PortalPageContentId contentId) {
        return new GraviteeMarkdownPageContent(contentId, ORG_ID, ENV_ID, GraviteeMarkdown.of("# Home"));
    }
}
