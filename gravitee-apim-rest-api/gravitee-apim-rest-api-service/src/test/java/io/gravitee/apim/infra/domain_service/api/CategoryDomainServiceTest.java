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

package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryDomainServiceTest {

    @InjectMocks
    CategoryDomainServiceImpl sut;

    @Mock
    CategoryMapper categoryMapper;

    @Mock
    ApiCategoryOrderRepository apiCategoryOrderRepository;

    @Captor
    ArgumentCaptor<Collection<String>> categoriesIdsCaptor;

    @Test
    void updateOrderCategoriesOfApi() throws TechnicalException {
        // Given
        String apiId = "apiId";
        List<String> categoryIds = List.of("cat1", "cat2");
        when(apiCategoryOrderRepository.findAllByApiId(any())).thenReturn(
            Set.of(
                ApiCategoryOrder.builder().categoryId("oldcat1").apiId(apiId).order(0).build(),
                ApiCategoryOrder.builder().categoryId("oldcat2").apiId(apiId).order(1).build()
            )
        );

        // When
        sut.updateOrderCategoriesOfApi(apiId, categoryIds);

        // Then
        verify(apiCategoryOrderRepository).delete(eq(apiId), categoriesIdsCaptor.capture());
        assertThat(categoriesIdsCaptor.getValue()).containsExactlyInAnyOrderElementsOf(List.of("oldcat1", "oldcat2"));

        verify(apiCategoryOrderRepository).create(ApiCategoryOrder.builder().categoryId("cat1").apiId(apiId).order(0).build());
        verify(apiCategoryOrderRepository).create(ApiCategoryOrder.builder().categoryId("cat2").apiId(apiId).order(1).build());
    }

    @Test
    void should_only_remove_if_give_null_categories_list() throws TechnicalException {
        // Given
        String apiId = "apiId";
        when(apiCategoryOrderRepository.findAllByApiId(any())).thenReturn(
            Set.of(
                ApiCategoryOrder.builder().categoryId("oldcat1").apiId(apiId).order(0).build(),
                ApiCategoryOrder.builder().categoryId("oldcat2").apiId(apiId).order(1).build()
            )
        );

        // When
        sut.updateOrderCategoriesOfApi(apiId, null);

        // Then
        verify(apiCategoryOrderRepository).delete(eq(apiId), categoriesIdsCaptor.capture());
        assertThat(categoriesIdsCaptor.getValue()).containsExactlyInAnyOrderElementsOf(List.of("oldcat1", "oldcat2"));

        verifyNoMoreInteractions(apiCategoryOrderRepository);
    }
}
