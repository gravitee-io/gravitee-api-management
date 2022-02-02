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

import static org.junit.Assert.assertEquals;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.impl.CategoryServiceImpl;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryService_GetTotalApisByCategoryTest {

    @InjectMocks
    private CategoryServiceImpl categoryService = new CategoryServiceImpl();

    private static Set<ApiEntity> apis;

    @BeforeClass
    public static void init() {
        ApiEntity apiA = new ApiEntity();
        apiA.setId("A");
        apiA.setCategories(new HashSet<>(Arrays.asList("1", "")));
        ApiEntity apiB = new ApiEntity();
        apiB.setId("B");
        apiB.setCategories(new HashSet<>(Arrays.asList("1", "2")));
        ApiEntity apiC = new ApiEntity();
        apiC.setId("C");
        apiC.setCategories(new HashSet<>(Arrays.asList("2", "3")));
        ApiEntity apiD = new ApiEntity();
        apiD.setId("D");
        apiD.setCategories(null);

        apis = new HashSet<>();
        apis.add(apiA);
        apis.add(apiB);
        apis.add(apiC);
        apis.add(apiD);
    }

    @Test
    public void testEnhanceForOneCategory() {
        CategoryEntity v = new CategoryEntity();
        v.setKey("1");

        long totalApisByCategory = categoryService.getTotalApisByCategory(apis, v);

        assertEquals(2, totalApisByCategory);
    }
}
