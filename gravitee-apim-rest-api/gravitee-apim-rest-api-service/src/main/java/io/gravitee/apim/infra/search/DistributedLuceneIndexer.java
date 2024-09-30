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

import io.gravitee.apim.core.search.Indexer;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class DistributedLuceneIndexer implements Indexer {

    private final SearchEngineService searchEngineService;

    public DistributedLuceneIndexer(@Lazy SearchEngineService searchEngineService) {
        this.searchEngineService = searchEngineService;
    }

    @Override
    public void index(IndexationContext context, Indexable indexable) {
        var executionContext = new ExecutionContext(context.organizationId(), context.environmentId());
        searchEngineService.index(executionContext, indexable, false, context.autoCommit());
    }

    @Override
    public void delete(IndexationContext context, Indexable indexable) {
        var executionContext = new ExecutionContext(context.organizationId(), context.environmentId());
        searchEngineService.delete(executionContext, indexable);
    }

    @Override
    public void commit() {
        searchEngineService.commit();
    }
}
