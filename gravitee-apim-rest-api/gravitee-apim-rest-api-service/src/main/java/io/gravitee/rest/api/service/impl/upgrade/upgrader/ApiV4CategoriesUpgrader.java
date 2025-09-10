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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Category;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Before this upgrader runs :
 *  - categories attached to V4 APIs are not ids but keys
 * For each V4 APIS, this upgrader will :
 *  - update its categories list to store ids instead of keys
 *
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiV4CategoriesUpgrader implements Upgrader {

    private final ApiRepository apiRepository;

    private final CategoryRepository categoryRepository;

    @Autowired
    public ApiV4CategoriesUpgrader(@Lazy ApiRepository apiRepository, @Lazy CategoryRepository categoryRepository) {
        this.apiRepository = apiRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.API_V4_CATEGORIES_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::migrateV4ApiCategories);
    }

    private boolean migrateV4ApiCategories() throws TechnicalException {
        Set<Category> categories;
        try {
            categories = categoryRepository.findAll();
        } catch (TechnicalException e) {
            log.error("An error occurred when finding all categories", e);
            throw new TechnicalException(e);
        }

        // If there are no categories, then upgrade is not necessary
        if (Objects.isNull(categories) || categories.isEmpty()) {
            return true;
        }

        // Two different maps so that we can look up the key or the id of a category
        var envByCategoryKeyId = new HashMap<String, Map<String, String>>();
        var categoryIdKeyMap = new HashMap<String, String>();

        categories.forEach(category -> {
            envByCategoryKeyId.computeIfPresent(
                category.getEnvironmentId(),
                (envId, keyIdMap) -> {
                    keyIdMap.put(category.getKey(), category.getId());
                    return keyIdMap;
                }
            );
            envByCategoryKeyId.computeIfAbsent(
                category.getEnvironmentId(),
                envId -> {
                    var keyIdMap = new HashMap<String, String>();
                    keyIdMap.put(category.getKey(), category.getId());
                    return keyIdMap;
                }
            );

            categoryIdKeyMap.put(category.getId(), category.getKey());
        });

        var modelCounter = new AtomicInteger(0);
        apiRepository
            .search(new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V4)).build(), null, ApiFieldFilter.allFields())
            .filter(v4Api -> Objects.nonNull(v4Api.getCategories()) && !v4Api.getCategories().isEmpty())
            .forEach(v4Api -> {
                try {
                    var newCategories = v4Api
                        .getCategories()
                        .stream()
                        .map(category -> {
                            // If the category is a key
                            if (
                                envByCategoryKeyId.containsKey(v4Api.getEnvironmentId()) &&
                                envByCategoryKeyId.get(v4Api.getEnvironmentId()).containsKey(category)
                            ) {
                                return envByCategoryKeyId.get(v4Api.getEnvironmentId()).get(category);
                            }
                            // If the category is an id
                            if (categoryIdKeyMap.containsKey(category)) {
                                return category;
                            }
                            // If not found
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                    v4Api.setCategories(newCategories);
                    apiRepository.update(v4Api);
                    modelCounter.incrementAndGet();
                } catch (Exception e) {
                    log.error("Unable to migrate categories for api {}", v4Api.getId(), e);
                }
            });
        log.info("{} v4 APIs have been migrated to use category ids instead of keys", modelCounter.get());
        return true;
    }
}
