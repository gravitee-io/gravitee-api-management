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
package io.gravitee.apim.infra.query_service.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiCategoryOrderQueryServiceImplTest {

    private static final String CAT_ID = "cat-id";

    @Mock
    ApiCategoryOrderRepository apiCategoryOrderRepository;

    ApiCategoryOrderQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiCategoryOrderQueryServiceImpl(apiCategoryOrderRepository);
    }

    @Nested
    class FindAllByCategoryId {

        @Test
        void should_return_list() {
            when(apiCategoryOrderRepository.findAllByCategoryId(CAT_ID)).thenReturn(
                Set.of(
                    ApiCategoryOrder.builder().categoryId(CAT_ID).apiId("api-1").order(0).build(),
                    ApiCategoryOrder.builder().categoryId(CAT_ID).apiId("api-2").order(1).build()
                )
            );

            var foundApiCategories = service.findAllByCategoryId(CAT_ID);

            verify(apiCategoryOrderRepository, times(1)).findAllByCategoryId(CAT_ID);
            Assertions.assertThat(foundApiCategories)
                .isNotNull()
                .hasSize(2)
                .usingRecursiveComparison()
                .isEqualTo(
                    Set.of(
                        io.gravitee.apim.core.category.model.ApiCategoryOrder.builder().categoryId(CAT_ID).apiId("api-1").order(0).build(),
                        io.gravitee.apim.core.category.model.ApiCategoryOrder.builder().categoryId(CAT_ID).apiId("api-2").order(1).build()
                    )
                );
        }

        @Test
        void should_return_empty_list() {
            when(apiCategoryOrderRepository.findAllByCategoryId(CAT_ID)).thenReturn(Set.of());

            var foundApiCategories = service.findAllByCategoryId(CAT_ID);

            verify(apiCategoryOrderRepository, times(1)).findAllByCategoryId(CAT_ID);
            Assertions.assertThat(foundApiCategories).isEmpty();
        }

        @Test
        void should_throw_when_validation_fails() {
            doThrow(new RuntimeException("error")).when(apiCategoryOrderRepository).findAllByCategoryId(any());

            var throwable = Assertions.catchThrowable(() -> service.findAllByCategoryId(CAT_ID));

            Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
        }
    }
}
