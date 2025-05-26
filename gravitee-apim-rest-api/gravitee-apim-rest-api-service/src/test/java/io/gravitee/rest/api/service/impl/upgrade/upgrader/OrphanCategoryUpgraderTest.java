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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrphanCategoryUpgraderTest {

    @InjectMocks
    @Spy
    private OrphanCategoryUpgrader upgrader = new OrphanCategoryUpgrader();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test(expected = UpgraderException.class)
    public void upgrade_should_failed_because_of_exception() throws TechnicalException, UpgraderException {
        when(categoryRepository.findAll()).thenThrow(new RuntimeException());

        upgrader.upgrade();

        verify(categoryRepository, times(1)).findAll();
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    public void upgrade_should_remove_orphan_categories() throws TechnicalException, UpgraderException {
        final String orphanCategoryId = UuidString.generateRandom();

        Category existingCategory = new Category();
        existingCategory.setId(UuidString.generateRandom());
        when(categoryRepository.findAll()).thenReturn(Set.of(existingCategory));

        Api apiWithOrphanCategory = new Api();
        apiWithOrphanCategory.setCategories(Set.of(orphanCategoryId, existingCategory.getId()));
        when(apiRepository.search(any(ApiCriteria.class), eq(null), any(ApiFieldFilter.class)))
            .thenReturn(Stream.of(apiWithOrphanCategory));

        assertTrue(upgrader.upgrade());

        assertEquals(1, apiWithOrphanCategory.getCategories().size());
        assertFalse(apiWithOrphanCategory.getCategories().contains(orphanCategoryId));
        assertTrue(apiWithOrphanCategory.getCategories().contains(existingCategory.getId()));
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.ORPHAN_CATEGORY_UPGRADER, upgrader.getOrder());
    }
}
