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

import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.model.ApiProductKindFilter;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer;
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

    private static final String FIELD_KIND = IndexableApiProductDocumentTransformer.FIELD_KIND;

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

    private QueryBuilder<IndexableApiProduct> searchWith(ApiProductKindFilter kindFilter) {
        service.search("env-1", "org-1", null, null, (Pageable) null, null, kindFilter);
        verify(apiProductSearchService).search(any(), queryCaptor.capture(), eq(null));
        return queryCaptor.getValue();
    }

    @Test
    void should_require_the_kind_when_the_listing_does_not_want_classic_products() {
        // The page has to be narrowed by the store, not after it: a page of twenty-five asked of a store
        // that also holds classic products comes back with fewer than twenty-five workspaces, or none.
        var query = searchWith(new ApiProductKindFilter(Set.of(ApiProductKind.AI_WORKSPACE), false)).build();

        assertThat(query.getFilters()).containsEntry(FIELD_KIND, Set.of(ApiProductKind.AI_WORKSPACE.name()));
        assertThat(query.getExcludedFilters()).doesNotContainKey(FIELD_KIND);
    }

    @Test
    void should_exclude_specialized_kinds_from_a_classic_listing() {
        // Classic products carry no kind, so they cannot be required; the listing hides the kinds it does
        // not want instead.
        var query = searchWith(ApiProductKindFilter.classicOnly()).build();

        assertThat(query.getExcludedFilters()).containsEntry(FIELD_KIND, Set.of(ApiProductKind.AI_WORKSPACE.name()));
        assertThat(query.getFilters()).doesNotContainKey(FIELD_KIND);
    }

    @Test
    void should_not_narrow_by_kind_when_the_listing_wants_everything() {
        var query = searchWith(ApiProductKindFilter.any()).build();

        assertThat(query.getFilters()).doesNotContainKey(FIELD_KIND);
        assertThat(query.getExcludedFilters()).doesNotContainKey(FIELD_KIND);
    }
}
