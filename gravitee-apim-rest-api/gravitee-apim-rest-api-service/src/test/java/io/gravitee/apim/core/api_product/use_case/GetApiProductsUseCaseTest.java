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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetApiProductsUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private GetApiProductsUseCase getApiProductsUseCase;

    @BeforeEach
    void setUp() {
        getApiProductsUseCase = new GetApiProductsUseCase(apiProductQueryService);
    }

    @Test
    void should_return_api_product_by_id() {
        ApiProduct product = ApiProduct.builder().id("api-product-id").name("Product 1").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(List.of(product));

        var input = GetApiProductsUseCase.Input.of(ENV_ID, "api-product-id");
        var output = getApiProductsUseCase.execute(input);
        assertAll(
            () -> assertThat(output.apiProduct().get().getId()).isEqualTo("api-product-id"),
            () -> assertThat(output.apiProduct().get().getName()).isEqualTo("Product 1")
        );
    }

    @Test
    void should_return_api_products_by_environment_id() {
        ApiProduct product1 = ApiProduct.builder().id("id1").name("P1").environmentId(ENV_ID).build();
        ApiProduct product2 = ApiProduct.builder().id("id2").name("P2").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(List.of(product1, product2));

        var input = GetApiProductsUseCase.Input.of(ENV_ID);
        var output = getApiProductsUseCase.execute(input);
        Set<ApiProduct> products = output.apiProducts();
        assertThat(products).hasSize(2);
        assertThat(products).extracting(ApiProduct::getId).containsExactlyInAnyOrder("id1", "id2");
    }

    @Test
    void should_throw_exception_if_environment_id_missing() {
        var input = GetApiProductsUseCase.Input.of(null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> getApiProductsUseCase.execute(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("environmentId must be provided");
    }
}
