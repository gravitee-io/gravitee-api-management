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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.rest.api.service.catalog.search.CatalogItem;
import io.gravitee.rest.api.service.catalog.search.CatalogSemanticIndexer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Fills the isolated catalog Lucene index from the management repository.
 * <p>
 * Implemented as a Gravitee {@link Initializer} (not {@code SmartLifecycle}) so it runs
 * in the same phase as {@link SearchIndexInitializer}, <strong>after</strong> repository
 * beans (e.g. {@link ApiRepository}) are registered by the storage plugin. Running earlier
 * during context {@code SmartLifecycle#start} caused {@code ApiRepository} to be missing.
 */
@CustomLog
@Component
public class CatalogSemanticIndexInitializer implements Initializer {

    private final CatalogSemanticIndexer catalogSemanticIndexer;
    private final ApiRepository apiRepository;

    public CatalogSemanticIndexInitializer(CatalogSemanticIndexer catalogSemanticIndexer, @Lazy ApiRepository apiRepository) {
        this.catalogSemanticIndexer = catalogSemanticIndexer;
        this.apiRepository = apiRepository;
    }

    @Override
    public boolean initialize() {
        try {
            List<CatalogItem> items = loadApisFromDatabase();
            catalogSemanticIndexer.reindexAll(items);
            if (items.isEmpty()) {
                log.warn("No PUBLISHED APIs in the database — catalog semantic index committed empty");
            } else {
                log.info("Catalog semantic index initialized with {} APIs from the database", items.size());
            }
        } catch (Exception e) {
            log.error("Failed to initialize catalog semantic index: {}", e.getMessage(), e);
        }
        return true;
    }

    @Override
    public int getOrder() {
        return InitializerOrder.CATALOG_SEMANTIC_INDEX_INITIALIZER;
    }

    private List<CatalogItem> loadApisFromDatabase() {
        log.info("Loading APIs from the database for catalog semantic indexing...");

        var criteria = new ApiCriteria.Builder().lifecycleStates(List.of(ApiLifecycleState.PUBLISHED)).build();

        var items = new ArrayList<CatalogItem>();

        try (Stream<Api> apiStream = apiRepository.search(criteria, null, ApiFieldFilter.defaultFields())) {
            apiStream.forEach(api -> {
                var item = CatalogItem.builder()
                    .id(api.getId())
                    .title(api.getName())
                    .description(api.getDescription() != null ? api.getDescription() : "")
                    .type(api.getType() != null ? api.getType().name().toLowerCase() : "api")
                    .owner(api.getEnvironmentId())
                    .tags(api.getLabels() != null ? api.getLabels() : List.of())
                    .build();
                items.add(item);
            });
        }

        return items;
    }
}
