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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures;
import io.gravitee.rest.api.portal.rest.model.PortalNavigationFolder;
import io.gravitee.rest.api.portal.rest.model.PortalNavigationLink;
import io.gravitee.rest.api.portal.rest.model.PortalNavigationPage;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemMapperTest {

    @Test
    void should_map_list_of_domain_items_to_rest_model_wrapped_items() {
        var area = PortalArea.HOMEPAGE;
        List<PortalNavigationItem> domain = PortalNavigationFixtures.sampleList(area);

        var restItems = PortalNavigationItemMapper.INSTANCE.map(domain);

        assertThat(restItems).hasSize(domain.size());

        // verify first is folder
        var item0 = restItems.getFirst();
        assertThat(item0.getActualInstance()).isInstanceOf(PortalNavigationFolder.class);
        var folder = (PortalNavigationFolder) item0.getActualInstance();
        assertThat(folder.getTitle()).isEqualTo("Folder 1");

        var item1 = restItems.get(1);
        assertThat(item1.getActualInstance()).isInstanceOf(PortalNavigationLink.class);
        var link = (PortalNavigationLink) item1.getActualInstance();
        assertThat(link.getTitle()).isEqualTo("Link 1");
        assertThat(link.getUrl()).isEqualTo("https://example.org");

        var item2 = restItems.get(2);
        assertThat(item2.getActualInstance()).isInstanceOf(PortalNavigationPage.class);
        var page = (PortalNavigationPage) item2.getActualInstance();
        assertThat(page.getTitle()).isEqualTo("Page 1");
        assertThat(page.getPortalPageContentId()).isNotNull();
    }

    @Test
    void should_map_ids_to_json_and_handle_null() {
        PortalNavigationItemId id = PortalNavigationFixtures.randomNavigationId();
        String json = PortalNavigationItemMapper.INSTANCE.map(id);
        assertThat(json).isEqualTo(id.json());

        String nullJson = PortalNavigationItemMapper.INSTANCE.map((PortalNavigationItemId) null);
        assertThat(nullJson).isNull();

        PortalPageContentId pageId = PortalNavigationFixtures.randomPageId();
        String pageJson = PortalNavigationItemMapper.INSTANCE.map(pageId);
        assertThat(pageJson).isEqualTo(pageId.json());
    }
}
