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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.Category;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoryRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/category-tests/";
    }

    @Test
    public void shouldFindByKey() throws Exception {
        final Optional<Category> optionalCategory = categoryRepository.findByKey("my-category", "DEFAULT");
        assertTrue(optionalCategory.isPresent());
        assertEquals("123", optionalCategory.get().getId());
    }

    @Test
    public void shouldFindByPage() throws Exception {
        final Set<Category> categories = categoryRepository.findByPage("documentationPageId");
        assertNotNull(categories);
        assertFalse(categories.isEmpty());
        assertEquals(3, categories.size());
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Category> categories = categoryRepository.findAll();

        assertNotNull(categories);
        assertEquals(6, categories.size());
    }

    @Test
    public void shouldFindAllByEnvironment() throws Exception {
        final Set<Category> categories = categoryRepository.findAllByEnvironment("DEFAULT");

        assertNotNull(categories);
        assertEquals(4, categories.size());
    }

    @Test
    public void shouldFindByEnvironmentIdAndIdIn() throws Exception {
        Set<Category> categories = categoryRepository.findByEnvironmentIdAndIdIn("DEFAULT", Set.of("123", "products", "i-do-not-exist"));
        assertNotNull(categories);
        assertEquals(2, categories.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Category category = new Category();
        category.setId("fd19297e-01a3-4828-9929-7e01a3782809");
        category.setKey("new-category");
        category.setEnvironmentId("DEFAULT");
        category.setName("Category name");
        category.setDescription("Description for the new category");
        category.setCreatedAt(new Date(1486771200000L));
        category.setUpdatedAt(new Date(1486771200000L));
        category.setHidden(true);
        category.setOrder(1);
        category.setPicture("New picture");
        category.setBackground("New background");
        category.setPage("documentationPageId");

        int nbCategorysBeforeCreation = categoryRepository.findAll().size();
        categoryRepository.create(category);
        int nbCategorysAfterCreation = categoryRepository.findAll().size();

        Assertions.assertEquals(nbCategorysBeforeCreation + 1, nbCategorysAfterCreation);

        Optional<Category> optional = categoryRepository.findById("fd19297e-01a3-4828-9929-7e01a3782809");
        Assertions.assertTrue(optional.isPresent(), "Category saved not found");

        final Category categorySaved = optional.get();
        Assertions.assertEquals(category.getId(), categorySaved.getId(), "Invalid saved category id.");
        Assertions.assertEquals(category.getKey(), categorySaved.getKey(), "Invalid saved category key.");
        Assertions.assertEquals(category.getEnvironmentId(), categorySaved.getEnvironmentId(), "Invalid saved environment id.");
        Assertions.assertEquals(category.getName(), categorySaved.getName(), "Invalid saved category name.");
        Assertions.assertEquals(category.getDescription(), categorySaved.getDescription(), "Invalid category description.");
        Assertions.assertTrue(compareDate(category.getCreatedAt(), categorySaved.getCreatedAt()), "Invalid category createdAt.");
        Assertions.assertTrue(compareDate(category.getUpdatedAt(), categorySaved.getUpdatedAt()), "Invalid category updatedAt.");
        Assertions.assertEquals(category.isHidden(), categorySaved.isHidden(), "Invalid category hidden.");
        Assertions.assertEquals(category.getOrder(), categorySaved.getOrder(), "Invalid category order.");
        Assertions.assertEquals("New picture", categorySaved.getPicture(), "Invalid category picture.");
        Assertions.assertEquals("New background", categorySaved.getBackground(), "Invalid category background.");
        Assertions.assertEquals("documentationPageId", categorySaved.getPage(), "Invalid category page.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Category> optional = categoryRepository.findById("products");
        Assertions.assertTrue(optional.isPresent(), "Category to update not found");
        Assertions.assertEquals("Products", optional.get().getName(), "Invalid saved category name.");

        final Category category = optional.get();
        category.setName("New product");
        category.setDescription("New description");
        category.setOrder(10);
        category.setHidden(true);
        category.setCreatedAt(new Date(1486771200000L));
        category.setUpdatedAt(new Date(1486771200000L));
        category.setHighlightApi("new Highlighted API");
        category.setPicture("New picture");
        category.setBackground("New background");
        category.setPage("documentationPageId");

        int nbCategorysBeforeUpdate = categoryRepository.findAll().size();
        categoryRepository.update(category);
        int nbCategorysAfterUpdate = categoryRepository.findAll().size();

        Assertions.assertEquals(nbCategorysBeforeUpdate, nbCategorysAfterUpdate);

        Optional<Category> optionalUpdated = categoryRepository.findById("products");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Category to update not found");

        final Category categoryUpdated = optionalUpdated.get();
        Assertions.assertEquals(category.getEnvironmentId(), categoryUpdated.getEnvironmentId(), "Invalid saved environment id.");
        Assertions.assertEquals(category.getName(), categoryUpdated.getName(), "Invalid saved category name.");
        Assertions.assertEquals(category.getDescription(), categoryUpdated.getDescription(), "Invalid category description.");
        Assertions.assertTrue(compareDate(category.getCreatedAt(), categoryUpdated.getCreatedAt()), "Invalid category createdAt.");
        Assertions.assertTrue(compareDate(category.getUpdatedAt(), categoryUpdated.getUpdatedAt()), "Invalid category updatedAt.");
        Assertions.assertEquals(category.isHidden(), categoryUpdated.isHidden(), "Invalid category hidden.");
        Assertions.assertEquals(category.getOrder(), categoryUpdated.getOrder(), "Invalid category order.");
        Assertions.assertEquals(category.getHighlightApi(), categoryUpdated.getHighlightApi(), "Invalid category highlight API.");
        Assertions.assertEquals("New picture", categoryUpdated.getPicture(), "Invalid category picture.");
        Assertions.assertEquals("New background", categoryUpdated.getBackground(), "Invalid category background.");
        Assertions.assertEquals("documentationPageId", categoryUpdated.getPage(), "Invalid category page.");
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbCategorysBeforeDeletion = categoryRepository.findAll().size();
        categoryRepository.delete("international");
        int nbCategorysAfterDeletion = categoryRepository.findAll().size();

        Assertions.assertEquals(nbCategorysBeforeDeletion - 1, nbCategorysAfterDeletion);
    }

    @Test
    public void shouldNotUpdateUnknownCategory() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Category unknownCategory = new Category();
            unknownCategory.setId("unknown");
            categoryRepository.update(unknownCategory);
            fail("An unknown category should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            categoryRepository.update(null);
            fail("A null category should not be updated");
        });
    }

    @Test
    public void should_delete_by_environment_id() throws Exception {
        final var nbBeforeDelete = categoryRepository.findAllByEnvironment("ToBeDeleted").size();

        final var deleted = categoryRepository.deleteByEnvironmentId("ToBeDeleted").size();
        final var nbAfterDelete = categoryRepository.findAllByEnvironment("ToBeDeleted").size();

        assertEquals(2, nbBeforeDelete);
        assertEquals(2, deleted);
        assertEquals(0, nbAfterDelete);
    }
}
