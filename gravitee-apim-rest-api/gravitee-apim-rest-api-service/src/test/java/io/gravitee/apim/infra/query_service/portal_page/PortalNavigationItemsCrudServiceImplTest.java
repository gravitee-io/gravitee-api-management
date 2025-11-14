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
package io.gravitee.apim.infra.query_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.PortalNavigationItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsCrudServiceImplTest {

    @Mock
    PortalNavigationItemRepository repository;

    PortalNavigationItemsCrudServiceImpl service;

    @Captor
    ArgumentCaptor<io.gravitee.repository.management.model.PortalNavigationItem> captor;

    @BeforeEach
    void setUp() {
        service = new PortalNavigationItemsCrudServiceImpl(repository);
    }

    @Nested
    class CreatePortalNavigationItem {

        @Test
        void should_create_a_folder() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var item = new PortalNavigationFolder(itemId, "organizationId", "environmentId", "title", PortalArea.TOP_NAVBAR, 0);

            service.create(item);

            final var expectedItem = io.gravitee.repository.management.model.PortalNavigationItem.builder()
                .id(itemId.toString())
                .title("title")
                .organizationId("organizationId")
                .environmentId("environmentId")
                .type(PortalNavigationItem.Type.FOLDER)
                .area(io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR)
                .order(0)
                .parentId(null)
                .configuration("{}")
                .build();

            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_create_a_page() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var contentId = PortalPageContentId.random();
            final var item = new PortalNavigationPage(
                itemId,
                "organizationId",
                "environmentId",
                "title",
                PortalArea.TOP_NAVBAR,
                0,
                contentId
            );

            service.create(item);

            final var expectedItem = io.gravitee.repository.management.model.PortalNavigationItem.builder()
                .id(itemId.toString())
                .title("title")
                .organizationId("organizationId")
                .environmentId("environmentId")
                .type(PortalNavigationItem.Type.PAGE)
                .area(io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR)
                .order(0)
                .parentId(null)
                .configuration("{\"portalPageContentId\":\"" + contentId.toString() + "\"}")
                .build();

            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_create_a_link() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var contentId = PortalPageContentId.random();
            final var href = "http://example.com";
            final var item = new PortalNavigationLink(itemId, "organizationId", "environmentId", "title", PortalArea.TOP_NAVBAR, 0, href);

            service.create(item);

            final var expectedItem = io.gravitee.repository.management.model.PortalNavigationItem.builder()
                .id(itemId.toString())
                .title("title")
                .organizationId("organizationId")
                .environmentId("environmentId")
                .type(PortalNavigationItem.Type.LINK)
                .area(io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR)
                .order(0)
                .parentId(null)
                .configuration("{\"href\":\"" + href + "\"}")
                .build();

            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }
    }
}
