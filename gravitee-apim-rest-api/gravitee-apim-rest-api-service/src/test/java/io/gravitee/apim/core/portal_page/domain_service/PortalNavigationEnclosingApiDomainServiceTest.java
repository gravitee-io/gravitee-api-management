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

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortalNavigationEnclosingApiDomainServiceTest {

    private static final String ROOT_PAGE_ID = "00000000-0000-0000-0000-00000000a001";
    private static final String FOLDER_UNDER_API_ID = "00000000-0000-0000-0000-00000000a002";
    private static final String DOC_PAGE_ID = "00000000-0000-0000-0000-00000000a003";

    private PortalNavigationItemsQueryServiceInMemory itemsQueryService;
    private PortalNavigationEnclosingApiDomainService enclosingApiDomainService;

    @BeforeEach
    void setUp() {
        itemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        enclosingApiDomainService = new PortalNavigationEnclosingApiDomainService(itemsQueryService);
    }

    @Test
    void should_find_no_api_when_root_page_without_api_ancestor() {
        var page = PortalNavigationItemFixtures.aPage(ROOT_PAGE_ID, "P", null);
        page.markAsRoot();
        itemsQueryService.initWith(List.of(page));

        assertThat(enclosingApiDomainService.findEnclosingApiId(ENV_ID, page)).isEmpty();
    }

    @Test
    void should_find_api_ancestor_through_folder_chain() {
        var api = PortalNavigationItemFixtures.anApi(PortalNavigationItemFixtures.API1_ID, "A", null, "api-x");
        api.markAsRoot();
        var folder = PortalNavigationItemFixtures.aFolder(FOLDER_UNDER_API_ID, "F", api.getId());
        folder.updateParent(api);
        var contentId = PortalPageContentId.random();
        var page = PortalNavigationItemFixtures.aPage(DOC_PAGE_ID, "Doc", folder.getId(), contentId);
        page.updateParent(folder);
        itemsQueryService.initWith(List.of(api, folder, page));

        assertThat(enclosingApiDomainService.findEnclosingApiId(ENV_ID, page)).contains("api-x");
    }
}
