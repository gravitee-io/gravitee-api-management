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

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductInfo;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GetApiProductInfosByApiIdUseCaseTest {

    private static final String API_ID = "api-1";

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();

    private GetApiProductInfosByApiIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetApiProductInfosByApiIdUseCase(apiProductQueryService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiProductQueryService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class WhenNoProductsLinkedToApi {

        @Test
        void should_return_empty_list_when_no_products_exist() {
            var output = useCase.execute(GetApiProductInfosByApiIdUseCase.Input.of(API_ID));

            assertThat(output.apiProductInfos()).isEmpty();
        }

        @Test
        void should_return_empty_list_when_products_reference_a_different_api() {
            apiProductQueryService.initWith(
                List.of(ApiProduct.builder().id("product-1").name("Product One").apiIds(Set.of("other-api")).build())
            );

            var output = useCase.execute(GetApiProductInfosByApiIdUseCase.Input.of(API_ID));

            assertThat(output.apiProductInfos()).isEmpty();
        }

        @Test
        void should_return_empty_list_when_product_has_null_api_ids() {
            apiProductQueryService.initWith(List.of(ApiProduct.builder().id("product-1").name("Product One").apiIds(null).build()));

            var output = useCase.execute(GetApiProductInfosByApiIdUseCase.Input.of(API_ID));

            assertThat(output.apiProductInfos()).isEmpty();
        }

        @Test
        void should_return_empty_list_when_product_has_empty_api_ids() {
            apiProductQueryService.initWith(List.of(ApiProduct.builder().id("product-1").name("Product One").apiIds(Set.of()).build()));

            var output = useCase.execute(GetApiProductInfosByApiIdUseCase.Input.of(API_ID));

            assertThat(output.apiProductInfos()).isEmpty();
        }
    }

    @Nested
    class WhenProductsAreLinkedToApi {

        @Test
        void should_return_id_and_name_mapped_from_matching_product() {
            apiProductQueryService.initWith(
                List.of(ApiProduct.builder().id("product-1").name("My Product").apiIds(Set.of(API_ID)).environmentId("env-1").build())
            );

            var output = useCase.execute(GetApiProductInfosByApiIdUseCase.Input.of(API_ID));

            assertThat(output.apiProductInfos()).containsExactly(new ApiProductInfo("product-1", "My Product"));
        }

        @Test
        void should_exclude_products_that_do_not_reference_the_api() {
            apiProductQueryService.initWith(
                List.of(
                    ApiProduct.builder().id("linked").name("Linked Product").apiIds(Set.of(API_ID, "other-api")).build(),
                    ApiProduct.builder().id("unlinked").name("Unlinked Product").apiIds(Set.of("other-api")).build()
                )
            );

            var output = useCase.execute(GetApiProductInfosByApiIdUseCase.Input.of(API_ID));

            assertThat(output.apiProductInfos()).containsExactly(new ApiProductInfo("linked", "Linked Product"));
        }

        @Test
        void should_return_multiple_products_sorted_by_name_case_insensitively() {
            apiProductQueryService.initWith(
                List.of(
                    ApiProduct.builder().id("product-c").name("zebra product").apiIds(Set.of(API_ID)).build(),
                    ApiProduct.builder().id("product-a").name("Alpha Product").apiIds(Set.of(API_ID)).build(),
                    ApiProduct.builder().id("product-b").name("beta product").apiIds(Set.of(API_ID)).build()
                )
            );

            var output = useCase.execute(GetApiProductInfosByApiIdUseCase.Input.of(API_ID));

            assertThat(output.apiProductInfos()).containsExactly(
                new ApiProductInfo("product-a", "Alpha Product"),
                new ApiProductInfo("product-b", "beta product"),
                new ApiProductInfo("product-c", "zebra product")
            );
        }

        @Test
        void should_sort_null_names_last() {
            apiProductQueryService.initWith(
                List.of(
                    ApiProduct.builder().id("product-null").name(null).apiIds(Set.of(API_ID)).build(),
                    ApiProduct.builder().id("product-a").name("Alpha").apiIds(Set.of(API_ID)).build()
                )
            );

            var output = useCase.execute(GetApiProductInfosByApiIdUseCase.Input.of(API_ID));

            assertThat(output.apiProductInfos()).containsExactly(
                new ApiProductInfo("product-a", "Alpha"),
                new ApiProductInfo("product-null", null)
            );
        }
    }
}
