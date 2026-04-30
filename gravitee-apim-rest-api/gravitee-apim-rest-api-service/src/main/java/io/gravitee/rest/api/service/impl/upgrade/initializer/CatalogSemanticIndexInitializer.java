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

import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.service.catalog.search.CatalogItem;
import io.gravitee.rest.api.service.catalog.search.CatalogSemanticIndexer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final CategoryRepository categoryRepository;

    public CatalogSemanticIndexInitializer(
        CatalogSemanticIndexer catalogSemanticIndexer,
        @Lazy ApiRepository apiRepository,
        @Lazy CategoryRepository categoryRepository
    ) {
        this.catalogSemanticIndexer = catalogSemanticIndexer;
        this.apiRepository = apiRepository;
        this.categoryRepository = categoryRepository;
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

        try (Stream<Api> apiStream = apiRepository.search(criteria, null, ApiFieldFilter.allFields())) {
            apiStream.forEach(api -> {
                var builder = CatalogItem.builder()
                    .id(api.getId())
                    .title(api.getName())
                    .description(api.getDescription() != null ? api.getDescription() : "")
                    .type(api.getType() != null ? api.getType().name().toLowerCase() : "api")
                    .owner(api.getEnvironmentId())
                    .tags(api.getLabels() != null ? api.getLabels() : List.of())
                    .categories(resolveCategoryNames(api));

                enrichFromDefinition(api, builder);
                items.add(builder.build());
            });
        }

        return items;
    }

    /**
     * Resolves API category id/key references to display names for indexing and API responses
     * (same matching rules as {@link io.gravitee.apim.infra.query_service.api.ApiCategoryQueryServiceImpl}).
     */
    private List<String> resolveCategoryNames(Api api) {
        var refs = api.getCategories();
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        try {
            return categoryRepository
                .findAllByEnvironment(api.getEnvironmentId())
                .stream()
                .filter(c -> refs.contains(c.getId()) || refs.contains(c.getKey()))
                .map(Category::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        } catch (TechnicalException e) {
            log.warn("Failed to resolve category names for API '{}': {}", api.getId(), e.getMessage());
            return List.of();
        }
    }

    private void enrichFromDefinition(Api api, CatalogItem.CatalogItemBuilder builder) {
        if (api.getDefinition() == null || api.getDefinitionVersion() == null) {
            return;
        }

        try {
            var mapper = GraviteeJacksonMapper.getInstance();

            if (api.getDefinitionVersion() == DefinitionVersion.V4) {
                if (api.getType() == ApiType.NATIVE) {
                    enrichFromNativeApi(mapper.readValue(api.getDefinition(), NativeApi.class), builder);
                } else {
                    enrichFromV4Api(mapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class), builder);
                }
            } else if (api.getDefinitionVersion() == DefinitionVersion.V2) {
                enrichFromV2Api(mapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class), builder);
            }
        } catch (IOException e) {
            log.warn("Failed to parse API definition for '{}', indexing with basic fields only: {}", api.getId(), e.getMessage());
        }
    }

    private void enrichFromV4Api(io.gravitee.definition.model.v4.Api v4Api, CatalogItem.CatalogItemBuilder builder) {
        if (v4Api == null) return;

        var paths = new ArrayList<String>();
        var entrypointTypes = new ArrayList<String>();
        var listenerTypes = new ArrayList<String>();
        var endpointTypes = new ArrayList<String>();

        if (v4Api.getListeners() != null) {
            for (var listener : v4Api.getListeners()) {
                if (listener.getType() != null) {
                    listenerTypes.add(listener.getType().getLabel());
                }
                if (
                    listener.getType() == ListenerType.HTTP &&
                    listener instanceof HttpListener httpListener &&
                    httpListener.getPaths() != null
                ) {
                    httpListener.getPaths().forEach(p -> paths.add(p.getPath()));
                }
                if (listener.getEntrypoints() != null) {
                    listener.getEntrypoints().forEach(ep -> entrypointTypes.add(ep.getType()));
                }
            }
        }

        if (v4Api.getEndpointGroups() != null) {
            v4Api.getEndpointGroups().forEach(eg -> endpointTypes.add(eg.getType()));
        }

        builder.paths(paths).entrypointTypes(entrypointTypes).listenerTypes(listenerTypes).endpointTypes(endpointTypes);
    }

    private void enrichFromNativeApi(NativeApi nativeApi, CatalogItem.CatalogItemBuilder builder) {
        if (nativeApi == null) return;

        var entrypointTypes = new ArrayList<String>();
        var listenerTypes = new ArrayList<String>();
        var endpointTypes = new ArrayList<String>();

        if (nativeApi.getListeners() != null) {
            for (var listener : nativeApi.getListeners()) {
                if (listener.getType() != null) {
                    listenerTypes.add(listener.getType().getLabel());
                }
                if (listener.getEntrypoints() != null) {
                    listener.getEntrypoints().forEach(ep -> entrypointTypes.add(ep.getType()));
                }
            }
        }

        if (nativeApi.getEndpointGroups() != null) {
            nativeApi.getEndpointGroups().forEach(eg -> endpointTypes.add(eg.getType()));
        }

        builder.paths(List.of()).entrypointTypes(entrypointTypes).listenerTypes(listenerTypes).endpointTypes(endpointTypes);
    }

    private void enrichFromV2Api(io.gravitee.definition.model.Api v2Api, CatalogItem.CatalogItemBuilder builder) {
        if (v2Api == null || v2Api.getProxy() == null) return;

        var paths = new ArrayList<String>();
        var endpointTypes = new ArrayList<String>();

        var proxy = v2Api.getProxy();
        if (proxy.getVirtualHosts() != null) {
            proxy.getVirtualHosts().forEach(vh -> paths.add(vh.getPath()));
        }

        if (proxy.getGroups() != null) {
            proxy
                .getGroups()
                .forEach(eg -> {
                    if (eg.getEndpoints() != null) {
                        eg
                            .getEndpoints()
                            .forEach(ep -> {
                                if (ep.getType() != null) {
                                    endpointTypes.add(ep.getType());
                                }
                            });
                    }
                });
        }

        builder.paths(paths).entrypointTypes(List.of()).listenerTypes(List.of("http")).endpointTypes(endpointTypes);
    }
}
