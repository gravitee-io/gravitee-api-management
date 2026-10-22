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
package io.gravitee.apim.infra.query_service.api_product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiProductSearchService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductSearchQueryServiceImplTest {

    @Mock
    private ApiProductSearchService apiProductSearchService;

    @Mock
    private ApiSearchService apiSearchService;

    @Captor
    private ArgumentCaptor<QueryBuilder<IndexableApiProduct>> queryCaptor;

    private ApiProductSearchQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiProductSearchQueryServiceImpl(apiProductSearchService, apiSearchService);
        when(apiProductSearchService.search(any(), any(), any())).thenReturn(new Page<>(List.of(), 1, 0, 0));
    }

    @Test
    void should_keep_an_empty_id_filter_so_it_matches_nothing() {
        service.search("env-1", "org-1", null, Set.of(), (Pageable) null, null);
        verify(apiProductSearchService).search(any(), queryCaptor.capture(), eq(null));

        assertThat(queryCaptor.getValue().build().getFilters()).containsEntry("api_product", Set.of());
    }

    @Test
    void should_not_filter_by_id_when_ids_are_null() {
        service.search("env-1", "org-1", null, null, (Pageable) null, null);
        verify(apiProductSearchService).search(any(), queryCaptor.capture(), eq(null));

        assertThat(queryCaptor.getValue().build().getFilters()).doesNotContainKey("api_product");
    }
}
