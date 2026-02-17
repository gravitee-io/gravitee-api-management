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
package io.gravitee.apim.infra.crud_service.api_product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.ApiProductAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApiProductCrudServiceImplTest {

    private final ApiProductsRepository apiProductsRepository = mock(ApiProductsRepository.class);
    private final ApiProductCrudServiceImpl apiProductCrudService = new ApiProductCrudServiceImpl(apiProductsRepository);

    @Nested
    class Create {

        @Test
        public void should_create_api_product() throws TechnicalException {
            var apiProduct = ApiProduct.builder()
                .id("api-product-id")
                .environmentId("env-id")
                .name("API Product 1")
                .description("desc")
                .version("1.0.0")
                .apiIds(Set.of("api-1", "api-2"))
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(apiProductsRepository.create(any())).thenReturn(ApiProductAdapter.INSTANCE.toRepository(apiProduct));

            var createdApiProduct = apiProductCrudService.create(apiProduct);

            assertAll(
                () -> assertThat(createdApiProduct).isNotNull(),
                () -> assertThat(createdApiProduct.getId()).isEqualTo(apiProduct.getId()),
                () -> assertThat(createdApiProduct.getName()).isEqualTo(apiProduct.getName()),
                () -> assertThat(createdApiProduct.getDescription()).isEqualTo(apiProduct.getDescription()),
                () -> assertThat(createdApiProduct.getVersion()).isEqualTo(apiProduct.getVersion()),
                () -> assertThat(createdApiProduct.getEnvironmentId()).isEqualTo(apiProduct.getEnvironmentId()),
                () -> assertThat(createdApiProduct.getApiIds()).isEqualTo(apiProduct.getApiIds())
            );
        }

        @Test
        public void should_throw_technical_domain_exception_when_create_fails() throws TechnicalException {
            var apiProduct = ApiProduct.builder()
                .id("api-product-id")
                .environmentId("env-id")
                .name("API Product 1")
                .version("1.0.0")
                .build();

            when(apiProductsRepository.create(any())).thenThrow(new TechnicalException("Database error"));

            var exception = assertThrows(TechnicalDomainException.class, () -> apiProductCrudService.create(apiProduct));

            assertThat(exception.getMessage()).contains("An error occurred while trying to create the API Product: api-product-id");
        }
    }

    @Nested
    class Delete {

        @Test
        public void should_delete_api_product() throws TechnicalException {
            String apiProductId = "api-product-id";

            apiProductCrudService.delete(apiProductId);

            verify(apiProductsRepository).delete(apiProductId);
        }

        @Test
        public void should_throw_technical_domain_exception_when_delete_fails() throws TechnicalException {
            String apiProductId = "api-product-id";
            doThrow(new TechnicalException("Database error")).when(apiProductsRepository).delete(apiProductId);

            var exception = assertThrows(TechnicalDomainException.class, () -> apiProductCrudService.delete(apiProductId));

            assertThat(exception.getMessage()).contains("An error occurs while trying to delete the api product: api-product-id");
        }
    }

    @Nested
    class Update {

        @Test
        public void should_update_api_product() throws TechnicalException {
            var apiProduct = ApiProduct.builder()
                .id("api-product-id")
                .environmentId("env-id")
                .name("Updated API Product")
                .description("Updated description")
                .version("2.0.0")
                .apiIds(Set.of("api-1", "api-3"))
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(apiProductsRepository.update(any())).thenReturn(ApiProductAdapter.INSTANCE.toRepository(apiProduct));

            var updatedApiProduct = apiProductCrudService.update(apiProduct);

            assertAll(
                () -> assertThat(updatedApiProduct).isNotNull(),
                () -> assertThat(updatedApiProduct.getId()).isEqualTo(apiProduct.getId()),
                () -> assertThat(updatedApiProduct.getName()).isEqualTo("Updated API Product"),
                () -> assertThat(updatedApiProduct.getDescription()).isEqualTo("Updated description"),
                () -> assertThat(updatedApiProduct.getVersion()).isEqualTo("2.0.0"),
                () -> assertThat(updatedApiProduct.getApiIds()).containsExactlyInAnyOrder("api-1", "api-3")
            );
        }

        @Test
        public void should_set_updated_at_if_null() throws TechnicalException {
            var apiProduct = ApiProduct.builder()
                .id("api-product-id")
                .environmentId("env-id")
                .name("API Product")
                .version("1.0.0")
                .createdAt(ZonedDateTime.now())
                .updatedAt(null)
                .build();

            when(apiProductsRepository.update(any())).thenAnswer(invocation -> {
                io.gravitee.repository.management.model.ApiProduct repoModel = invocation.getArgument(0);
                assertThat(repoModel.getUpdatedAt()).isNotNull();
                return repoModel;
            });

            apiProductCrudService.update(apiProduct);

            verify(apiProductsRepository).update(any());
        }

        @Test
        public void should_throw_technical_management_exception_when_update_fails() throws TechnicalException {
            var apiProduct = ApiProduct.builder().id("api-product-id").environmentId("env-id").name("API Product").version("1.0.0").build();

            when(apiProductsRepository.update(any())).thenThrow(new TechnicalException("Database error"));

            var exception = assertThrows(TechnicalManagementException.class, () -> apiProductCrudService.update(apiProduct));

            assertThat(exception.getMessage()).contains("ApiProduct");
            assertThat(exception.getMessage()).contains("api-product-id");
        }
    }

    @Nested
    class Get {

        @Test
        void should_get_an_api() throws TechnicalException {
            var apiProduct = io.gravitee.repository.management.model.ApiProduct.builder()
                .id("api-product-id")
                .environmentId("env-id")
                .name("Updated API Product")
                .description("Updated description")
                .version("2.0.0")
                .apiIds(List.of("api-1", "api-3"))
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            when(apiProductsRepository.findById("api-product-id")).thenReturn(Optional.of(apiProduct));

            var result = apiProductCrudService.get("api-product-id");
            Assertions.assertThat(result).extracting(ApiProduct::getId, ApiProduct::getVersion).containsExactly("api-product-id", "2.0.0");
        }

        @Test
        void should_throw_exception_if_api_not_found() throws TechnicalException {
            when(apiProductsRepository.findById("api-product-id")).thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(() -> apiProductCrudService.get("api-product-id")).isInstanceOf(
                ApiProductNotFoundException.class
            );
        }
    }
}
