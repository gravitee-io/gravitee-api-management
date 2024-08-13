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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.NewCategoryEntity;
import io.gravitee.rest.api.model.UpdateCategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface CategoryService {
    List<CategoryEntity> findAll(final String environmentId);
    Set<CategoryEntity> findByIdIn(final String environmentId, final Map<String, Integer> distinctCategories);
    CategoryEntity findById(String id, final String environment);
    CategoryEntity findNotHiddenById(String id, final String environmentId);
    CategoryEntity create(ExecutionContext executionContext, NewCategoryEntity category);
    CategoryEntity update(ExecutionContext executionContext, String categoryId, UpdateCategoryEntity category);
    List<CategoryEntity> update(ExecutionContext executionContext, List<UpdateCategoryEntity> categories);
    void delete(ExecutionContext executionContext, String categoryId);
    InlinePictureEntity getPicture(final String environmentId, String categoryId);
    InlinePictureEntity getBackground(final String environmentId, String categoryId);
    List<CategoryEntity> findByPage(String pageId);
}
