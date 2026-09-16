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
package io.gravitee.rest.api.service.impl.search.lucene.searcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.search.query.SearchSortStrategy;
import java.util.List;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.Test;

class ApiDocumentSearcherStableSortTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";

    @Test
    void should_return_the_same_order_for_equivalent_indexes_when_stable_sort_is_requested() throws Exception {
        var results = searchEquivalentIndexes(true);

        assertThat(results.getFirst()).containsExactly("api-relevant", "api-a", "api-b", "api-z");
        assertThat(results.getLast()).containsExactlyElementsOf(results.getFirst());
    }

    @Test
    void should_preserve_lucene_order_when_stable_sort_is_not_requested() throws Exception {
        var results = searchEquivalentIndexes(false);

        assertThat(results.getFirst()).containsExactly("api-relevant", "api-b", "api-a", "api-z");
        assertThat(results.getLast()).containsExactly("api-relevant", "api-z", "api-a", "api-b");
    }

    private static List<List<String>> searchEquivalentIndexes(boolean stableSort) throws Exception {
        var apis = List.of(
            api("api-relevant", "Zulu common", "unrelated"),
            api("api-b", "Same", "common"),
            api("api-a", "Same", "common"),
            api("api-z", "Zed", "common")
        );

        try (
            var firstDirectory = new ByteBuffersDirectory();
            var secondDirectory = new ByteBuffersDirectory();
            var firstAnalyzer = new StandardAnalyzer();
            var secondAnalyzer = new StandardAnalyzer();
            var firstWriter = new IndexWriter(firstDirectory, new IndexWriterConfig(firstAnalyzer));
            var secondWriter = new IndexWriter(secondDirectory, new IndexWriterConfig(secondAnalyzer))
        ) {
            var transformer = new ApiDocumentTransformer(mock(ApiService.class));
            addDocuments(firstWriter, transformer, apis);
            addDocuments(secondWriter, transformer, apis.reversed());

            var queryBuilder = QueryBuilder.create(ApiEntity.class).setQuery("common").setTypoTolerance(false);
            if (stableSort) {
                queryBuilder.setSearchSortStrategy(SearchSortStrategy.SCORE_WITH_NAME_AND_ID_TIE_BREAKERS);
            }
            var query = queryBuilder.build();
            var executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

            var firstResult = new ApiDocumentSearcher(firstWriter).search(executionContext, query);
            var secondResult = new ApiDocumentSearcher(secondWriter).search(executionContext, query);

            return List.of(List.copyOf(firstResult.getDocuments()), List.copyOf(secondResult.getDocuments()));
        }
    }

    private static void addDocuments(IndexWriter writer, ApiDocumentTransformer transformer, List<ApiEntity> apis) throws Exception {
        for (var api : apis) {
            writer.addDocument(transformer.transform(api));
        }
        writer.commit();
    }

    private static ApiEntity api(String id, String name, String description) {
        var api = new ApiEntity();
        api.setId(id);
        api.setName(name);
        api.setDescription(description);
        api.setLifecycleState(ApiLifecycleState.CREATED);
        api.setVisibility(Visibility.PUBLIC);
        api.setReferenceId(ENVIRONMENT_ID);
        api.setReferenceType(ReferenceContext.Type.ENVIRONMENT.name());
        api.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());

        var owner = new PrimaryOwnerEntity();
        owner.setId("owner-id");
        owner.setDisplayName("Owner");
        api.setPrimaryOwner(owner);
        return api;
    }
}
