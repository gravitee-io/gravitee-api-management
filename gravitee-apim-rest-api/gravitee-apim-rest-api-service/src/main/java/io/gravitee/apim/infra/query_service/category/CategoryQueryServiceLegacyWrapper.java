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
package io.gravitee.apim.infra.query_service.category;

import io.gravitee.apim.core.category.model.Category;
import io.gravitee.apim.core.category.query_service.CategoryQueryService;
import io.gravitee.apim.infra.adapter.CategoryAdapter;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CategoryQueryServiceLegacyWrapper implements CategoryQueryService {

    private final CategoryService categoryService;

    @Override
    public Optional<Category> findByIdOrKey(String idOrKey, String environmentId) {
        try {
            return Optional.of(CategoryAdapter.INSTANCE.toCoreModel(this.categoryService.findById(idOrKey, environmentId)));
        } catch (CategoryNotFoundException e) {
            return Optional.empty();
        }
    }
}
