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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetApiProductApisUseCaseTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String USER_ID = "user-id";
    private static final String API_PRODUCT_ID = "api-product-1";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORG_ID, ENV_ID);

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService = new ApiAuthorizationDomainServiceInMemory();

    @Mock
    private ApiProductSearchQueryService apiProductSearchQueryService;

    private GetApiProductApisUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetApiProductApisUseCase(apiProductQueryService, apiAuthorizationDomainService, apiProductSearchQueryService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiProductQueryService, apiAuthorizationDomainService).forEach(InMemoryAlternative::reset);
    }

    private GetApiProductApisUseCase.Input input(
        String apiProductId,
        String query,
        io.gravitee.rest.api.model.common.Pageable pageable,
        io.gravitee.rest.api.model.common.Sortable sortable,
        String userId,
        boolean isAdmin
    ) {
        return new GetApiProductApisUseCase.Input(EXECUTION_CONTEXT, apiProductId, query, pageable, sortable, userId, isAdmin);
    }

    @Nested
    class ProductNotFound {

        @Test
        void should_return_empty_output_when_product_not_found() {
            var input = input(API_PRODUCT_ID, null, new PageableImpl(1, 10), null, USER_ID, true);
            var output = useCase.execute(input);

            assertThat(output.apiProduct()).isEmpty();
            assertThat(output.apisPage()).isNull();
        }
    }

    @Nested
    class ProductWithNoApis {

        @Test
        void should_return_empty_page_when_product_has_no_api_ids() {
            ApiProduct product = ApiProduct.builder().id(API_PRODUCT_ID).name("Product").environmentId(ENV_ID).apiIds(Set.of()).build();
            apiProductQueryService.initWith(List.of(product));

            var input = input(API_PRODUCT_ID, null, new PageableImpl(1, 10), null, USER_ID, true);
            var output = useCase.execute(input);

            assertThat(output.apiProduct()).isPresent();
            assertThat(output.apiProduct().get().getId()).isEqualTo(API_PRODUCT_ID);
            assertThat(output.apisPage()).isNotNull();
            assertThat(output.apisPage().getContent()).isEmpty();
            assertThat(output.apisPage().getTotalElements()).isZero();
        }

        @Test
        void should_return_empty_page_when_product_has_null_api_ids() {
            ApiProduct product = ApiProduct.builder().id(API_PRODUCT_ID).name("Product").environmentId(ENV_ID).apiIds(null).build();
            apiProductQueryService.initWith(List.of(product));

            var input = input(API_PRODUCT_ID, null, new PageableImpl(1, 10), null, USER_ID, true);
            var output = useCase.execute(input);

            assertThat(output.apiProduct()).isPresent();
            assertThat(output.apisPage()).isNotNull();
            assertThat(output.apisPage().getContent()).isEmpty();
        }

        @Test
        void should_return_apis_page_when_admin_and_product_has_apis() {
            ApiProduct product = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1", "api-2"))
                .build();
            apiProductQueryService.initWith(List.of(product));

            var apiEntity = new ApiEntity();
            apiEntity.setId("api-1");
            apiEntity.setName("API 1");
            when(apiProductSearchQueryService.searchApis(any(), anyString(), anyBoolean(), any(), any(), any(), any())).thenReturn(
                new Page<>(List.of(apiEntity), 1, 10, 2)
            );

            var input = input(API_PRODUCT_ID, null, new PageableImpl(1, 10), null, USER_ID, true);
            var output = useCase.execute(input);

            assertThat(output.apiProduct()).isPresent();
            assertThat(output.apiProduct().get().getId()).isEqualTo(API_PRODUCT_ID);
            assertThat(output.apisPage()).isNotNull();
            assertThat(output.apisPage().getContent()).hasSize(1);
            assertThat(output.apisPage().getTotalElements()).isEqualTo(2);
        }
    }

    @Nested
    class AdminUser {

        @Test
        void should_return_page_from_search_when_product_has_apis() {
            ApiProduct product = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1", "api-2", "api-3"))
                .build();
            apiProductQueryService.initWith(List.of(product));
            when(apiProductSearchQueryService.searchApis(any(), anyString(), anyBoolean(), any(), any(), any(), any())).thenReturn(
                new Page<>(List.of(new ApiEntity(), new ApiEntity()), 1, 2, 3)
            );

            var input = input(API_PRODUCT_ID, null, new PageableImpl(1, 2), null, USER_ID, true);
            var output = useCase.execute(input);

            assertThat(output.apisPage()).isNotNull();
            assertThat(output.apisPage().getContent()).hasSize(2);
            assertThat(output.apisPage().getTotalElements()).isEqualTo(3);
        }
    }
}
