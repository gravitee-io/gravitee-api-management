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

import io.gravitee.apim.core.category.model.Category;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceLegacyWrapperTest {

    private static final String CAT_ID = "cat-id";
    private static final String CAT_KEY = "cat-key";
    private static final String CAT_NAME = "cat-name";
    private static final String ENVIRONMENT_ID = "environment-id";

    @Mock
    CategoryService categoryService;

    CategoryQueryServiceLegacyWrapper service;

    @BeforeEach
    void setUp() {
        service = new CategoryQueryServiceLegacyWrapper(categoryService);
    }

    @Nested
    class FindById {

        @Test
        void should_return_category() {
            when(categoryService.findById(CAT_ID, ENVIRONMENT_ID))
                .thenReturn(CategoryEntity.builder().id(CAT_ID).key(CAT_KEY).name(CAT_NAME).build());

            var optional = service.findByIdOrKey(CAT_ID, ENVIRONMENT_ID);

            verify(categoryService, times(1)).findById(CAT_ID, ENVIRONMENT_ID);
            Assertions.assertThat(optional).hasValue(Category.builder().id(CAT_ID).key(CAT_KEY).name(CAT_NAME).build());
        }

        @Test
        void should_not_find_category() {
            when(categoryService.findById(CAT_ID, ENVIRONMENT_ID)).thenThrow(new CategoryNotFoundException(CAT_ID));

            var optional = service.findByIdOrKey(CAT_ID, ENVIRONMENT_ID);

            verify(categoryService, times(1)).findById(CAT_ID, ENVIRONMENT_ID);
            Assertions.assertThat(optional).isEmpty();
        }

        @Test
        void should_throw_when_other_error_occurs() {
            when(categoryService.findById(any(), any())).thenThrow(new TechnicalManagementException(CAT_ID));

            var throwable = Assertions.catchThrowable(() -> service.findByIdOrKey(CAT_ID, ENVIRONMENT_ID));

            Assertions.assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }
}
