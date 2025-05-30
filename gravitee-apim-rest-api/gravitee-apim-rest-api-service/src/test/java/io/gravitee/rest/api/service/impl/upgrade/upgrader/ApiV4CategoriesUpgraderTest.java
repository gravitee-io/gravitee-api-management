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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
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
public class ApiV4CategoriesUpgraderTest {

    @InjectMocks
    private ApiV4CategoriesUpgrader apiV4CategoriesUpgrader;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    public void shouldMigrateV4ApiCategoriesWhenCategoriesExist() throws Exception {
        Api api = new Api();
        api.setCategories(new HashSet<>(Arrays.asList("category1", "category2")));
        api.setEnvironmentId("env1");
        api.setDefinitionVersion(DefinitionVersion.V4);

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));

        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");

        Category category2 = new Category();
        category2.setKey("category2");
        category2.setId("id2");
        category2.setEnvironmentId("env1");

        when(categoryRepository.findAll()).thenReturn(Set.of(category1, category2));

        apiV4CategoriesUpgrader.upgrade();

        verify(apiRepository, times(1)).update(argThat(currentApi -> currentApi.getCategories().containsAll(List.of("id1", "id2"))));
    }

    @Test
    public void shouldNotMigrateV4ApiCategoriesWhenNoV4ApisWithCategoriesExist() throws Exception {
        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");

        when(categoryRepository.findAll()).thenReturn(Set.of(category1));
        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.empty());

        apiV4CategoriesUpgrader.upgrade();

        verify(apiRepository, never()).update(any());
    }

    @Test
    public void shouldNotMigrateV4ApiCategoriesWhenNoCategoriesExistInCategoriesTable() throws Exception {
        Api api = new Api();
        api.setCategories(new HashSet<>(Arrays.asList("category1", "category2")));
        api.setEnvironmentId("env1");
        api.setDefinitionVersion(DefinitionVersion.V4);

        when(categoryRepository.findAll()).thenReturn(Set.of());

        apiV4CategoriesUpgrader.upgrade();

        verify(apiRepository, never()).update(any());
    }

    @Test
    public void shouldNotMigrateV4ApiCategoriesWhenCategoriesNull() throws Exception {
        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");

        when(categoryRepository.findAll()).thenReturn(Set.of(category1));
        Api api = new Api();
        api.setCategories(null);
        api.setEnvironmentId("env1");
        api.setDefinitionVersion(DefinitionVersion.V4);

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));

        apiV4CategoriesUpgrader.upgrade();

        verify(apiRepository, never()).update(any());
    }

    @Test(expected = UpgraderException.class)
    public void shouldReturnFalseWhenExceptionOccursDuringUpgrade() throws Exception {
        when(categoryRepository.findAll()).thenThrow(new RuntimeException());
        apiV4CategoriesUpgrader.upgrade();
    }

    @Test
    public void shouldMapCategoryIfSavedAsIdBeforeMigration() throws Exception {
        Api api = new Api();
        api.setCategories(new HashSet<>(Arrays.asList("id1", "category2")));
        api.setEnvironmentId("env1");
        api.setDefinitionVersion(DefinitionVersion.V4);

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));

        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");

        Category category2 = new Category();
        category2.setKey("category2");
        category2.setId("id2");
        category2.setEnvironmentId("env1");

        when(categoryRepository.findAll()).thenReturn(Set.of(category1, category2));

        apiV4CategoriesUpgrader.upgrade();

        verify(apiRepository, times(1)).update(argThat(currentApi -> currentApi.getCategories().containsAll(List.of("id1", "id2"))));
    }

    @Test
    public void shouldRemoveCategoriesThatDoNotExist() throws Exception {
        Api api = new Api();
        api.setCategories(new HashSet<>(Arrays.asList("id1", "category2", "does-not-exist")));
        api.setEnvironmentId("env1");
        api.setDefinitionVersion(DefinitionVersion.V4);

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));

        Category category1 = new Category();
        category1.setKey("category1");
        category1.setId("id1");
        category1.setEnvironmentId("env1");

        Category category2 = new Category();
        category2.setKey("category2");
        category2.setId("id2");
        category2.setEnvironmentId("env1");

        when(categoryRepository.findAll()).thenReturn(Set.of(category1, category2));

        apiV4CategoriesUpgrader.upgrade();

        verify(apiRepository, times(1)).update(argThat(currentApi -> currentApi.getCategories().containsAll(List.of("id1", "id2"))));
    }

    @Test
    public void shouldReturnTrueWhenNoCategoriesExist() throws Exception {
        when(categoryRepository.findAll()).thenReturn(null);

        boolean result = apiV4CategoriesUpgrader.upgrade();

        verify(apiRepository, never()).search(any(), any(), any());
        verify(apiRepository, never()).update(any());
        assert (result);
    }
}
