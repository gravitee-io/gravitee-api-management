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

import static fixtures.core.model.RoleFixtures.apiPrimaryOwnerRoleId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetApiProductsUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();

    private GetApiProductsUseCase getApiProductsUseCase;

    @BeforeEach
    void setUp() {
        userCrudService.initWith(
            List.of(
                io.gravitee.apim.core.user.model.BaseUserEntity.builder()
                    .id(USER_ID)
                    .email("user@example.com")
                    .firstname("Test")
                    .lastname("User")
                    .build()
            )
        );

        roleQueryService.resetSystemRoles(ORG_ID);

        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiProductPrimaryOwnerDomainService = new ApiProductPrimaryOwnerDomainService(
            auditService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        getApiProductsUseCase = new GetApiProductsUseCase(apiProductQueryService, apiProductPrimaryOwnerDomainService);
    }

    @Test
    void should_return_api_product_by_id() {
        ApiProduct product = ApiProduct.builder().id("api-product-id").name("Product 1").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(List.of(product));

        membershipCrudService.initWith(
            List.of(
                Membership.builder()
                    .id("membership-id")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("api-product-id")
                    .roleId(apiPrimaryOwnerRoleId(ORG_ID))
                    .source("system")
                    .build()
            )
        );

        var input = GetApiProductsUseCase.Input.of(ENV_ID, "api-product-id", ORG_ID);
        var output = getApiProductsUseCase.execute(input);
        assertAll(
            () -> assertThat(output.apiProduct().get().getId()).isEqualTo("api-product-id"),
            () -> assertThat(output.apiProduct().get().getName()).isEqualTo("Product 1"),
            () -> assertThat(output.apiProduct().get().getPrimaryOwner()).isNotNull(),
            () -> assertThat(output.apiProduct().get().getPrimaryOwner().id()).isEqualTo(USER_ID),
            () -> assertThat(output.apiProduct().get().getPrimaryOwner().displayName()).isEqualTo("Test User")
        );
    }

    @Test
    void should_return_api_products_by_environment_id() {
        ApiProduct product1 = ApiProduct.builder().id("id1").name("P1").environmentId(ENV_ID).build();
        ApiProduct product2 = ApiProduct.builder().id("id2").name("P2").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(List.of(product1, product2));

        membershipCrudService.initWith(
            List.of(
                Membership.builder()
                    .id("membership-1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("id1")
                    .roleId(apiPrimaryOwnerRoleId(ORG_ID))
                    .source("system")
                    .build(),
                Membership.builder()
                    .id("membership-2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("id2")
                    .roleId(apiPrimaryOwnerRoleId(ORG_ID))
                    .source("system")
                    .build()
            )
        );

        var input = GetApiProductsUseCase.Input.of(ENV_ID, ORG_ID);
        var output = getApiProductsUseCase.execute(input);
        Set<ApiProduct> products = output.apiProducts();
        assertThat(products).hasSize(2);
        assertThat(products).extracting(ApiProduct::getId).containsExactlyInAnyOrder("id1", "id2");
        assertThat(products).allMatch(product -> product.getPrimaryOwner() != null);
        assertThat(products).allMatch(product -> product.getPrimaryOwner().id().equals(USER_ID));
    }

    @Test
    void should_throw_exception_if_environment_id_missing() {
        var input = GetApiProductsUseCase.Input.of(null, null, ORG_ID);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> getApiProductsUseCase.execute(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("environmentId must be provided");
    }
}
