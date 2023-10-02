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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageQueryServiceImplTest {

    PageRepository pageRepository;
    PageQueryService service;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PageRepository.class);
        service = new PageQueryServiceImpl(pageRepository);
    }

    @Test
    void search_should_return_matching_pages() {
        String API_ID = "api-id";
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

    @SneakyThrows
    private void givenMatchingPages(String apiId, List<Page> pages) {
        when(pageRepository.search(eq(new PageCriteria.Builder().referenceId(apiId).referenceType(PageReferenceType.API.name()).build())))
            .thenReturn(pages);
    }
}
