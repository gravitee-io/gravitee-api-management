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

import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.model.CreateApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateApiProductUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final ValidateApiProductService validateApiProductService = new ValidateApiProductService();
    private final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    private CreateApiProductUseCase createApiProductUseCase;

    @BeforeEach
    void setUp() {
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiProductPrimaryOwnerDomainService = new ApiProductPrimaryOwnerDomainService(
            auditService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService
        );

        createApiProductUseCase = new CreateApiProductUseCase(
            apiProductQueryService,
            apiProductCrudService,
            validateApiProductService,
            auditService,
            apiProductPrimaryOwnerDomainService
        );

        initRoles();
    }

    private void initRoles() {
        List<Role> roles = List.of(
            Role.builder()
                .id("api-product-po-role-id")
                .scope(Role.Scope.ENVIRONMENT)
                .name("PRIMARY_OWNER")
                .referenceType(Role.ReferenceType.ORGANIZATION)
                .referenceId(ORG_ID)
                .build()
        );
        roleQueryService.initWith(roles);
    }

    @Test
    void should_create_api_product() {
        var toCreate = CreateApiProduct.builder()
            .name("API Product 1")
            .version("1.0.0")
            .description("desc")
            .apiIds(List.of("api-1", "api-2"))
            .build();

        var output = createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO));

        assertThat(output.apiProduct())
            .hasFieldOrPropertyWithValue("id", GENERATED_UUID)
            .hasFieldOrPropertyWithValue("name", "API Product 1")
            .hasFieldOrPropertyWithValue("version", "1.0.0")
            .hasFieldOrPropertyWithValue("description", "desc")
            .hasFieldOrPropertyWithValue("environmentId", ENV_ID);

        assertThat(output.apiProduct().getApiIds()).containsExactlyInAnyOrder("api-1", "api-2");
        assertThat(output.apiProduct().getCreatedAt()).isNotNull();
        assertThat(output.apiProduct().getUpdatedAt()).isNotNull();
    }

    @Test
    void should_throw_exception_when_name_is_null() {
        var toCreate = CreateApiProduct.builder().version("1.0.0").build();
        var throwable = Assertions.catchThrowable(() ->
            createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO))
        );
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessageContaining("API Product name is required");
    }

    @Test
    void should_throw_exception_when_version_is_null() {
        var toCreate = CreateApiProduct.builder().name("API Product 1").build();
        var throwable = Assertions.catchThrowable(() ->
            createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO))
        );
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessageContaining("API Product version is required");
    }
}
