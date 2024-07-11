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
package io.gravitee.apim.infra.query_service.documentation;

import static io.gravitee.apim.core.fixtures.PageFixtures.aPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import java.util.Date;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PageQueryServiceImplTest {

    private final Date DATE = new Date();
    private final String PAGE_ID = "page-id";
    private final String PAGE_NAME = "page-name";
    private final String API_ID = "api-id";

    PageRepository pageRepository;
    PageQueryService service;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PageRepository.class);
        service = new PageQueryServiceImpl(pageRepository);
    }

    @Nested
    class SearchByApiId {

        @Test
        void search_should_return_matching_pages() {
            Page page1 = aPage(API_ID, "page#1", "page 1");
            Page page2 = aPage(API_ID, "page#2", "page 2");
            List<Page> pages = List.of(page1, page2);
            givenMatchingPages(API_ID, pages);

            var res = service.searchByApiId(API_ID);
            assertThat(res).hasSize(2);
            assertThat(res.get(0).getId()).isEqualTo("page#1");
            assertThat(res.get(0).getName()).isEqualTo("page 1");
            assertThat(res.get(0).getReferenceId()).isEqualTo(API_ID);
            assertThat(res.get(0).getReferenceType()).isEqualTo(io.gravitee.apim.core.documentation.model.Page.ReferenceType.API);
            assertThat(res.get(1).getId()).isEqualTo("page#2");
            assertThat(res.get(1).getName()).isEqualTo("page 2");
            assertThat(res.get(1).getReferenceId()).isEqualTo(API_ID);
            assertThat(res.get(1).getReferenceType()).isEqualTo(io.gravitee.apim.core.documentation.model.Page.ReferenceType.API);
        }
    }

    @Nested
    class SearchByApiIdAndIsHomepage {

        @Test
        void search_should_return_api_homepage() {
            Page page1 = aPage(API_ID, "page#1", "page 1");
            List<Page> pages = List.of(page1);
            givenMatchingPages(new PageCriteria.Builder().referenceId(API_ID).referenceType("API").homepage(true), pages);

            var res = service.findHomepageByApiId(API_ID).get();

            assertThat(res).isNotNull();
            assertThat(res.getId()).isEqualTo("page#1");
            assertThat(res.getName()).isEqualTo("page 1");
        }

        @Test
        void search_should_return_no_api_homepage() {
            givenMatchingPages(new PageCriteria.Builder().referenceId(API_ID).referenceType("API").homepage(true), List.of());

            var res = service.findHomepageByApiId(API_ID);
            assertThat(res.isEmpty()).isTrue();
        }
    }

    @Nested
    class SearchByApiIdAndParentId {

        @Test
        void search_should_return_pages_if_parent_is_root() {
            Page page1 = aPage(API_ID, "page#1", "page 1");
            List<Page> pages = List.of(page1);
            givenMatchingPages(new PageCriteria.Builder().referenceId(API_ID).referenceType("API").rootParent(true), pages);

            var res = service.searchByApiIdAndParentId(API_ID, null);

            assertThat(res).isNotNull().hasSize(1);
        }

        @Test
        void search_should_return_pages_if_parent_is_not_root() {
            Page page1 = aPage(API_ID, "page#1", "page 1");
            List<Page> pages = List.of(page1);
            givenMatchingPages(new PageCriteria.Builder().referenceId(API_ID).referenceType("API").parent("nice-parent"), pages);

            var res = service.searchByApiIdAndParentId(API_ID, "nice-parent");

            assertThat(res).isNotNull().hasSize(1);
        }
    }

    @Nested
    class FindByApiIdAndParentIdAndNameAndType {

        @Test
        void should_find_page_with_parent_id() {
            String PARENT_ID = "parent-id";

            var repositoryPage = aPage(API_ID, PAGE_ID, "duplicate name");
            var expectedPage = io.gravitee.apim.core.documentation.model.Page
                .builder()
                .id(PAGE_ID)
                .referenceId(API_ID)
                .referenceType(io.gravitee.apim.core.documentation.model.Page.ReferenceType.API)
                .name(repositoryPage.getName())
                .build();

            givenMatchingPages(
                new PageCriteria.Builder()
                    .referenceId(API_ID)
                    .referenceType("API")
                    .parent(PARENT_ID)
                    .type("MARKDOWN")
                    .name("duplicate name"),
                List.of(repositoryPage)
            );

            var res = service.findByApiIdAndParentIdAndNameAndType(
                API_ID,
                PARENT_ID,
                "duplicate name",
                io.gravitee.apim.core.documentation.model.Page.Type.MARKDOWN
            );

            assertThat(res.get()).usingRecursiveComparison().isEqualTo(expectedPage);
        }

        @Test
        void should_find_page_with_null_parent_id() {
            String PARENT_ID = null;

            var repositoryPage = aPage(API_ID, PAGE_ID, "duplicate name");
            var expectedPage = io.gravitee.apim.core.documentation.model.Page
                .builder()
                .id(PAGE_ID)
                .referenceId(API_ID)
                .referenceType(io.gravitee.apim.core.documentation.model.Page.ReferenceType.API)
                .name(repositoryPage.getName())
                .build();

            givenMatchingPages(
                new PageCriteria.Builder()
                    .referenceId(API_ID)
                    .referenceType("API")
                    .rootParent(true)
                    .type("MARKDOWN")
                    .name("duplicate name"),
                List.of(repositoryPage)
            );

            var res = service.findByApiIdAndParentIdAndNameAndType(
                API_ID,
                PARENT_ID,
                "duplicate name",
                io.gravitee.apim.core.documentation.model.Page.Type.MARKDOWN
            );

            assertThat(res.get()).usingRecursiveComparison().isEqualTo(expectedPage);
        }

        @Test
        void should_not_find_matching_page() {
            String PARENT_ID = "parent-id";

            var page = aPage(API_ID, PAGE_ID, "original name");
            givenMatchingPages(
                new PageCriteria.Builder()
                    .referenceId(API_ID)
                    .referenceType("API")
                    .parent(PARENT_ID)
                    .type("MARKDOWN")
                    .name("different name"),
                List.of(page)
            );

            var res = service.findByApiIdAndParentIdAndNameAndType(
                API_ID,
                PARENT_ID,
                "original name",
                io.gravitee.apim.core.documentation.model.Page.Type.MARKDOWN
            );

            assertThat(res.isEmpty()).isTrue();
        }
    }

    @Nested
    class FindByName {

        @Test
        void should_find_a_page() throws TechnicalException {
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
                .build();

            when(
                pageRepository.search(
                    new PageCriteria.Builder().name(exisitingPage.getName()).referenceId(exisitingPage.getReferenceId()).build()
                )
            )
                .thenReturn(List.of(exisitingPage));

            var expectedPage = io.gravitee.apim.core.documentation.model.Page
                .builder()
                .id(PAGE_ID)
                .type(io.gravitee.apim.core.documentation.model.Page.Type.MARKDOWN)
                .name(PAGE_NAME)
                .createdAt(DATE)
                .updatedAt(DATE)
                .content("nice content")
                .homepage(true)
                .referenceId("api-id")
                .referenceType(io.gravitee.apim.core.documentation.model.Page.ReferenceType.API)
                .order(21)
                .crossId("cross-id")
                .visibility(io.gravitee.apim.core.documentation.model.Page.Visibility.PRIVATE)
                .lastContributor("last-contributor")
                .published(true)
                .parentId("parent-id")
                .build();

            var foundPage = service.findByNameAndReferenceId(PAGE_NAME, API_ID);

            Assertions.assertThat(foundPage).isPresent().get().usingRecursiveComparison().isEqualTo(expectedPage);
        }

        @Test
        void should_return_empty_if_no_page_found() throws TechnicalException {
            when(pageRepository.search(new PageCriteria.Builder().name(PAGE_NAME).build())).thenReturn(List.of());

            var foundPage = service.findByNameAndReferenceId(PAGE_NAME, API_ID);

            Assertions.assertThat(foundPage).isEmpty();
        }

        @Test
        void should_throw_exception_if_more_than_one_page_found() throws TechnicalException {
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
                .build();

            var anotherExistingPageWithSameName = io.gravitee.repository.management.model.Page
                .builder()
                .id(PAGE_ID + "2")
                .type("MARKDOWN")
                .name(PAGE_NAME)
                .createdAt(DATE)
                .updatedAt(DATE)
                .content("very nice content")
                .homepage(true)
                .referenceId("api-id")
                .referenceType(PageReferenceType.API)
                .order(21)
                .crossId("cross-id")
                .visibility("PRIVATE")
                .lastContributor("last-contributor")
                .published(true)
                .parentId("parent-id")
                .build();

            when(
                pageRepository.search(
                    new PageCriteria.Builder().name(exisitingPage.getName()).referenceId(exisitingPage.getReferenceId()).build()
                )
            )
                .thenReturn(List.of(exisitingPage, anotherExistingPageWithSameName));

            assertThatThrownBy(() -> service.findByNameAndReferenceId(PAGE_NAME, API_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Found more than one page with name " + PAGE_NAME);
        }

        @Test
        void should_throw_exception_if_problem_occurs() throws TechnicalException {
            doThrow(new TechnicalException("exception"))
                .when(pageRepository)
                .search(new PageCriteria.Builder().name(PAGE_NAME).referenceId(API_ID).build());
            assertThatThrownBy(() -> service.findByNameAndReferenceId(PAGE_NAME, API_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("Error when updating Page");
            verify(pageRepository).search(new PageCriteria.Builder().name(PAGE_NAME).referenceId(API_ID).build());
        }
    }

    @SneakyThrows
    private void givenMatchingPages(String apiId, List<Page> pages) {
        givenMatchingPages(new PageCriteria.Builder().referenceId(apiId).referenceType(PageReferenceType.API.name()), pages);
    }

    @SneakyThrows
    private void givenMatchingPages(PageCriteria.Builder pageCriteriaBuilder, List<Page> pages) {
        when(pageRepository.search(eq(pageCriteriaBuilder.build()))).thenReturn(pages);
    }
}
