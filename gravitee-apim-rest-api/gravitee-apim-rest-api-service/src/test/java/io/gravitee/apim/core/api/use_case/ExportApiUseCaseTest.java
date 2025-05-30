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
package io.gravitee.apim.core.api.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.MediaQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.WorkflowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.api.ApiExportDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ExportApiUseCaseTest {

    static final String ORG_ID = "MyOrg";
    static final String API_ID = "apiId";
    static final String API_NAME = "MyAPI";
    static final String API_VERSION = "3.14.159";

    @Mock
    PermissionService permissionService;

    MediaQueryServiceInMemory mediaService = new MediaQueryServiceInMemory();

    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    AuditDomainService auditService;

    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;

    WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    IntegrationCrudServiceInMemory integrationCrudService = new IntegrationCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();

    ApiExportDomainService apiExportDomainService;

    @InjectMocks
    ExportApiUseCase sut;

    AuditInfo auditInfo = AuditInfo.builder().organizationId(ORG_ID).environmentId("DEFAULT").build();

    @BeforeEach
    void setUp() {
        auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        apiPrimaryOwnerDomainService =
            new ApiPrimaryOwnerDomainService(
                auditService,
                groupQueryService,
                membershipCrudService,
                membershipQueryService,
                roleQueryService,
                userCrudService
            );
        apiExportDomainService =
            new ApiExportDomainServiceImpl(
                permissionService,
                mediaService,
                workflowCrudService,
                membershipCrudService,
                userCrudService,
                roleQueryService,
                metadataCrudService,
                pageQueryService,
                pageCrudService,
                apiCrudService,
                apiPrimaryOwnerDomainService,
                planCrudService,
                integrationCrudService,
                flowCrudService
            );
        sut = new ExportApiUseCase(apiExportDomainService);
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .id("role-id")
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId("role-id")
                    .memberType(Membership.Type.USER)
                    .memberId("user id")
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("user id").build()));
        lenient().when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                groupQueryService,
                membershipCrudService,
                membershipQueryService,
                roleQueryService,
                userCrudService,
                workflowCrudService,
                metadataCrudService,
                pageQueryService,
                apiCrudService,
                planCrudService,
                integrationCrudService,
                flowCrudService
            )
            .forEach(InMemoryAlternative::reset);
    }

    @ParameterizedTest
    @CsvSource(
        delimiterString = "|",
        textBlock = """
        MyAPI     |  3.14.159  | myapi-3.14.159.json
        My API    |  3.14 .159 | my-api-3.14-.159.json
        My    API |  3.14.159  | my-api-3.14.159.json
     """
    )
    void should_generate_excepted_filename(String name, String version, String expectedFilename) {
        // Given
        apiCrudService.initWith(List.of(v4Api(name, version)));
        var input = ExportApiUseCase.Input.of(API_ID, auditInfo, Set.of());

        // When
        var output = sut.execute(input);

        // Then
        assertThat(output.filename()).isEqualTo(expectedFilename);
    }

    @Test
    void should_export_native_api() {
        // Given
        apiCrudService.initWith(List.of(nativeApi()));
        var input = ExportApiUseCase.Input.of(API_ID, auditInfo, Set.of());

        // When
        var output = sut.execute(input);

        // Then
        assertThat(output.definition().api().name()).isEqualTo(API_NAME);
    }

    @Test
    void should_not_export_federated_API() {
        // Given
        apiCrudService.initWith(List.of(federatedApi()));
        var input = ExportApiUseCase.Input.of(API_ID, auditInfo, Set.of());

        // When
        var throwable = catchThrowable(() -> sut.execute(input));

        // Then
        assertThat(throwable).isInstanceOf(ApiDefinitionVersionNotSupportedException.class);
    }

    /**
     * It’s not the current behaviour of ApiExportDomainService, but it can become in future
     */
    @Test
    void should_throw_API_not_found_exception_if_export_return_null() {
        // Given
        var mockedApiExportDomainService = mock(ApiExportDomainService.class);
        sut = new ExportApiUseCase(mockedApiExportDomainService);
        var input = ExportApiUseCase.Input.of(API_ID, auditInfo, Set.of());

        // When
        var throwable = catchThrowable(() -> sut.execute(input));

        // Then
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    private static Api federatedApi() {
        return Api
            .builder()
            .id(API_ID)
            .version(API_VERSION)
            .name(API_NAME)
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.FEDERATED)
            .federatedApiDefinition(FederatedApi.builder().apiVersion(API_VERSION).build())
            .build();
    }

    private static Api v4Api(String name, String version) {
        return Api
            .builder()
            .id(API_ID)
            .version(version)
            .name(name)
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(io.gravitee.definition.model.v4.Api.builder().apiVersion(version).build())
            .build();
    }

    private static Api nativeApi() {
        return Api
            .builder()
            .id(API_ID)
            .version(API_VERSION)
            .name(API_NAME)
            .type(ApiType.NATIVE)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionNativeV4(NativeApi.builder().apiVersion(API_VERSION).build())
            .build();
    }
}
