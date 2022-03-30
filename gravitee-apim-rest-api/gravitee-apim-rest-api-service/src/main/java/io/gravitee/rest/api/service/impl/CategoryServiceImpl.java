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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.CATEGORY;
import static io.gravitee.repository.management.model.Category.AuditEvent.*;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.NewCategoryEntity;
import io.gravitee.rest.api.model.UpdateCategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import io.gravitee.rest.api.service.exceptions.DuplicateCategoryNameException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CategoryServiceImpl extends TransactionalService implements CategoryService {

    private final Logger LOGGER = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EnvironmentService environmentService;

    @Override
    public List<CategoryEntity> findAll(final String environmentId) {
        try {
            LOGGER.debug("Find all categories");
            return categoryRepository.findAllByEnvironment(environmentId).stream().map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all categories", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all categories", ex);
        }
    }

    @Override
    public Set<CategoryEntity> findByIdIn(String environmentId, Set<String> ids) {
        try {
            return categoryRepository
                .findByEnvironmentIdAndIdIn(environmentId, ids)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to find categories by ids", e);
            throw new TechnicalManagementException("An error occurs while trying to find categories by ids", e);
        }
    }

    @Override
    public List<CategoryEntity> findByPage(String page) {
        try {
            LOGGER.debug("Find all categories by page");
            return categoryRepository.findByPage(page).stream().map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all categories by page", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all categories by page", ex);
        }
    }

    @Override
    public CategoryEntity findById(final String id, final String environmentId) {
        try {
            LOGGER.debug("Find category by id : {}", id);
            Optional<Category> category = categoryRepository.findById(id);
            if (!category.isPresent()) {
                category = categoryRepository.findByKey(id, environmentId);
            }
            if (category.isPresent()) {
                return convert(category.get());
            }
            throw new CategoryNotFoundException(id);
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find a category using its id: " + id;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public CategoryEntity findNotHiddenById(String id, final String environmentId) {
        CategoryEntity category = this.findById(id, environmentId);
        if (!category.isHidden()) {
            return category;
        }
        throw new CategoryNotFoundException(id);
    }

    @Override
    public CategoryEntity create(ExecutionContext executionContext, final String environmentId, NewCategoryEntity newCategory) {
        // First we prevent the duplicate category name
        final List<CategoryEntity> categories = findAll(environmentId);
        final Optional<CategoryEntity> optionalCategory = categories
            .stream()
            .filter(v -> v.getName().equals((newCategory.getName())))
            .findAny();

        if (optionalCategory.isPresent()) {
            throw new DuplicateCategoryNameException(optionalCategory.get().getName());
        }

        try {
            // check if environment exists
            this.environmentService.findById(environmentId);

            Category category = convert(newCategory);
            final Date createdAt = new Date();
            category.setCreatedAt(createdAt);
            category.setUpdatedAt(createdAt);
            category.setEnvironmentId(environmentId);
            category.setOrder(categories.size());
            CategoryEntity createdCategory = convert(categoryRepository.create(category));
            auditService.createEnvironmentAuditLog(
                executionContext,
                environmentId,
                Collections.singletonMap(CATEGORY, category.getId()),
                CATEGORY_CREATED,
                createdAt,
                null,
                category
            );

            return createdCategory;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create category {}", newCategory.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create category " + newCategory.getName(), ex);
        }
    }

    @Override
    public CategoryEntity update(ExecutionContext executionContext, String categoryId, UpdateCategoryEntity categoryEntity) {
        try {
            LOGGER.debug("Update Category {}", categoryId);

            Optional<Category> optCategoryToUpdate = categoryRepository.findById(categoryId);
            if (!optCategoryToUpdate.isPresent()) {
                throw new CategoryNotFoundException(categoryId);
            }

            final Category categoryToUpdate = optCategoryToUpdate.get();
            Category category = convert(categoryEntity, categoryToUpdate.getEnvironmentId());

            // check if picture has been set
            // If no new picture and the current picture url is not the default one, keep the current picture
            if (
                categoryEntity.getPicture() == null &&
                categoryEntity.getPictureUrl() != null &&
                categoryEntity.getPictureUrl().indexOf("?hash") > 0
            ) {
                category.setPicture(categoryToUpdate.getPicture());
            }
            final Date updatedAt = new Date();
            category.setCreatedAt(categoryToUpdate.getCreatedAt());
            category.setUpdatedAt(updatedAt);
            CategoryEntity updatedCategory = convert(categoryRepository.update(category));
            auditService.createEnvironmentAuditLog(
                executionContext,
                categoryToUpdate.getEnvironmentId(),
                Collections.singletonMap(CATEGORY, category.getId()),
                CATEGORY_UPDATED,
                updatedAt,
                categoryToUpdate,
                category
            );

            return updatedCategory;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update category {}", categoryEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update category " + categoryEntity.getName(), ex);
        }
    }

    @Override
    public List<CategoryEntity> update(ExecutionContext executionContext, final List<UpdateCategoryEntity> categoriesEntities) {
        final List<CategoryEntity> savedCategories = new ArrayList<>(categoriesEntities.size());
        categoriesEntities.forEach(
            categoryEntity -> {
                try {
                    Optional<Category> optCategoryToUpdate = categoryRepository.findById(categoryEntity.getId());
                    if (optCategoryToUpdate.isPresent()) {
                        final Category categoryToUpdate = optCategoryToUpdate.get();
                        Category category = convert(categoryEntity, categoryToUpdate.getEnvironmentId());
                        // check if picture has been set
                        if (category.getPicture() == null) {
                            // Picture can not be updated when re-ordering categories
                            category.setPicture(categoryToUpdate.getPicture());
                        }
                        // check if background has been set
                        if (category.getBackground() == null) {
                            // Background can not be updated when re-ordering categories
                            category.setBackground(categoryToUpdate.getBackground());
                        }
                        final Date updatedAt = new Date();
                        category.setCreatedAt(categoryToUpdate.getCreatedAt());
                        category.setUpdatedAt(updatedAt);
                        savedCategories.add(convert(categoryRepository.update(category)));
                        auditService.createEnvironmentAuditLog(
                            executionContext,
                            category.getEnvironmentId(),
                            Collections.singletonMap(CATEGORY, category.getId()),
                            CATEGORY_UPDATED,
                            updatedAt,
                            categoryToUpdate,
                            category
                        );
                    }
                } catch (TechnicalException ex) {
                    LOGGER.error("An error occurs while trying to update category {}", categoryEntity.getName(), ex);
                    throw new TechnicalManagementException(
                        "An error occurs while trying to update category " + categoryEntity.getName(),
                        ex
                    );
                }
            }
        );
        return savedCategories;
    }

    @Override
    public void delete(ExecutionContext executionContext, final String categoryId) {
        try {
            Optional<Category> categoryOptional = categoryRepository.findById(categoryId);
            if (categoryOptional.isPresent()) {
                Category categoryToDelete = categoryOptional.get();
                categoryRepository.delete(categoryToDelete.getId());
                auditService.createEnvironmentAuditLog(
                    executionContext,
                    categoryToDelete.getEnvironmentId(),
                    Collections.singletonMap(CATEGORY, categoryId),
                    CATEGORY_DELETED,
                    new Date(),
                    null,
                    categoryToDelete
                );

                // delete all reference on APIs
                apiService.deleteCategoryFromAPIs(executionContext, categoryId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete category {}", categoryId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete category " + categoryId, ex);
        }
    }

    @Override
    public InlinePictureEntity getPicture(final String environmentId, String categoryId) {
        CategoryEntity categoryEntity = findById(categoryId, environmentId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        if (categoryEntity.getPicture() != null) {
            String[] parts = categoryEntity.getPicture().split(";", 2);
            imageEntity.setType(parts[0].split(":")[1]);
            String base64Content = categoryEntity.getPicture().split(",", 2)[1];
            imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
        }
        return imageEntity;
    }

    @Override
    public InlinePictureEntity getBackground(final String environmentId, String categoryId) {
        CategoryEntity categoryEntity = findById(categoryId, environmentId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        if (categoryEntity.getBackground() != null) {
            String[] parts = categoryEntity.getBackground().split(";", 2);
            imageEntity.setType(parts[0].split(":")[1]);
            String base64Content = categoryEntity.getBackground().split(",", 2)[1];
            imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
        }
        return imageEntity;
    }

    private Category convert(final NewCategoryEntity categoryEntity) {
        final Category category = new Category();
        category.setId(UuidString.generateRandom());
        category.setKey(IdGenerator.generate(categoryEntity.getName()));
        category.setName(categoryEntity.getName());
        category.setDescription(categoryEntity.getDescription());
        category.setOrder(categoryEntity.getOrder());
        category.setHidden(categoryEntity.isHidden());
        category.setHighlightApi(categoryEntity.getHighlightApi());
        category.setPicture(categoryEntity.getPicture());
        category.setBackground(categoryEntity.getBackground());
        category.setPage(categoryEntity.getPage());
        return category;
    }

    private Category convert(final UpdateCategoryEntity categoryEntity, final String environment) {
        final Category category = new Category();
        category.setId(categoryEntity.getId());
        category.setKey(IdGenerator.generate(categoryEntity.getName()));
        category.setEnvironmentId(environment);
        category.setName(categoryEntity.getName());
        category.setDescription(categoryEntity.getDescription());
        category.setOrder(categoryEntity.getOrder());
        category.setHidden(categoryEntity.isHidden());
        category.setHighlightApi(categoryEntity.getHighlightApi());
        category.setPicture(categoryEntity.getPicture());
        category.setBackground(categoryEntity.getBackground());
        category.setPage(categoryEntity.getPage());
        return category;
    }

    private CategoryEntity convert(final Category category) {
        final CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(category.getId());
        categoryEntity.setKey(category.getKey());
        categoryEntity.setName(category.getName());
        categoryEntity.setDescription(category.getDescription());
        categoryEntity.setOrder(category.getOrder());
        categoryEntity.setHidden(category.isHidden());
        categoryEntity.setHighlightApi(category.getHighlightApi());
        categoryEntity.setPicture(category.getPicture());
        categoryEntity.setBackground(category.getBackground());
        categoryEntity.setPage(category.getPage());
        categoryEntity.setUpdatedAt(category.getUpdatedAt());
        categoryEntity.setCreatedAt(category.getCreatedAt());
        return categoryEntity;
    }

    @Override
    public long getTotalApisByCategory(Set<ApiEntity> apis, CategoryEntity category) {
        return apis.stream().filter(api -> api.getCategories() != null && api.getCategories().contains(category.getKey())).count();
    }

    @Override
    public long getTotalApisByCategoryId(Set<Api> apis, String categoryId) {
        return apis.stream().filter(api -> api.getCategories() != null && api.getCategories().contains(categoryId)).count();
    }
}
