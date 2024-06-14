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
package io.gravitee.apim.infra.crud_service.category;

import static assertions.CoreAssertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.category.exception.ApiAndCategoryNotAssociatedException;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CategoryApiCrudServiceImplTest {

    ApiCategoryOrderRepository apiCategoryOrderRepository;
    CategoryApiCrudServiceImpl service;

    private static final String API_ID = "api-id";
    private static final String CAT_ID = "cat-id";

    @BeforeEach
    void setUp() {
        apiCategoryOrderRepository = mock(ApiCategoryOrderRepository.class);
        service = new CategoryApiCrudServiceImpl(apiCategoryOrderRepository);
    }

    @Nested
    class Get {

        @Test
        void should_get_existing_api_category_order() {
            var apiCategoryOrder = ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(99).build();

            when(apiCategoryOrderRepository.findById(eq(API_ID), eq(CAT_ID))).thenReturn(Optional.of(apiCategoryOrder));

            var result = service.get(API_ID, CAT_ID);

            assertThat(result)
                .isNotNull()
                .satisfies(res -> {
                    Assertions.assertEquals(API_ID, res.getApiId());
                    Assertions.assertEquals(CAT_ID, res.getCategoryId());
                    Assertions.assertEquals(99, res.getOrder());
                });
        }

        @Test
        void should_throw_exception_when_does_not_exist() {
            when(apiCategoryOrderRepository.findById(eq(API_ID), eq(CAT_ID))).thenReturn(Optional.empty());

            Assertions.assertThrows(ApiAndCategoryNotAssociatedException.class, () -> service.get(API_ID, CAT_ID));
        }

        @Test
        void should_throw_exception_when_category_id_null() {
            Assertions.assertThrows(ApiAndCategoryNotAssociatedException.class, () -> service.get(API_ID, null));
        }

        @Test
        void should_throw_exception_when_api_id_null() {
            Assertions.assertThrows(ApiAndCategoryNotAssociatedException.class, () -> service.get(null, CAT_ID));
        }
    }
}
