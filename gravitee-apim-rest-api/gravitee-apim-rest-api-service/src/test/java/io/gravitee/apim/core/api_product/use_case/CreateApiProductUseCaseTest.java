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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import fixtures.core.model.ApiFixtures;
import inmemory.AbstractUseCaseTest;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.model.CreateApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateApiProductUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);
    private final ValidateApiProductService validateApiProductService = new ValidateApiProductService(apiQueryService);
    private final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private final ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final EventCrudService eventCrudService = mock(EventCrudService.class);
    private final EventLatestCrudService eventLatestCrudService = mock(EventLatestCrudService.class);

    private CreateApiProductUseCase createApiProductUseCase;

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

        parametersQueryService.initWith(
            List.of(
                new Parameter(Key.API_PRIMARY_OWNER_MODE.key(), ENV_ID, ParameterReferenceType.ENVIRONMENT, ApiPrimaryOwnerMode.USER.name())
            )
        );

        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiProductPrimaryOwnerFactory = new ApiProductPrimaryOwnerFactory(
            membershipQueryService,
            parametersQueryService,
            roleQueryService,
            userCrudService,
            groupQueryService
        );
        var apiProductPrimaryOwnerDomainService = new ApiProductPrimaryOwnerDomainService(
            auditService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        createApiProductUseCase = new CreateApiProductUseCase(
            apiProductQueryService,
            apiProductCrudService,
            validateApiProductService,
            auditService,
            apiProductPrimaryOwnerDomainService,
            apiProductPrimaryOwnerFactory,
            eventCrudService,
            eventLatestCrudService
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
        Api api1 = createV4ProxyApi("api-1", true);
        Api api2 = createV4ProxyApi("api-2", true);
        apiCrudService.initWith(List.of(api1, api2));

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

        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
        verify(eventLatestCrudService).createOrPatchLatestEvent(eq(ORG_ID), eq(GENERATED_UUID), any());
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

    @Test
    void should_throw_exception_when_api_not_allowed_in_product() {
        Api allowedApi = createV4ProxyApi("api-allowed", true);
        Api notAllowedApi = createV4ProxyApi("api-not-allowed", false);
        Api nullAllowedApi = createV4ProxyApi("api-null-allowed", null);

        apiCrudService.initWith(List.of(allowedApi, notAllowedApi, nullAllowedApi));
        var toCreate = CreateApiProduct.builder()
            .name("API Product 1")
            .version("1.0.0")
            .description("desc")
            .apiIds(List.of("api-allowed", "api-not-allowed", "api-null-allowed"))
            .build();

        var throwable = Assertions.catchThrowable(() ->
            createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO))
        );
        Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        Assertions.assertThat(throwable.getMessage()).contains("not allowed in API Products");
    }

    @Test
    void should_throw_exception_when_api_does_not_exist() {
        Api allowedApi = createV4ProxyApi("api-allowed", true);
        apiCrudService.initWith(List.of(allowedApi));

        var toCreate = CreateApiProduct.builder()
            .name("API Product 1")
            .version("1.0.0")
            .description("desc")
            .apiIds(List.of("api-allowed", "non-existent-1"))
            .build();

        var throwable = Assertions.catchThrowable(() ->
            createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO))
        );
        Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        Assertions.assertThat(throwable.getMessage()).contains("do not exist");
        Assertions.assertThat(throwable.getMessage()).contains("non-existent-1");
    }

    @Test
    void should_include_all_allowed_apis() {
        Api api1 = createV4ProxyApi("api-1", true);
        Api api2 = createV4ProxyApi("api-2", true);
        Api api3 = createV4ProxyApi("api-3", true);

        apiCrudService.initWith(List.of(api1, api2, api3));
        var toCreate = CreateApiProduct.builder()
            .name("API Product 1")
            .version("1.0.0")
            .description("desc")
            .apiIds(List.of("api-1", "api-2", "api-3"))
            .build();

        var output = createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO));
        assertThat(output.apiProduct().getApiIds()).containsExactlyInAnyOrder("api-1", "api-2", "api-3");
    }

    @Test
    void should_throw_exception_when_no_apis_are_allowed() {
        Api api1 = createV4ProxyApi("api-1", false);
        Api api2 = createV4ProxyApi("api-2", null);

        apiCrudService.initWith(List.of(api1, api2));

        var toCreate = CreateApiProduct.builder()
            .name("API Product 1")
            .version("1.0.0")
            .description("desc")
            .apiIds(List.of("api-1", "api-2"))
            .build();

        var throwable = Assertions.catchThrowable(() ->
            createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO))
        );
        Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        Assertions.assertThat(throwable.getMessage()).contains("not allowed in API Products");
    }

    @Test
    void should_create_product_with_empty_api_ids() {
        var toCreate = CreateApiProduct.builder().name("API Product 1").version("1.0.0").description("desc").apiIds(List.of()).build();

        var output = createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO));
        assertThat(output.apiProduct().getApiIds()).isEmpty();
    }

    @Test
    void should_throw_exception_when_api_is_not_v4() {
        Api v4Api = createV4ProxyApi("api-v4", true);
        Api v2Api = ApiFixtures.aProxyApiV2().toBuilder().id("api-v2").environmentId(ENV_ID).build();

        apiCrudService.initWith(List.of(v4Api, v2Api));

        var toCreate = CreateApiProduct.builder()
            .name("API Product 1")
            .version("1.0.0")
            .description("desc")
            .apiIds(List.of("api-v4", "api-v2"))
            .build();

        var throwable = Assertions.catchThrowable(() ->
            createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO))
        );
        Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        Assertions.assertThat(throwable.getMessage()).contains("Only V4 API definition is supported");
        Assertions.assertThat(throwable.getMessage()).contains("api-v2");
    }

    @Test
    void should_throw_exception_with_all_errors_when_multiple_validation_failures() {
        Api validApi = createV4ProxyApi("api-valid", true);
        Api notAllowedApi = createV4ProxyApi("api-not-allowed", false);
        Api v2Api = ApiFixtures.aProxyApiV2().toBuilder().id("api-v2").environmentId(ENV_ID).build();

        apiCrudService.initWith(List.of(validApi, notAllowedApi, v2Api));

        var toCreate = CreateApiProduct.builder()
            .name("API Product 1")
            .version("1.0.0")
            .description("desc")
            .apiIds(List.of("api-valid", "api-not-allowed", "api-v2", "non-existent-1", "non-existent-2"))
            .build();

        var throwable = Assertions.catchThrowable(() ->
            createApiProductUseCase.execute(new CreateApiProductUseCase.Input(toCreate, AUDIT_INFO))
        );
        Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        var message = throwable.getMessage();
        // Should contain all error types
        Assertions.assertThat(message).contains("do not exist");
        Assertions.assertThat(message).contains("non-existent-1");
        Assertions.assertThat(message).contains("non-existent-2");
        Assertions.assertThat(message).contains("not V4");
        Assertions.assertThat(message).contains("api-v2");
        Assertions.assertThat(message).contains("not allowed in API Products");
        Assertions.assertThat(message).contains("api-not-allowed");
    }

    private Api createV4ProxyApi(String id, Boolean allowedInApiProducts) {
        var apiDefinition = ApiFixtures.aProxyApiV4().getApiDefinitionValue();
        if (apiDefinition instanceof io.gravitee.definition.model.v4.Api v4Api) {
            var updatedApiDefinition = v4Api.toBuilder().id(id).allowedInApiProducts(allowedInApiProducts).build();
            return ApiFixtures.aProxyApiV4().toBuilder().id(id).environmentId(ENV_ID).apiDefinitionValue(updatedApiDefinition).build();
        }
        throw new IllegalStateException("Expected V4 API definition");
    }
}
