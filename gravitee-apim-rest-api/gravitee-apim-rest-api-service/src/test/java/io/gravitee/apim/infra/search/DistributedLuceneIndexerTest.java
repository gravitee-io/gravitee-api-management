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
package io.gravitee.apim.infra.search;

import static org.mockito.Mockito.*;

import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexablePage;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class DistributedLuceneIndexerTest {

    private static final String CONTEXT_ORG = "GRAVITEE";
    private static final String CONTEXT_ENV = "TEST";

    private final SearchEngineService searchEngineService = mock(SearchEngineService.class);
    private DistributedLuceneIndexer cut;

    @BeforeEach
    void setUp() {
        reset(searchEngineService);
        cut = new DistributedLuceneIndexer(searchEngineService);
    }

    @Test
    void should_call_index_with_execution_context_and_distribute_across_node() {
        var indexable = mock(Indexable.class);
        cut.index(new Indexer.IndexationContext(CONTEXT_ORG, CONTEXT_ENV), indexable);
        verify(searchEngineService).index(new ExecutionContext(CONTEXT_ORG, CONTEXT_ENV), indexable, false, true);
    }

    @Test
    void should_call_delete_with_execution_context() {
        var indexable = mock(Indexable.class);
        cut.delete(new Indexer.IndexationContext(CONTEXT_ORG, CONTEXT_ENV), indexable);
        verify(searchEngineService).delete(new ExecutionContext(CONTEXT_ORG, CONTEXT_ENV), indexable);
    }
}
