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
package io.gravitee.apim.infra.crud_service.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.documentation.exception.ApiPageNotDeletedException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.infra.crud_service.documentation.PageCrudServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.AccessControl;
import io.gravitee.repository.management.model.PageMedia;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PageCrudServiceImplTest {

    private final Date DATE = new Date();
    private final String PAGE_ID = "page-id";
    private final String PAGE_NAME = "page-name";

    @Mock
    PageRepository pageRepository;

    @Captor
    ArgumentCaptor<io.gravitee.repository.management.model.Page> pageArgumentCaptor;

    PageCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PageCrudServiceImpl(pageRepository);
    }

    @Nested
    class Create {

        @Test
        void should_create_a_page() throws TechnicalException {
            var date = new Date();
            String PAGE_ID = "page-id";
            service.createDocumentationPage(
                Page
                    .builder()
                    .id(PAGE_ID)
                    .type(Page.Type.MARKDOWN)
                    .name(PAGE_NAME)
                    .createdAt(date)
                    .updatedAt(date)
                    .content("nice content")
                    .homepage(true)
                    .referenceId("api-id")
                    .referenceType(Page.ReferenceType.API)
                    .order(21)
                    .crossId("cross-id")
                    .visibility(Page.Visibility.PRIVATE)
                    .lastContributor("last-contributor")
                    .published(true)
                    .parentId("parent-id")
                    .content("nice content")
                    .build()
            );

            var expectedPage = io.gravitee.repository.management.model.Page
                .builder()
                .id(PAGE_ID)
                .type("MARKDOWN")
                .name(PAGE_NAME)
                .createdAt(date)
                .updatedAt(date)
                .content("nice content")
                .homepage(true)
                .referenceId("api-id")
                .referenceType(PageReferenceType.API)
                .order(21)
                .crossId("cross-id")
                .visibility("PRIVATE")
                .lastContributor("last-contributor")
                .published(true)
                .parentId("parent-id")
                .content("nice content")
                .build();

            verify(pageRepository).create(pageArgumentCaptor.capture());
            Assertions.assertThat(pageArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedPage);
        }
    }

    @Nested
    class Get {

        @Test
        void should_get_a_page() throws TechnicalException {
            var exisitingPage = io.gravitee.repository.management.model.Page
                .builder()
                .id(PAGE_ID)
                .type("MARKDOWN")
                .name(PAGE_NAME)
                .createdAt(DATE)
                .updatedAt(DATE)
                .content("nice content")
                .homepage(true)
                .referenceId("api-id")
                .referenceType(PageReferenceType.API)
                .order(21)
                .crossId("cross-id")
                .visibility("PRIVATE")
                .lastContributor("last-contributor")
                .published(true)
                .parentId("parent-id")
                .content("nice content")
                .build();

            when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(exisitingPage));

            var expectedPage = Page
                .builder()
                .id(PAGE_ID)
                .type(Page.Type.MARKDOWN)
                .name(PAGE_NAME)
                .createdAt(DATE)
                .updatedAt(DATE)
                .content("nice content")
                .homepage(true)
                .referenceId("api-id")
                .referenceType(Page.ReferenceType.API)
                .order(21)
                .crossId("cross-id")
                .visibility(Page.Visibility.PRIVATE)
                .lastContributor("last-contributor")
                .published(true)
                .parentId("parent-id")
                .content("nice content")
                .build();

            var foundPage = service.get(PAGE_ID);

            Assertions.assertThat(foundPage).usingRecursiveComparison().isEqualTo(expectedPage);
        }

        @Test
        void should_throw_exception_if_page_not_found() throws TechnicalException {
            when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(PAGE_ID)).isInstanceOf(PageNotFoundException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void should_update_a_page() throws TechnicalException {
            var pageSource = new PageSource();
            pageSource.setConfiguration("page-source-configuration");
            pageSource.setType("config-type");

            var returnedPage = io.gravitee.repository.management.model.Page
                .builder()
                .id(PAGE_ID)
                .type("MARKDOWN")
                .name(PAGE_NAME)
                .createdAt(DATE)
                .updatedAt(DATE)
                .content("nice content")
                .homepage(false)
                .referenceId("api-id")
                .referenceType(PageReferenceType.API)
                .order(21)
                .crossId("cross-id")
                .visibility("PRIVATE")
                .lastContributor("last-contributor")
                .published(true)
                .parentId("parent-id")
                .content("nice content")
                .excludedAccessControls(true)
                .accessControls(Set.of(new AccessControl("ref", "api")))
                .useAutoFetch(true)
                .source(pageSource)
                .metadata(Map.of("key", "value"))
                .attachedMedia(List.of(new PageMedia("hash", "name", DATE)))
                .configuration(Map.of("foo", "bar"))
                .build();

            when(pageRepository.update(returnedPage)).thenReturn(returnedPage);

            var pageToUpdate = Page
                .builder()
                .id(PAGE_ID)
                .type(Page.Type.MARKDOWN)
                .name(PAGE_NAME)
                .createdAt(DATE)
                .updatedAt(DATE)
                .content("nice content")
                .homepage(false)
                .referenceId("api-id")
                .referenceType(Page.ReferenceType.API)
                .order(21)
                .crossId("cross-id")
                .visibility(Page.Visibility.PRIVATE)
                .lastContributor("last-contributor")
                .published(true)
                .parentId("parent-id")
                .content("nice content")
                .excludedAccessControls(true)
                .accessControls(
                    Set.of(
                        io.gravitee.apim.core.documentation.model.AccessControl.builder().referenceId("ref").referenceType("api").build()
                    )
                )
                .useAutoFetch(true)
                .source(
                    io.gravitee.apim.core.documentation.model.PageSource
                        .builder()
                        .type(pageSource.getType())
                        .configuration(pageSource.getConfiguration())
                        .build()
                )
                .metadata(Map.of("key", "value"))
                .attachedMedia(
                    List.of(
                        io.gravitee.apim.core.documentation.model.PageMedia
                            .builder()
                            .mediaHash("hash")
                            .mediaName("name")
                            .attachedAt(DATE)
                            .build()
                    )
                )
                .configuration(Map.of("foo", "bar"))
                .build();

            var updatedPage = service.updateDocumentationPage(pageToUpdate);

            verify(pageRepository).update(pageArgumentCaptor.capture());
            Assertions.assertThat(pageArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(returnedPage);

            Assertions.assertThat(updatedPage).usingRecursiveComparison().isEqualTo(pageToUpdate);
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_a_page() throws TechnicalException {
            service.delete(PAGE_ID);
            verify(pageRepository).delete(PAGE_ID);
        }

        @Test
        void should_throw_if_deletion_problem_occurs() throws TechnicalException {
            doThrow(new TechnicalException("exception")).when(pageRepository).delete(PAGE_ID);
            assertThatThrownBy(() -> service.delete(PAGE_ID))
                .isInstanceOf(ApiPageNotDeletedException.class)
                .hasMessage("Page page-id not deleted");
            verify(pageRepository).delete(PAGE_ID);
        }
    }
}
