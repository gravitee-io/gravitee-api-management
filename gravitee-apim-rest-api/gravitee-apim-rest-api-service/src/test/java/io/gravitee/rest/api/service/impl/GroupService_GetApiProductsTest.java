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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.api.search.ApiProductCriteria;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.rest.api.model.ApiProductEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GroupService_GetApiProductsTest {

    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final String GROUP_ID = "my-group-id";

    @InjectMocks
    private final GroupService groupService = new GroupServiceImpl();

    @Mock
    private ApiProductsRepository apiProductsRepository;

    @Test
    public void shouldReturnApiProductsBelongingToGroup() throws TechnicalException {
        ApiProduct product1 = new ApiProduct();
        product1.setId("product-1");
        product1.setName("API Product One");
        product1.setVersion("1.0");

        ApiProduct product2 = new ApiProduct();
        product2.setId("product-2");
        product2.setName("API Product Two");
        product2.setVersion("2.0");

        when(
            apiProductsRepository.search(new ApiProductCriteria.Builder().environmentId(ENVIRONMENT_ID).groups(List.of(GROUP_ID)).build())
        ).thenReturn(List.of(product1, product2));

        List<ApiProductEntity> result = groupService.getApiProducts(ENVIRONMENT_ID, GROUP_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApiProductEntity::getId).containsExactlyInAnyOrder("product-1", "product-2");
        assertThat(result).extracting(ApiProductEntity::getName).containsExactlyInAnyOrder("API Product One", "API Product Two");
        assertThat(result).extracting(ApiProductEntity::getVersion).containsExactlyInAnyOrder("1.0", "2.0");
    }

    @Test
    public void shouldReturnEmptyListWhenNoApiProductsBelongToGroup() throws TechnicalException {
        when(
            apiProductsRepository.search(new ApiProductCriteria.Builder().environmentId(ENVIRONMENT_ID).groups(List.of(GROUP_ID)).build())
        ).thenReturn(List.of());

        List<ApiProductEntity> result = groupService.getApiProducts(ENVIRONMENT_ID, GROUP_ID);

        assertThat(result).isEmpty();
    }

    @Test
    public void shouldMapRepositoryFieldsToEntity() throws TechnicalException {
        ApiProduct product = new ApiProduct();
        product.setId("product-id");
        product.setName("My Product");
        product.setVersion("3.0");

        when(
            apiProductsRepository.search(new ApiProductCriteria.Builder().environmentId(ENVIRONMENT_ID).groups(List.of(GROUP_ID)).build())
        ).thenReturn(List.of(product));

        List<ApiProductEntity> result = groupService.getApiProducts(ENVIRONMENT_ID, GROUP_ID);

        assertThat(result).hasSize(1);
        ApiProductEntity entity = result.get(0);
        assertThat(entity.getId()).isEqualTo("product-id");
        assertThat(entity.getName()).isEqualTo("My Product");
        assertThat(entity.getVersion()).isEqualTo("3.0");
    }

    @Test
    public void shouldWrapRepositoryExceptionAsTechnicalManagementException() throws TechnicalException {
        when(
            apiProductsRepository.search(new ApiProductCriteria.Builder().environmentId(ENVIRONMENT_ID).groups(List.of(GROUP_ID)).build())
        ).thenThrow(new TechnicalException("DB error"));

        assertThatThrownBy(() -> groupService.getApiProducts(ENVIRONMENT_ID, GROUP_ID))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining(GROUP_ID);
    }
}
