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

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import java.io.IOException;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiProductDocumentSearcherTest {

    private static final String ENV_1 = "env-1";
    private static final String ENV_2 = "env-2";
    private static final String ORG_1 = "org-1";

    private IndexWriter indexWriter;
    private IndexableApiProductDocumentTransformer transformer;
    private ApiProductDocumentSearcher searcher;

    @BeforeEach
    void setUp() throws IOException {
        ByteBuffersDirectory directory = new ByteBuffersDirectory();
        indexWriter = new IndexWriter(directory, new IndexWriterConfig());
        transformer = new IndexableApiProductDocumentTransformer();
        searcher = new ApiProductDocumentSearcher(indexWriter);
    }

    @Test
    void should_handle_indexable_api_product() {
        assertThat(searcher.handle(IndexableApiProduct.class)).isTrue();
        assertThat(searcher.handle(Indexable.class)).isFalse();
    }

    @Test
    void should_scope_search_by_environment() throws Exception {
        indexApiProduct("product-1", "Product One", ENV_1);
        indexApiProduct("product-2", "Product Two", ENV_2);
        indexWriter.commit();

        ExecutionContext executionContext = new ExecutionContext(ORG_1, ENV_1);
        var query = QueryBuilder.create(IndexableApiProduct.class).build();

        var result = searcher.search(executionContext, query);

        assertThat(result.getDocuments()).containsExactly("product-1");
        assertThat(result.getHits()).isEqualTo(1);
    }

    @Test
    void should_filter_by_ids_when_ids_provided_via_filter() throws Exception {
        indexApiProduct("product-1", "Product One", ENV_1);
        indexApiProduct("product-2", "Product Two", ENV_1);
        indexApiProduct("product-3", "Product Three", ENV_1);
        indexWriter.commit();

        ExecutionContext executionContext = new ExecutionContext(ORG_1, ENV_1);
        var query = QueryBuilder.create(IndexableApiProduct.class)
            .setQuery(null)
            .addFilter(IndexableApiProductDocumentTransformer.FIELD_TYPE_VALUE, Set.of("product-1", "product-3"))
            .build();

        var result = searcher.search(executionContext, query);

        assertThat(result.getDocuments()).containsExactlyInAnyOrder("product-1", "product-3");
        assertThat(result.getHits()).isEqualTo(2);
    }

    @Test
    void should_search_by_text_query() throws Exception {
        indexApiProduct("product-1", "My API Product", ENV_1);
        indexApiProduct("product-2", "Other Product", ENV_1);
        indexWriter.commit();

        ExecutionContext executionContext = new ExecutionContext(ORG_1, ENV_1);
        var query = QueryBuilder.create(IndexableApiProduct.class).setQuery("API").build();

        var result = searcher.search(executionContext, query);

        assertThat(result.getDocuments()).contains("product-1");
    }

    private void indexApiProduct(String id, String name, String environmentId) throws IOException {
        ApiProduct apiProduct = ApiProduct.builder().id(id).environmentId(environmentId).name(name).build();
        IndexableApiProduct indexable = IndexableApiProduct.builder().apiProduct(apiProduct).build();
        Document doc = transformer.transform(indexable);
        indexWriter.addDocument(doc);
    }
}
