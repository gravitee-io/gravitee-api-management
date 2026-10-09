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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.repository.model.PortalNavigationItemsRepositoryFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import java.time.Instant;
import java.util.Optional;
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
                .segment("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.create(item);

            final var expectedItem = PortalNavigationItemsRepositoryFixtures.expectedFolderFromCreate(
                itemId.toString(),
                "title",
                "organizationId",
                "environmentId",
                0,
                PortalNavigationItemsRepositoryFixtures.ROOT_ID_ZERO
            );

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
                .segment("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .portalPageContentId(contentId)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.create(item);

            final var expectedItem = PortalNavigationItemsRepositoryFixtures.expectedPageFromCreate(
                itemId.toString(),
                "title",
                "organizationId",
                "environmentId",
                0,
                PortalNavigationItemsRepositoryFixtures.ROOT_ID_ZERO,
                contentId.toString()
            );

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
                .segment("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .url(url)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.create(item);

            final var expectedItem = PortalNavigationItemsRepositoryFixtures.expectedLinkFromCreate(
                itemId.toString(),
                "title",
                "organizationId",
                "environmentId",
                0,
                PortalNavigationItemsRepositoryFixtures.ROOT_ID_ZERO,
                url
            );

            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedItem);
        }

        @Test
        void should_create_a_folder_with_non_null_rootId() throws TechnicalException {
            final var itemId = PortalNavigationItemId.random();
            final var rootId = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000099");
            final var item = PortalNavigationFolder.builder()
                .id(itemId)
                .organizationId("organizationId")
                .environmentId("environmentId")
                .title("title")
                .segment("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .rootId(rootId)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.create(item);

            final var expectedItem = PortalNavigationItemsRepositoryFixtures.expectedFolderFromCreate(
                itemId.toString(),
                "title",
                "organizationId",
                "environmentId",
                0,
                "00000000-0000-0000-0000-000000000099"
            );

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
                .segment("title")
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
                .segment("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.update(item);

            final var expectedItem = PortalNavigationItemsRepositoryFixtures.expectedFolderFromCreate(
                itemId.toString(),
                "title",
                "organizationId",
                "environmentId",
                0,
                PortalNavigationItemsRepositoryFixtures.ROOT_ID_ZERO
            );

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
                .segment("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .portalPageContentId(contentId)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.update(item);

            final var expectedItem = PortalNavigationItemsRepositoryFixtures.expectedPageFromCreate(
                itemId.toString(),
                "title",
                "organizationId",
                "environmentId",
                0,
                PortalNavigationItemsRepositoryFixtures.ROOT_ID_ZERO,
                contentId.toString()
            );

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
                .segment("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .url(url)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();

            service.update(item);

            final var expectedItem = PortalNavigationItemsRepositoryFixtures.expectedLinkFromCreate(
                itemId.toString(),
                "title",
                "organizationId",
                "environmentId",
                0,
                PortalNavigationItemsRepositoryFixtures.ROOT_ID_ZERO,
                url
            );

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
                .segment("title")
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

    @Nested
    class UpdateSourceFetchState {

        private static final String ID = "00000000-0000-0000-0000-000000000007";
        private static final String STORED_CONFIGURATION =
            "{\"portalPageContentId\":\"00000000-0000-0000-0000-000000000070\",\"source\":{\"type\":\"http-fetcher\",\"configuration\":\"{}\",\"lastFetchedAt\":\"2026-10-01T10:00:00Z\"}}";
        private static final PortalNavigationItemSource FETCHED_SOURCE = PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{}")
            .build();
        private static final PortalNavigationItemSource.FetchState FETCH_STATE = PortalNavigationItemSource.FetchState.succeeded(
            Instant.parse("2026-10-08T10:00:00Z"),
            Instant.parse("2026-10-08T10:00:00Z")
        );

        private io.gravitee.repository.management.model.PortalNavigationItem storedPage() {
            var page = PortalNavigationItemsRepositoryFixtures.aPage(ID, "Stored title", "00000000-0000-0000-0000-000000000070", null);
            page.setConfiguration(STORED_CONFIGURATION);
            return page;
        }

        @Test
        void should_replace_only_the_configuration_of_the_stored_item() throws TechnicalException {
            when(repository.findById(ID)).thenReturn(Optional.of(storedPage()));
            when(repository.updateConfigurationIfUnchanged(eq(ID), eq(STORED_CONFIGURATION), any())).thenReturn(true);

            var updated = service.updateSourceFetchState(PortalNavigationItemId.of(ID), FETCHED_SOURCE, FETCH_STATE);

            verify(repository, never()).update(any());
            var configurationCaptor = ArgumentCaptor.forClass(String.class);
            verify(repository).updateConfigurationIfUnchanged(eq(ID), eq(STORED_CONFIGURATION), configurationCaptor.capture());
            assertThat(configurationCaptor.getValue()).contains("\"lastFetchedAt\":\"2026-10-08T10:00:00Z\"");
            assertThat(updated)
                .get()
                .satisfies(item -> {
                    assertThat(item.getTitle()).isEqualTo("Stored title");
                    assertThat(item.getSource().getLastFetchedAt()).isEqualTo(FETCH_STATE.lastFetchedAt());
                    assertThat(item.getSource().getLastFetchAttemptAt()).isEqualTo(FETCH_STATE.lastFetchAttemptAt());
                });
        }

        @Test
        void should_retry_on_the_fresh_row_when_the_configuration_changed_meanwhile() throws TechnicalException {
            var restampedConfiguration = STORED_CONFIGURATION.replace("2026-10-01T10:00:00Z", "2026-10-05T10:00:00Z");
            var fresh = storedPage();
            fresh.setConfiguration(restampedConfiguration);
            when(repository.findById(ID)).thenReturn(Optional.of(storedPage()), Optional.of(fresh));
            when(repository.updateConfigurationIfUnchanged(eq(ID), eq(STORED_CONFIGURATION), any())).thenReturn(false);
            when(repository.updateConfigurationIfUnchanged(eq(ID), eq(restampedConfiguration), any())).thenReturn(true);

            var updated = service.updateSourceFetchState(PortalNavigationItemId.of(ID), FETCHED_SOURCE, FETCH_STATE);

            assertThat(updated)
                .get()
                .satisfies(item -> assertThat(item.getSource().getLastFetchedAt()).isEqualTo(FETCH_STATE.lastFetchedAt()));
        }

        @Test
        void should_drop_the_state_without_writing_when_the_stored_source_has_another_origin() throws TechnicalException {
            var swapped = storedPage();
            swapped.setConfiguration(
                STORED_CONFIGURATION.replace("\"configuration\":\"{}\"", "\"configuration\":\"{\\\"url\\\":\\\"x\\\"}\"")
            );
            when(repository.findById(ID)).thenReturn(Optional.of(swapped));

            var updated = service.updateSourceFetchState(PortalNavigationItemId.of(ID), FETCHED_SOURCE, FETCH_STATE);

            assertThat(updated)
                .get()
                .satisfies(item -> {
                    assertThat(item.getSource().getSourceConfiguration()).isEqualTo("{\"url\":\"x\"}");
                    assertThat(item.getSource().getLastFetchedAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
                });
            verify(repository, never()).updateConfigurationIfUnchanged(any(), any(), any());
        }

        @Test
        void should_return_empty_without_writing_when_the_item_no_longer_exists() throws TechnicalException {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            var updated = service.updateSourceFetchState(PortalNavigationItemId.of(ID), FETCHED_SOURCE, FETCH_STATE);

            assertThat(updated).isEmpty();
            verify(repository, never()).updateConfigurationIfUnchanged(any(), any(), any());
            verify(repository, never()).update(any());
        }

        @Test
        void should_return_the_stored_item_without_writing_when_it_no_longer_carries_a_source() throws TechnicalException {
            var sourceless = storedPage();
            sourceless.setConfiguration("{\"portalPageContentId\":\"00000000-0000-0000-0000-000000000070\"}");
            when(repository.findById(ID)).thenReturn(Optional.of(sourceless));

            var updated = service.updateSourceFetchState(PortalNavigationItemId.of(ID), FETCHED_SOURCE, FETCH_STATE);

            assertThat(updated)
                .get()
                .satisfies(item -> assertThat(item.getSource()).isNull());
            verify(repository, never()).updateConfigurationIfUnchanged(any(), any(), any());
        }

        @Test
        void should_give_up_when_the_row_keeps_changing() throws TechnicalException {
            when(repository.findById(ID)).thenReturn(Optional.of(storedPage()));
            when(repository.updateConfigurationIfUnchanged(any(), any(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.updateSourceFetchState(PortalNavigationItemId.of(ID), FETCHED_SOURCE, FETCH_STATE))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessageContaining(ID);
            verify(repository, times(5)).updateConfigurationIfUnchanged(any(), any(), any());
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            when(repository.findById(ID)).thenThrow(new TechnicalException("boom"));

            assertThatThrownBy(() -> service.updateSourceFetchState(PortalNavigationItemId.of(ID), FETCHED_SOURCE, FETCH_STATE))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while updating the fetch state of portal navigation item with id " + ID)
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }
}
