/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl.search;

import io.gravitee.management.model.search.Indexable;
import io.gravitee.management.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.management.service.impl.search.lucene.DocumentTransformer;
import io.gravitee.management.service.impl.search.lucene.SearchEngineIndexer;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.repository.exceptions.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SearchEngineServiceImpl implements SearchEngineService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SearchEngineServiceImpl.class);

    @Autowired
    private SearchEngineIndexer indexer;

    @Autowired
    private Collection<DocumentTransformer> transformers;

    @Autowired
    private Collection<DocumentSearcher> searchers;

    @Async
    @Override
    public void index(Indexable source) {
        transformers.stream()
                .filter(transformer -> transformer.handle(source.getClass()))
                .findFirst()
                .ifPresent(transformer -> {
                    try {
                        indexer.index(transformer.transform(source));
                    } catch (TechnicalException te) {
                        logger.error("Unexpected error while indexing a document", te);
                    }
                });
    }

    @Async
    @Override
    public void delete(Indexable source) {
        transformers.stream()
                .filter(transformer -> transformer.handle(source.getClass()))
                .findFirst()
                .ifPresent(transformer -> {
                    try {
                        indexer.remove(transformer.transform(source));
                    } catch (TechnicalException te) {
                        logger.error("Unexpected error while deleting a document", te);
                    }
                });
    }

    @Override
    public Collection<String> search(io.gravitee.management.service.search.query.Query<? extends Indexable> query) {
        Optional<Collection<String>> results = searchers.stream()
                .filter(searcher -> searcher.handle(query.getRoot()))
                .findFirst()
                .flatMap(searcher -> {
                    try {
                        List<String> ids = searcher.search(query)
                                .stream()
                                .distinct()
                                .collect(Collectors.toList());
                        return Optional.of(ids);
                    } catch (TechnicalException te) {
                        logger.error("Unexpected error while deleting a document", te);
                        return Optional.of(Collections.emptyList());
                    }
                });

        return results.get();
    }
}
