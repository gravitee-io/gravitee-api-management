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
package io.gravitee.apim.infra.query_service.api_product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiProductQueryServiceImplTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";
    private static final String API_PRODUCT_NAME = "My API Product";
    private static final String API_ID = "api-id";

    ApiProductsRepository apiProductRepository;
    ApiProductQueryService service;

    @BeforeEach
    void setUp() {
        apiProductRepository = mock(ApiProductsRepository.class);
        service = new ApiProductQueryServiceImpl(apiProductRepository);
        GraviteeContext.setCurrentOrganization(ORG_ID);
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class FindByEnvironmentIdAndName {

        @Test
        void should_return_empty_when_not_found() throws TechnicalException {
            when(apiProductRepository.findByEnvironmentIdAndName(ENV_ID, API_PRODUCT_NAME)).thenReturn(Optional.empty());
            var result = service.findByEnvironmentIdAndName(ENV_ID, API_PRODUCT_NAME);
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_api_product_when_found() throws TechnicalException {
            var repoApiProduct = buildRepositoryApiProduct();
            when(apiProductRepository.findByEnvironmentIdAndName(ENV_ID, API_PRODUCT_NAME)).thenReturn(Optional.of(repoApiProduct));
            var result = service.findByEnvironmentIdAndName(ENV_ID, API_PRODUCT_NAME);

            assertAll(() -> {
                assertThat(result).isPresent();
                assertThat(result.get().getId()).isEqualTo(API_PRODUCT_ID);
                assertThat(result.get().getName()).isEqualTo(API_PRODUCT_NAME);
                assertThat(result.get().getEnvironmentId()).isEqualTo(ENV_ID);
            });
        }

        @Test
        void should_throw_technical_exception_when_repository_fails() throws TechnicalException {
            when(apiProductRepository.findByEnvironmentIdAndName(ENV_ID, API_PRODUCT_NAME)).thenThrow(
                new TechnicalException("Database error")
            );

            var throwable = catchThrowable(() -> service.findByEnvironmentIdAndName(ENV_ID, API_PRODUCT_NAME));
            assertThat(throwable).isInstanceOf(TechnicalDomainException.class);
        }
    }

    @Nested
    class FindByEnvironmentId {

        @Test
        void should_return_empty_set_when_no_api_products_found() throws TechnicalException {
            when(apiProductRepository.findByEnvironmentId(ENV_ID)).thenReturn(Set.of());
            var result = service.findByEnvironmentId(ENV_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_api_products_when_found() throws TechnicalException {
            var repoApiProduct1 = buildRepositoryApiProduct();
            var repoApiProduct2 = io.gravitee.repository.management.model.ApiProduct.builder()
                .id("api-product-2")
                .environmentId(ENV_ID)
                .name("Another API Product")
                .description("Description")
                .version("1.0.0")
                .apiIds(List.of("api-2"))
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

            when(apiProductRepository.findByEnvironmentId(ENV_ID)).thenReturn(Set.of(repoApiProduct1, repoApiProduct2));

            var result = service.findByEnvironmentId(ENV_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApiProduct::getId).containsExactlyInAnyOrder(API_PRODUCT_ID, "api-product-2");
        }
    }

    @Nested
    class FindByEnvironmentIdAndIdIn {

        @Test
        void should_return_empty_set_when_ids_null() {
            var result = service.findByEnvironmentIdAndIdIn(ENV_ID, null);
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_set_when_ids_empty() {
            var result = service.findByEnvironmentIdAndIdIn(ENV_ID, Set.of());
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_products_matching_environment_and_ids() throws TechnicalException {
            var repoApiProduct = buildRepositoryApiProduct();
            var otherEnvProduct = io.gravitee.repository.management.model.ApiProduct.builder()
                .id("other-env-product")
                .environmentId("other-env")
                .name("Other")
                .version("1.0.0")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            when(apiProductRepository.findByIds(Set.of(API_PRODUCT_ID, "other-env-product"))).thenReturn(
                Set.of(repoApiProduct, otherEnvProduct)
            );

            var result = service.findByEnvironmentIdAndIdIn(ENV_ID, Set.of(API_PRODUCT_ID, "other-env-product"));

            assertThat(result).hasSize(1);
            assertThat(result.iterator().next().getId()).isEqualTo(API_PRODUCT_ID);
        }

        @Test
        void should_throw_technical_exception_when_repository_fails() throws TechnicalException {
            when(apiProductRepository.findByIds(Set.of(API_PRODUCT_ID))).thenThrow(new TechnicalException("Database error"));
            var throwable = catchThrowable(() -> service.findByEnvironmentIdAndIdIn(ENV_ID, Set.of(API_PRODUCT_ID)));
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class FindById {

        @Test
        void should_return_empty_when_not_found() throws TechnicalException {
            when(apiProductRepository.findById(API_PRODUCT_ID)).thenReturn(Optional.empty());
            var result = service.findById(API_PRODUCT_ID);
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_id_is_null() {
            var result = service.findById(null);
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_api_product_when_found() throws TechnicalException {
            var repoApiProduct = buildRepositoryApiProduct();
            when(apiProductRepository.findById(API_PRODUCT_ID)).thenReturn(Optional.of(repoApiProduct));
            var result = service.findById(API_PRODUCT_ID);

            assertAll(() -> {
                assertThat(result).isPresent();
                assertThat(result.get().getId()).isEqualTo(API_PRODUCT_ID);
                assertThat(result.get().getName()).isEqualTo(API_PRODUCT_NAME);
            });
        }

        @Test
        void should_throw_technical_exception_when_repository_fails() throws TechnicalException {
            when(apiProductRepository.findById(API_PRODUCT_ID)).thenThrow(new TechnicalException("Database error"));
            var throwable = catchThrowable(() -> service.findById(API_PRODUCT_ID));
            assertThat(throwable).isInstanceOf(TechnicalDomainException.class);
        }
    }

    @Nested
    class FindByApiId {

        @Test
        void should_return_empty_set_when_no_api_products_found() throws TechnicalException {
            when(apiProductRepository.findByApiId(API_ID)).thenReturn(Set.of());
            var result = service.findByApiId(API_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_api_products_containing_api() throws TechnicalException {
            var repoApiProduct = buildRepositoryApiProduct();
            when(apiProductRepository.findByApiId(API_ID)).thenReturn(Set.of(repoApiProduct));
            var result = service.findByApiId(API_ID);

            assertAll(() -> {
                assertThat(result).hasSize(1);
                assertThat(result.iterator().next().getApiIds()).contains(API_ID);
            });
        }

        @Test
        void should_throw_technical_exception_when_repository_fails() throws TechnicalException {
            when(apiProductRepository.findByApiId(API_ID)).thenThrow(new TechnicalException("Database error"));
            var throwable = catchThrowable(() -> service.findByApiId(API_ID));
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class FindProductsByApiIds {

        @Test
        void should_return_empty_map_when_api_ids_empty() {
            var result = service.findProductsByApiIds(Set.of());
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_map_when_api_ids_null() {
            var result = service.findProductsByApiIds(null);
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_products_grouped_by_api_id() throws TechnicalException {
            var repoProduct1 = buildRepositoryApiProduct();
            var repoProduct2 = io.gravitee.repository.management.model.ApiProduct.builder()
                .id("product-2")
                .environmentId(ENV_ID)
                .name("Product 2")
                .apiIds(List.of("api-2", API_ID))
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            when(apiProductRepository.findApiProductsByApiIds(Set.of(API_ID, "api-2"))).thenReturn(Set.of(repoProduct1, repoProduct2));

            Map<String, Set<ApiProduct>> result = service.findProductsByApiIds(Set.of(API_ID, "api-2"));

            assertThat(result).hasSize(2);
            assertThat(result.get(API_ID)).extracting(ApiProduct::getId).containsExactlyInAnyOrder(API_PRODUCT_ID, "product-2");
            assertThat(result.get("api-2")).extracting(ApiProduct::getId).containsExactlyInAnyOrder("product-2");
        }

        @Test
        void should_throw_technical_exception_when_repository_fails() throws TechnicalException {
            when(apiProductRepository.findApiProductsByApiIds(Set.of(API_ID))).thenThrow(new TechnicalException("Database error"));
            var throwable = catchThrowable(() -> service.findProductsByApiIds(Set.of(API_ID)));
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class SearchByIds {

        @Test
        void should_return_empty_page_when_ids_null() {
            var result = service.searchByIds(null, ENV_ID, new PageableImpl(1, 10));
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void should_return_empty_page_when_ids_empty() {
            var result = service.searchByIds(Set.of(), ENV_ID, new PageableImpl(1, 10));
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void should_return_empty_page_when_search_returns_no_results() throws TechnicalException {
            when(apiProductRepository.search(any(), any(), any())).thenReturn(new Page<>(List.of(), 1, 0, 0));

            Page<ApiProduct> result = service.searchByIds(Set.of(API_PRODUCT_ID), ENV_ID, new PageableImpl(1, 10));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void should_return_products_for_current_page() throws TechnicalException {
            var repoApiProduct = buildRepositoryApiProduct();
            when(apiProductRepository.search(any(), any(), any())).thenReturn(new Page<>(List.of(repoApiProduct), 1, 1, 1));

            Page<ApiProduct> result = service.searchByIds(Set.of(API_PRODUCT_ID), ENV_ID, new PageableImpl(1, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(API_PRODUCT_ID);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void should_respect_db_sort_order_from_search() throws TechnicalException {
            var productA = io.gravitee.repository.management.model.ApiProduct.builder()
                .id("p-a")
                .environmentId(ENV_ID)
                .name("Alpha Product")
                .version("1.0.0")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            var productZ = io.gravitee.repository.management.model.ApiProduct.builder()
                .id("p-z")
                .environmentId(ENV_ID)
                .name("Zebra Product")
                .version("1.0.0")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            when(apiProductRepository.search(any(), any(), any())).thenReturn(new Page<>(List.of(productA, productZ), 1, 2, 2));

            Page<ApiProduct> result = service.searchByIds(Set.of("p-a", "p-z"), ENV_ID, new PageableImpl(1, 10));

            assertThat(result.getContent()).extracting(ApiProduct::getName).containsExactly("Alpha Product", "Zebra Product");
        }

        @Test
        void should_return_only_current_page_with_correct_total() throws TechnicalException {
            var product3 = io.gravitee.repository.management.model.ApiProduct.builder()
                .id("p-3")
                .environmentId(ENV_ID)
                .name("Product C")
                .version("1.0.0")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            when(apiProductRepository.search(any(), any(), any())).thenReturn(new Page<>(List.of(product3), 2, 1, 3));

            Page<ApiProduct> result = service.searchByIds(Set.of("p-1", "p-2", "p-3"), ENV_ID, new PageableImpl(2, 2));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Product C");
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        void should_throw_technical_exception_when_search_fails() throws TechnicalException {
            when(apiProductRepository.search(any(), any(), any())).thenThrow(new TechnicalException("Database error"));
            var throwable = catchThrowable(() -> service.searchByIds(Set.of(API_PRODUCT_ID), ENV_ID, new PageableImpl(1, 10)));
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    private io.gravitee.repository.management.model.ApiProduct buildRepositoryApiProduct() {
        return io.gravitee.repository.management.model.ApiProduct.builder()
            .id(API_PRODUCT_ID)
            .environmentId(ENV_ID)
            .name(API_PRODUCT_NAME)
            .description("API Product Description")
            .version("1.0.0")
            .apiIds(List.of(API_ID))
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
    }
}
