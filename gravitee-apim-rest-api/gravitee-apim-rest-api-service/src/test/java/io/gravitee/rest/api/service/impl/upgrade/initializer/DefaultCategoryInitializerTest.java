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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultCategoryInitializerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private final DefaultCategoryInitializer initializer = new DefaultCategoryInitializer();

    @Test
    public void shouldAddKeyAndCreationDate() throws TechnicalException {
        Category category = new Category();
        category.setName("products");
        when(categoryRepository.findAll()).thenReturn(Set.of(category));
        initializer.initialize();
        verify(categoryRepository, times(1)).update(argThat(cat -> cat.getCreatedAt() != null && cat.getKey() != null));
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(InitializerOrder.DEFAULT_CATEGORY_INITIALIZER, initializer.getOrder());
    }
}
