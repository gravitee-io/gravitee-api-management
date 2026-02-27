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
package io.gravitee.apim.infra.crud_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
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

        @BeforeEach
        void setUp() throws TechnicalException {
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void should_create_a_folder() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var item = PortalNavigationFolder.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

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
                .published(true)
                .visibility(io.gravitee.repository.management.model.PortalNavigationItem.Visibility.PUBLIC)
                .build();

            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_create_a_page() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var contentId = PortalPageContentId.random();
            final var item = PortalNavigationPage.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .portalPageContentId(contentId)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

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
                .published(true)
                .visibility(io.gravitee.repository.management.model.PortalNavigationItem.Visibility.PUBLIC)
                .build();

            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_create_a_link() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var url = "http://example.com";
            final var item = PortalNavigationLink.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .url(url)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

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
                .configuration("{\"url\":\"" + url + "\"}")
                .published(true)
                .visibility(io.gravitee.repository.management.model.PortalNavigationItem.Visibility.PUBLIC)
                .build();

            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            // Given
            final var itemId = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001");
            final var contentId = PortalPageContentId.random();
            final var item = PortalNavigationPage.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .portalPageContentId(contentId)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();
            when(repository.create(any())).thenThrow(new TechnicalException("Database error"));

            // When & Then
            assertThatThrownBy(() -> service.create(item))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while creating portal navigation item with id 00000000-0000-0000-0000-000000000001 and environmentId environmentId"
                )
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    @Nested
    class UpdatePortalNavigationItem {

        @BeforeEach
        void setUp() throws TechnicalException {
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void should_update_a_folder() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var item = PortalNavigationFolder.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.update(item);

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
                .published(true)
                .visibility(io.gravitee.repository.management.model.PortalNavigationItem.Visibility.PUBLIC)
                .build();

            verify(repository).update(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_update_a_page() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var contentId = PortalPageContentId.random();
            final var item = PortalNavigationPage.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .portalPageContentId(contentId)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.update(item);

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
                .published(true)
                .visibility(io.gravitee.repository.management.model.PortalNavigationItem.Visibility.PUBLIC)
                .build();

            verify(repository).update(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_update_a_link() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var url = "http://example.com";
            final var item = PortalNavigationLink.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .url(url)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.update(item);

            final var expectedItem = io.gravitee.repository.management.model.PortalNavigationItem.builder()
                .id(itemId.toString())
                .title("title")
                .organizationId("organizationId")
                .environmentId("environmentId")
                .type(PortalNavigationItem.Type.LINK)
                .area(io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR)
                .order(0)
                .parentId(null)
                .configuration("{\"url\":\"" + url + "\"}")
                .published(true)
                .visibility(io.gravitee.repository.management.model.PortalNavigationItem.Visibility.PUBLIC)
                .build();

            verify(repository).update(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            // Given
            final var itemId = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001");
            final var contentId = PortalPageContentId.random();
            final var item = PortalNavigationPage.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .portalPageContentId(contentId)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();
            when(repository.update(any())).thenThrow(new TechnicalException("Database error"));

            // When & Then
            assertThatThrownBy(() -> service.update(item))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while updating portal navigation item with id 00000000-0000-0000-0000-000000000001 and environmentId environmentId"
                )
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    @Nested
    class DeletePortalNavigationItem {

        @Test
        void should_delete_an_item() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();

            service.delete(itemId);

            final var captor = ArgumentCaptor.forClass(String.class);
            verify(repository).delete(captor.capture());
            assertThat(captor.getValue()).isEqualTo(itemId.toString());
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            // Given
            final var itemId = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001");
            doThrow(new TechnicalException("Database error")).when(repository).delete(any());

            // When & Then
            assertThatThrownBy(() -> service.delete(itemId))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while deleting portal navigation item with id 00000000-0000-0000-0000-000000000001")
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }
}
