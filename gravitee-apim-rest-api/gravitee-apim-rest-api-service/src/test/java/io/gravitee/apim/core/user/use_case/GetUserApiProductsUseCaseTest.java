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
package io.gravitee.apim.core.user.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.membership.model.Membership;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetUserApiProductsUseCaseTest {

    private static final String USER_ID = "user-1";

    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    GetUserApiProductsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUserApiProductsUseCase(membershipQueryService, apiProductQueryService, environmentCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(membershipQueryService, apiProductQueryService, environmentCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_user_api_products_with_correct_fields() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("ap-1")
                    .build()
            )
        );
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id("ap-1").name("My Product").version("1.0").environmentId("env-1").build())
        );
        environmentCrudService.initWith(List.of(Environment.builder().id("env-1").name("Development").build()));

        var output = useCase.execute(new GetUserApiProductsUseCase.Input(USER_ID, null, 1, 10));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(1);

        var result = output.data().get(0);
        assertThat(result.getId()).isEqualTo("ap-1");
        assertThat(result.getName()).isEqualTo("My Product");
        assertThat(result.getVersion()).isEqualTo("1.0");
        assertThat(result.getVisibility()).isNull();
        assertThat(result.getEnvironmentId()).isEqualTo("env-1");
        assertThat(result.getEnvironmentName()).isEqualTo("Development");
    }

    @Test
    void should_filter_by_environment_id() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("ap-1")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("ap-2")
                    .build()
            )
        );
        apiProductQueryService.initWith(
            List.of(
                ApiProduct.builder().id("ap-1").name("Product 1").environmentId("env-1").build(),
                ApiProduct.builder().id("ap-2").name("Product 2").environmentId("env-2").build()
            )
        );
        environmentCrudService.initWith(
            List.of(Environment.builder().id("env-1").name("Dev").build(), Environment.builder().id("env-2").name("Prod").build())
        );

        var output = useCase.execute(new GetUserApiProductsUseCase.Input(USER_ID, "env-1", 1, 10));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(1);
        assertThat(output.data().get(0).getId()).isEqualTo("ap-1");
    }

    @Test
    void should_return_empty_page_when_user_has_no_memberships() {
        var output = useCase.execute(new GetUserApiProductsUseCase.Input(USER_ID, null, 1, 10));

        assertThat(output.data()).isEmpty();
        assertThat(output.totalCount()).isZero();
    }

    @Test
    void should_paginate_correctly() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("ap-1")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("ap-2")
                    .build(),
                Membership.builder()
                    .id("m3")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("ap-3")
                    .build()
            )
        );
        apiProductQueryService.initWith(
            List.of(
                ApiProduct.builder().id("ap-1").name("Product 1").environmentId("env-1").build(),
                ApiProduct.builder().id("ap-2").name("Product 2").environmentId("env-1").build(),
                ApiProduct.builder().id("ap-3").name("Product 3").environmentId("env-1").build()
            )
        );
        environmentCrudService.initWith(List.of(Environment.builder().id("env-1").name("Dev").build()));

        var output = useCase.execute(new GetUserApiProductsUseCase.Input(USER_ID, "env-1", 2, 2));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(3);
        assertThat(output.data().get(0).getId()).isEqualTo("ap-3");
    }
}
