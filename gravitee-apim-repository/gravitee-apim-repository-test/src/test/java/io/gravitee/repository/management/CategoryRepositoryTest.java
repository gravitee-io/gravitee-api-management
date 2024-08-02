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
import static org.junit.Assert.*;

import io.gravitee.repository.management.model.Category;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertEquals(nbCategorysBeforeCreation + 1, nbCategorysAfterCreation);

        Optional<Category> optional = categoryRepository.findById("fd19297e-01a3-4828-9929-7e01a3782809");
        Assert.assertTrue("Category saved not found", optional.isPresent());

        final Category categorySaved = optional.get();
        Assert.assertEquals("Invalid saved category id.", category.getId(), categorySaved.getId());
        Assert.assertEquals("Invalid saved category key.", category.getKey(), categorySaved.getKey());
        Assert.assertEquals("Invalid saved environment id.", category.getEnvironmentId(), categorySaved.getEnvironmentId());
        Assert.assertEquals("Invalid saved category name.", category.getName(), categorySaved.getName());
        Assert.assertEquals("Invalid category description.", category.getDescription(), categorySaved.getDescription());
        Assert.assertTrue("Invalid category createdAt.", compareDate(category.getCreatedAt(), categorySaved.getCreatedAt()));
        Assert.assertTrue("Invalid category updatedAt.", compareDate(category.getUpdatedAt(), categorySaved.getUpdatedAt()));
        Assert.assertEquals("Invalid category hidden.", category.isHidden(), categorySaved.isHidden());
        Assert.assertEquals("Invalid category order.", category.getOrder(), categorySaved.getOrder());
        Assert.assertEquals("Invalid category picture.", "New picture", categorySaved.getPicture());
        Assert.assertEquals("Invalid category background.", "New background", categorySaved.getBackground());
        Assert.assertEquals("Invalid category page.", "documentationPageId", categorySaved.getPage());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Category> optional = categoryRepository.findById("products");
        Assert.assertTrue("Category to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved category name.", "Products", optional.get().getName());

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

        Assert.assertEquals(nbCategorysBeforeUpdate, nbCategorysAfterUpdate);

        Optional<Category> optionalUpdated = categoryRepository.findById("products");
        Assert.assertTrue("Category to update not found", optionalUpdated.isPresent());

        final Category categoryUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved environment id.", category.getEnvironmentId(), categoryUpdated.getEnvironmentId());
        Assert.assertEquals("Invalid saved category name.", category.getName(), categoryUpdated.getName());
        Assert.assertEquals("Invalid category description.", category.getDescription(), categoryUpdated.getDescription());
        Assert.assertTrue("Invalid category createdAt.", compareDate(category.getCreatedAt(), categoryUpdated.getCreatedAt()));
        Assert.assertTrue("Invalid category updatedAt.", compareDate(category.getUpdatedAt(), categoryUpdated.getUpdatedAt()));
        Assert.assertEquals("Invalid category hidden.", category.isHidden(), categoryUpdated.isHidden());
        Assert.assertEquals("Invalid category order.", category.getOrder(), categoryUpdated.getOrder());
        Assert.assertEquals("Invalid category highlight API.", category.getHighlightApi(), categoryUpdated.getHighlightApi());
        Assert.assertEquals("Invalid category picture.", "New picture", categoryUpdated.getPicture());
        Assert.assertEquals("Invalid category background.", "New background", categoryUpdated.getBackground());
        Assert.assertEquals("Invalid category page.", "documentationPageId", categoryUpdated.getPage());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbCategorysBeforeDeletion = categoryRepository.findAll().size();
        categoryRepository.delete("international");
        int nbCategorysAfterDeletion = categoryRepository.findAll().size();

        Assert.assertEquals(nbCategorysBeforeDeletion - 1, nbCategorysAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownCategory() throws Exception {
        Category unknownCategory = new Category();
        unknownCategory.setId("unknown");
        categoryRepository.update(unknownCategory);
        fail("An unknown category should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        categoryRepository.update(null);
        fail("A null category should not be updated");
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
