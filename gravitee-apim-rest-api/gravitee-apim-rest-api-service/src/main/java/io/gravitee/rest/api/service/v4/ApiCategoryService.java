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
package io.gravitee.rest.api.service.v4;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiCategoryService {
    Set<CategoryEntity> listCategories(Collection<String> apis, String environment);

    void deleteCategoryFromAPIs(ExecutionContext executionContext, String categoryId);

    ToLongFunction<String> countApisPublishedGroupedByCategoriesForUser(String userId);

    void addApiToCategories(String apiId, Set<String> categoryId);
    void changeApiOrderInCategory(String apiId, String categoryId, int nextOrder);
    void updateApiCategories(String apiId, Set<String> categoryIds);
    void deleteApiFromCategories(String apiId);
}
