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
package io.gravitee.apim.infra.query_service.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiDocumentTransformer;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterApiSearchQueryServiceImplTest {

    private static final String ENV_ID = "env-id";

    private final IndexableApiDocumentTransformer transformer = new IndexableApiDocumentTransformer();

    private IndexWriter indexWriter;
    private ClusterApiSearchQueryServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        indexWriter = new IndexWriter(new ByteBuffersDirectory(), new IndexWriterConfig(new StandardAnalyzer()));
        service = new ClusterApiSearchQueryServiceImpl(indexWriter);
    }

    @AfterEach
    void tearDown() throws IOException {
        indexWriter.close();
    }

    @Test
    void should_find_the_apis_using_one_of_the_clusters_sorted_by_name() throws IOException {
        index(
            ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, "prod", "sasl"),
            ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, "prod", "plain"),
            ApiFixtures.aNativeApiBoundToVirtualCluster("audit", ENV_ID, Api.LifecycleState.STARTED, "mesh"),
            ApiFixtures.aNativeApiBoundToCluster("other", ENV_ID, Api.LifecycleState.STARTED, "staging", "plain"),
            ApiFixtures.aNativeApiBoundToCluster("elsewhere", "other-env", Api.LifecycleState.STARTED, "prod", "plain")
        );

        var page = service.search(ENV_ID, List.of("prod", "mesh"), null, new PageableImpl(1, 10));

        assertThat(page.getContent()).containsExactly("audit", "orders", "payments");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void should_return_the_requested_page_with_the_total() throws IOException {
        index(
            ApiFixtures.aNativeApiBoundToCluster("a", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain"),
            ApiFixtures.aNativeApiBoundToCluster("b", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain"),
            ApiFixtures.aNativeApiBoundToCluster("c", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain"),
            ApiFixtures.aNativeApiBoundToCluster("d", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain"),
            ApiFixtures.aNativeApiBoundToCluster("e", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain")
        );

        var page = service.search(ENV_ID, List.of("prod"), null, new PageableImpl(2, 2));

        assertThat(page.getContent()).containsExactly("c", "d");
        assertThat(page.getPageNumber()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    void should_return_an_empty_page_past_the_last_one() throws IOException {
        index(ApiFixtures.aNativeApiBoundToCluster("a", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain"));

        var page = service.search(ENV_ID, List.of("prod"), null, new PageableImpl(3, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void should_sort_the_apis_with_the_same_name_by_id() throws IOException {
        index(
            ApiFixtures.aNativeApiBoundToCluster("b", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain")
                .toBuilder()
                .name("Orders")
                .build(),
            ApiFixtures.aNativeApiBoundToCluster("a", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain")
                .toBuilder()
                .name("Orders")
                .build()
        );

        assertThat(service.search(ENV_ID, List.of("prod"), null, new PageableImpl(1, 10)).getContent()).containsExactly("a", "b");
    }

    @Test
    void should_narrow_the_apis_to_the_names_containing_the_query_ignoring_case() throws IOException {
        index(
            ApiFixtures.aNativeApiBoundToCluster("1", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain")
                .toBuilder()
                .name("Orders Events")
                .build(),
            ApiFixtures.aNativeApiBoundToCluster("2", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain")
                .toBuilder()
                .name("Payments")
                .build(),
            ApiFixtures.aNativeApiBoundToCluster("3", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain")
                .toBuilder()
                .name("Pre-orders*")
                .build()
        );

        assertThat(service.search(ENV_ID, List.of("prod"), " ORDERS ", new PageableImpl(1, 10)).getContent()).containsExactly("1", "3");
        assertThat(service.search(ENV_ID, List.of("prod"), "rs*", new PageableImpl(1, 10)).getContent()).containsExactly("3");
    }

    @Test
    void should_see_a_write_not_committed_yet() throws IOException {
        // The search engine indexes without committing: the reader must see the writer's pending changes.
        indexWriter.addDocument(
            transformer.transform(
                new IndexableApi(
                    ApiFixtures.aNativeApiBoundToCluster("a", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain"),
                    null,
                    null,
                    null
                )
            )
        );

        assertThat(service.search(ENV_ID, List.of("prod"), null, new PageableImpl(1, 10)).getContent()).containsExactly("a");
    }

    @Test
    void should_find_nothing_for_no_cluster() throws IOException {
        index(ApiFixtures.aNativeApiBoundToCluster("a", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain"));

        var page = service.search(ENV_ID, List.of(), null, new PageableImpl(1, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    private void index(Api... apis) throws IOException {
        for (Api api : apis) {
            indexWriter.addDocument(transformer.transform(new IndexableApi(api, null, null, null)));
        }
        indexWriter.commit();
    }
}
