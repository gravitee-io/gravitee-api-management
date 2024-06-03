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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import io.gravitee.repository.management.model.ApiCategory;
import java.util.Set;
import org.junit.Test;

public class ApiCategoryRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/apicategory-tests/";
    }

    @Override
    protected Class getClassFromFileName(String baseName) {
        return ApiCategory.class;
    }

    @Test
    public void shouldFindAll() throws Exception {
        Set<ApiCategory> all = apiCategoryRepository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    public void shouldThrowErrorOnUpdateIfNoCategoryWithId() {
        assertThrows(
            IllegalStateException.class,
            () ->
                apiCategoryRepository.update(
                    ApiCategory
                        .builder()
                        .id(ApiCategory.Id.builder().categoryId("does-not-exist").apiId("does-not-exist-either").build())
                        .order(0)
                        .categoryKey("a-category-key")
                        .build()
                )
        );
    }

    @Test
    public void shouldUpdateExistingApiCategory() throws Exception {
        var apiCategoryId = ApiCategory.Id.builder().apiId("api-1").categoryId("cat-2").build();

        var updatedCategory = apiCategoryRepository.update(
            ApiCategory.builder().id(apiCategoryId).categoryKey("cat-key-2").order(10).build()
        );

        assertNotNull(updatedCategory);
        assertNotNull(updatedCategory.getId());
        assertEquals("cat-2", updatedCategory.getId().getCategoryId());
        assertEquals("api-1", updatedCategory.getId().getApiId());
        assertEquals("cat-key-2", updatedCategory.getCategoryKey());
        assertEquals(10, updatedCategory.getOrder());

        var persistedCategory = apiCategoryRepository.findById(apiCategoryId).orElse(null);
        assertNotNull(persistedCategory);
        assertNotNull(persistedCategory.getId());
        assertEquals("cat-2", persistedCategory.getId().getCategoryId());
        assertEquals("api-1", persistedCategory.getId().getApiId());
        assertEquals("cat-key-2", persistedCategory.getCategoryKey());
        assertEquals(10, persistedCategory.getOrder());

        assertEquals(3, apiCategoryRepository.findAll().size());
    }

    @Test
    public void shouldCreate() throws Exception {
        var createdCategory = apiCategoryRepository.create(
            ApiCategory
                .builder()
                .id(ApiCategory.Id.builder().apiId("api-3").categoryId("cat-3").build())
                .categoryKey("cat-key-3")
                .order(0)
                .build()
        );
        assertNotNull(createdCategory);
        assertNotNull(createdCategory.getId());
        assertEquals("cat-3", createdCategory.getId().getCategoryId());
        assertEquals("api-3", createdCategory.getId().getApiId());
        assertEquals("cat-key-3", createdCategory.getCategoryKey());
        assertEquals(0, createdCategory.getOrder());

        assertEquals(4, apiCategoryRepository.findAll().size());
    }

    @Test
    public void shouldDelete() throws Exception {
        var apiCategoryId = ApiCategory.Id.builder().apiId("api-1").categoryId("cat-2").build();
        apiCategoryRepository.delete(apiCategoryId);

        var deletedCategory = apiCategoryRepository.findById(apiCategoryId).orElse(null);
        assertNull(deletedCategory);

        assertEquals(3, apiCategoryRepository.findAll().size());
    }
}
