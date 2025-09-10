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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import io.gravitee.repository.management.model.Category;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiCategoryOrderUpgraderTest {

    @InjectMocks
    private ApiCategoryOrderUpgrader apiCategoryOrderUpgrader;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ApiCategoryOrderRepository apiCategoryOrderRepository;

    @Test
    public void shouldCreateOrderEntriesWhenCategoriesExist() throws Exception {
        Api api = new Api();
        api.setCategories(new HashSet<>(Arrays.asList("id1", "id2")));
        api.setEnvironmentId("env1");
        api.setDefinitionVersion(DefinitionVersion.V4);

        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");

        Category category2 = new Category();
        category2.setKey("category2");
        category2.setId("id2");
        category2.setEnvironmentId("env1");

        when(categoryRepository.findAll()).thenReturn(Set.of(category1, category2));
        when(apiRepository.search(any(), any())).thenReturn(List.of(api));

        apiCategoryOrderUpgrader.upgrade();

        var catOrder1 = ApiCategoryOrder.builder().order(0).apiId(api.getId()).categoryId(category1.getId()).build();
        var catOrder2 = ApiCategoryOrder.builder().order(0).apiId(api.getId()).categoryId(category2.getId()).build();

        verify(apiCategoryOrderRepository, times(1)).create(eq(catOrder1));
        verify(apiCategoryOrderRepository, times(1)).create(eq(catOrder2));
    }

    @Test
    public void shouldNotCreateOrderEntriesWhenNoCategoriesExist() throws Exception {
        when(categoryRepository.findAll()).thenReturn(Set.of());

        apiCategoryOrderUpgrader.upgrade();

        verify(apiCategoryOrderRepository, never()).create(any());
    }

    @Test
    public void shouldNotCreateOrderEntriesWhenNoApisHaveCategories() throws Exception {
        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");

        when(categoryRepository.findAll()).thenReturn(Set.of(category1));

        when(apiRepository.search(any(), any())).thenReturn(List.of());

        apiCategoryOrderUpgrader.upgrade();

        verify(apiCategoryOrderRepository, never()).create(any());
    }

    @Test
    public void shouldIncrementOrder() throws Exception {
        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");

        Api api1 = new Api();
        api1.setId("api1");
        api1.setCategories(new HashSet<>(Arrays.asList("category1")));
        api1.setEnvironmentId("env1");
        api1.setDefinitionVersion(DefinitionVersion.V4);

        Api api2 = new Api();
        api2.setId("api2");
        api2.setCategories(new HashSet<>(Arrays.asList("category1")));
        api2.setEnvironmentId("env1");
        api2.setDefinitionVersion(DefinitionVersion.V4);

        when(apiRepository.search(any(), any())).thenReturn(List.of(api1, api2));
        when(categoryRepository.findAll()).thenReturn(Set.of(category1));

        apiCategoryOrderUpgrader.upgrade();

        var catOrder1 = ApiCategoryOrder.builder().order(0).apiId(api1.getId()).categoryId(category1.getId()).build();
        var catOrder2 = ApiCategoryOrder.builder().order(1).apiId(api2.getId()).categoryId(category1.getId()).build();

        verify(apiCategoryOrderRepository, times(1)).create(eq(catOrder1));
        verify(apiCategoryOrderRepository, times(1)).create(eq(catOrder2));
    }

    @Test(expected = UpgraderException.class)
    public void shouldReturnFalseWhenExceptionOccursDuringUpgrade() throws TechnicalException, UpgraderException {
        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");
        when(categoryRepository.findAll()).thenReturn(Set.of(category1));

        when(apiRepository.search(any(), any())).thenThrow(new RuntimeException());
        apiCategoryOrderUpgrader.upgrade();
    }
}
