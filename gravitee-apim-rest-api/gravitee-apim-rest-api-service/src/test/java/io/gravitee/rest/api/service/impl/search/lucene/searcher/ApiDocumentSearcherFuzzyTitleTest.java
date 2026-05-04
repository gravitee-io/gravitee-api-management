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
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import java.util.Date;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.Test;

/**
 * Fuzzy match on tokenized {@code name_split} (same analyzer as production {@link org.apache.lucene.analysis.standard.StandardAnalyzer})
 * so titles like {@code management #1} match a one-letter typo in {@code management}.
 */
class ApiDocumentSearcherFuzzyTitleTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-fuzzy-title";

    @Test
    void should_match_typo_against_name_token_when_title_has_suffix() throws Exception {
        try (
            ByteBuffersDirectory directory = new ByteBuffersDirectory();
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))
        ) {
            ApiDocumentTransformer transformer = new ApiDocumentTransformer(mock(ApiService.class));
            indexWriter.addDocument(transformer.transform(apiNamed("management #1")));
            indexWriter.commit();

            ApiDocumentSearcher searcher = new ApiDocumentSearcher(indexWriter);
            ExecutionContext ctx = new ExecutionContext(ORG, ENV);

            SearchResult withoutTypo = searcher.search(
                ctx,
                QueryBuilder.create(ApiEntity.class).setQuery("managment").setTypoTolerance(false).build()
            );
            assertThat(withoutTypo.getHits()).isZero();

            SearchResult withTypo = searcher.search(
                ctx,
                QueryBuilder.create(ApiEntity.class).setQuery("managment").setTypoTolerance(true).build()
            );
            assertThat(withTypo.getHits()).isEqualTo(1);
            assertThat(withTypo.getDocuments()).containsExactly("api-fuzzy-title");
        }
    }

    private static ApiEntity apiNamed(String name) {
        ApiEntity api = new ApiEntity();
        api.setId("api-fuzzy-title");
        api.setName(name);
        api.setLifecycleState(ApiLifecycleState.CREATED);
        api.setVisibility(Visibility.PUBLIC);
        api.setReferenceId(ENV);
        api.setReferenceType(ReferenceContext.Type.ENVIRONMENT.name());
        api.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        api.setUpdatedAt(new Date());
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity();
        owner.setId("owner-1");
        owner.setDisplayName("Owner");
        api.setPrimaryOwner(owner);
        return api;
    }
}
