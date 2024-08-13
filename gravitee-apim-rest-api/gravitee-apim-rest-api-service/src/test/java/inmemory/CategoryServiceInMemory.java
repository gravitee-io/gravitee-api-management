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
package inmemory;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.NewCategoryEntity;
import io.gravitee.rest.api.model.UpdateCategoryEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import jakarta.xml.bind.DatatypeConverter;
import java.util.*;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoryServiceInMemory implements CategoryService {

    final List<CategoryEntity> storage;

    public CategoryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public void initWith(List<CategoryEntity> items) {
        storage.clear();
        storage.addAll(items);
    }

    public void reset() {
        storage.clear();
    }

    public List<CategoryEntity> storage() {
        return storage;
    }

    @Override
    public List<CategoryEntity> findAll(String environmentId) {
        return storage;
    }

    @Override
    public Set<CategoryEntity> findByIdIn(String environmentId, Map<String, Integer> ids) {
        return new HashSet<>(storage);
    }

    @Override
    public CategoryEntity findById(String id, String environment) {
        return storage
            .stream()
            .filter(category -> category.getId().equals(id) || category.getKey().equals(id))
            .findFirst()
            .map(category -> {
                var entity = new CategoryEntity();
                entity.setId(category.getId());
                return entity;
            })
            .orElse(null);
    }

    @Override
    public CategoryEntity findNotHiddenById(String id, String environmentId) {
        CategoryEntity categoryEntity = storage.stream().filter(category -> category.getId().equals(id)).findFirst().orElse(null);

        if (!categoryEntity.isHidden()) {
            return categoryEntity;
        }
        throw new CategoryNotFoundException(id);
    }

    @Override
    public CategoryEntity create(ExecutionContext executionContext, NewCategoryEntity category) {
        return null;
    }

    @Override
    public CategoryEntity update(ExecutionContext executionContext, String categoryId, UpdateCategoryEntity category) {
        return null;
    }

    @Override
    public List<CategoryEntity> update(ExecutionContext executionContext, List<UpdateCategoryEntity> categories) {
        return List.of();
    }

    @Override
    public void delete(ExecutionContext executionContext, String categoryId) {}

    @Override
    public InlinePictureEntity getPicture(String environmentId, String categoryId) {
        return null;
    }

    @Override
    public InlinePictureEntity getBackground(String environmentId, String categoryId) {
        CategoryEntity categoryEntity = storage.stream().filter(category -> category.getId().equals(categoryId)).findFirst().orElse(null);

        InlinePictureEntity imageEntity = new InlinePictureEntity();
        if (categoryEntity.getBackground() != null) {
            String[] parts = categoryEntity.getBackground().split(";", 2);
            imageEntity.setType(parts[0].split(":")[1]);
            String base64Content = categoryEntity.getBackground().split(",", 2)[1];
            imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
        }
        return imageEntity;
    }

    @Override
    public List<CategoryEntity> findByPage(String pageId) {
        return storage.stream().filter(category -> category.getPage().equals(pageId)).toList();
    }
}
