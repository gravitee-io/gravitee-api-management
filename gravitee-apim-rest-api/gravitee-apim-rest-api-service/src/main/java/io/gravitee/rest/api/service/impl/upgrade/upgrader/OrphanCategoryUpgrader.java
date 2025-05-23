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

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class OrphanCategoryUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public int getOrder() {
        return UpgraderOrder.ORPHAN_CATEGORY_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
                Set<Api> updatedApis = findAndFixApisWithOrphanCategories();
                for (Api api : updatedApis) {
                    log.info("Removing orphan categories for API [{}]", api.getId());
                    apiRepository.update(api);
                }
                return true;
            });
    }

    private Set<Api> findAndFixApisWithOrphanCategories() throws TechnicalException {
        Set<String> existingCategoryIds = getExistingCategoryIds();

        return apiRepository
            .search(new ApiCriteria.Builder().build(), null, ApiFieldFilter.allFields())
            .filter(api -> hasOrphanCategories(api, existingCategoryIds))
            .peek(api -> removeOrphanCategories(api, existingCategoryIds))
            .collect(Collectors.toSet());
    }

    private void removeOrphanCategories(Api api, Set<String> existingCategoryIds) {
        HashSet<String> updatedCategories = new HashSet<>(api.getCategories());
        updatedCategories.retainAll(existingCategoryIds);
        api.setCategories(updatedCategories);
    }

    private boolean hasOrphanCategories(Api api, Set<String> existingCategoryIds) {
        if (CollectionUtils.isEmpty(api.getCategories())) {
            return false;
        }
        HashSet<String> orphanCategories = new HashSet<>(api.getCategories());
        orphanCategories.removeAll(existingCategoryIds);
        return !orphanCategories.isEmpty();
    }

    private Set<String> getExistingCategoryIds() throws TechnicalException {
        return categoryRepository.findAll().stream().map(Category::getId).collect(Collectors.toSet());
    }
}
