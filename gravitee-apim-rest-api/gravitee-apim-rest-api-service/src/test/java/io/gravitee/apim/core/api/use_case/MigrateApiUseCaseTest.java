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

import static io.gravitee.apim.core.api.use_case.MigrateApiUseCase.Input.UpgradeMode.DRY_RUN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PrimaryOwnerDomainServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.api.ApiStateDomainServiceLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MigrateApiUseCaseTest {

    private static final String API_ID = "api-id";
    private static final String USER_ID = "user-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ROLE_ID = "role-id";
    private static final AuditInfo AUDIT_INFO = AuditInfo
        .builder()
        .environmentId(ENVIRONMENT_ID)
        .organizationId(ORGANIZATION_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    private final ApiStateService apiStateService = mock(ApiStateService.class);
    private final ApiService apiService = mock(ApiService.class);
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final IndexerInMemory indexer = new IndexerInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final PrimaryOwnerDomainServiceInMemory primaryOwnerDomainService = new PrimaryOwnerDomainServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    private final ApiCategoryQueryServiceInMemory apiCategoryQueryService = new ApiCategoryQueryServiceInMemory();

    private final AuditDomainService auditDomainService = new AuditDomainService(
        auditCrudService,
        userCrudService,
        new JacksonJsonDiffProcessor()
    );
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
        auditDomainService,
        groupQueryService,
        membershipCrudService,
        membershipQueryService,
        roleQueryService,
        userCrudService
    );
    private final ApiIndexerDomainService apiIndexerDomainService = new ApiIndexerDomainService(
        new ApiMetadataDecoderDomainService(apiMetadataQueryService, new FreemarkerTemplateProcessor()),
        apiPrimaryOwnerDomainService,
        apiCategoryQueryService,
        indexer
    );
    private final ApiStateDomainService apiStateDomainService = new ApiStateDomainServiceLegacyWrapper(apiStateService, apiService);

    private final MigrateApiUseCase useCase = new MigrateApiUseCase(
        apiCrudService,
        auditDomainService,
        apiIndexerDomainService,
        apiPrimaryOwnerDomainService,
        planCrudService,
        apiStateDomainService
    );

    @BeforeEach
    void setUp() {
        lenient().when(apiService.isSynchronized(any(), any())).thenReturn(true);
        primaryOwnerDomainService.add(
            API_ID,
            PrimaryOwnerEntity.builder().id(USER_ID).displayName("User").type(PrimaryOwnerEntity.Type.USER).build()
        );
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .name("PRIMARY_OWNER")
                    .id(ROLE_ID)
                    .referenceId(ORGANIZATION_ID)
                    .build()
            )
        );
        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId(ROLE_ID)
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id(USER_ID).firstname("John").lastname("Doe").build()));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudService,
                auditCrudService,
                indexer,
                planCrudService,
                primaryOwnerDomainService,
                userCrudService,
                groupQueryService,
                apiMetadataQueryService,
                membershipQueryService,
                roleQueryService,
                membershipCrudService,
                apiCategoryQueryService
            )
            .forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_fail_when_api_not_found() {
        // Given
        // don’t initialize repositories with API

        // When
        Throwable throwable = catchThrowable(() -> useCase.execute(new MigrateApiUseCase.Input(API_ID, null, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
        assertThat(((ApiNotFoundException) throwable).getId()).isEqualTo(API_ID);
    }

    @Test
    void should_return_fail_when_api_is_not_v2() {
        // Given
        var api = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        apiCrudService.initWith(List.of(api));

        // When
        var result = useCase.execute(new MigrateApiUseCase.Input(API_ID, null, AUDIT_INFO));

        // Then
        assertThat(result.state()).isEqualTo(MigrationResult.State.IMPOSSIBLE);
        assertThat(result.apiId()).isEqualTo(API_ID);
        assertThat(result.issues())
            .containsExactly(
                new MigrationResult.Issue("Cannot upgrade an API which is not a v2 definition", MigrationResult.State.IMPOSSIBLE)
            );
    }

    @Test
    void should_return_fail_when_api_is_not_synchronized() {
        // Given
        when(apiService.isSynchronized(any(), any())).thenReturn(false);
        var api = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).build();
        api.getApiDefinition().setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        apiCrudService.initWith(List.of(api));

        // When
        var result = useCase.execute(new MigrateApiUseCase.Input(API_ID, null, AUDIT_INFO));

        // Then
        assertThat(result.state()).isEqualTo(MigrationResult.State.CAN_BE_FORCED);
        assertThat(result.apiId()).isEqualTo(API_ID);
        assertThat(result.issues())
            .containsExactly(new MigrationResult.Issue("Cannot upgrade an API which is out of sync", MigrationResult.State.CAN_BE_FORCED));
    }

    @Test
    void should_return_fail_when_api_dont_use_v4_emulation_engine() {
        // Given
        var api = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).build();
        apiCrudService.initWith(List.of(api));

        // When
        var result = useCase.execute(new MigrateApiUseCase.Input(API_ID, null, AUDIT_INFO));

        // Then
        assertThat(result.state()).isEqualTo(MigrationResult.State.IMPOSSIBLE);
        assertThat(result.apiId()).isEqualTo(API_ID);
        assertThat(result.issues())
            .containsExactly(new MigrationResult.Issue("Cannot migrate an API not using V4 emulation", MigrationResult.State.IMPOSSIBLE));
    }

    @Test
    void should_not_upgrade_api_in_dry_run_mode() {
        // Given
        var v2Api = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).definitionVersion(DefinitionVersion.V2).build();
        v2Api.getApiDefinition().setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        apiCrudService.initWith(List.of(v2Api));

        var plan = PlanFixtures.aPlanV2().toBuilder().id("plan-id").apiId(API_ID).build();
        planCrudService.initWith(List.of(plan));

        // When
        var result = useCase.execute(new MigrateApiUseCase.Input(API_ID, DRY_RUN, AUDIT_INFO));

        // Then
        assertThat(result.state()).isEqualTo(MigrationResult.State.MIGRATABLE);
        assertThat(result.apiId()).isEqualTo(API_ID);

        var storedApi = apiCrudService.findById(API_ID);
        assertThat(storedApi).map(Api::getDefinitionVersion).hasValue(DefinitionVersion.V2);

        assertThat(auditCrudService.storage()).isEmpty();

        assertThat(indexer.storage()).isEmpty();
    }

    @Test
    void should_upgrade_api_definition() {
        // Given
        var v2Api = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).build();
        v2Api.getApiDefinition().setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        apiCrudService.initWith(List.of(v2Api));

        var plan = PlanFixtures.aPlanV2().toBuilder().id("plan-id").apiId(API_ID).build();
        planCrudService.initWith(List.of(plan));

        // When
        var result = useCase.execute(new MigrateApiUseCase.Input(API_ID, null, AUDIT_INFO));

        // Then
        assertThat(result.state()).isEqualTo(MigrationResult.State.MIGRATED);
        assertThat(result.apiId()).isEqualTo(API_ID);

        var upgradedApiOpt = apiCrudService.findById(API_ID);
        assertThat(upgradedApiOpt).hasValueSatisfying(MigrateApiUseCaseTest::assertApiV4);

        var upgradedPlans = planCrudService.findByApiId(API_ID);
        assertThat(upgradedPlans).hasSize(1).first().satisfies(MigrateApiUseCaseTest::assertPlanV4);

        assertThat(auditCrudService.storage()).hasSize(1);
        var auditLog = auditCrudService.storage().getFirst();
        assertThat(auditLog.getEvent()).isEqualTo(ApiAuditEvent.API_UPDATED.name());
        assertThat(auditLog.getReferenceId()).isEqualTo(API_ID);
        assertThat(auditLog.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(auditLog.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);

        assertThat(indexer.storage()).hasSize(1);
    }

    @Test
    void should_upgrade_api_with_multiple_plans() {
        // Given
        var v2Api = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).build();
        v2Api.getApiDefinition().setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        apiCrudService.initWith(List.of(v2Api));

        var plan1 = PlanFixtures.aPlanV2().toBuilder().id("plan-1").apiId(API_ID).build();
        var plan2 = PlanFixtures.aPlanV2().toBuilder().id("plan-2").apiId(API_ID).build();
        planCrudService.initWith(List.of(plan1, plan2));

        var primaryOwner = PrimaryOwnerEntity.builder().id(USER_ID).displayName("User").type(PrimaryOwnerEntity.Type.USER).build();
        primaryOwnerDomainService.add(API_ID, primaryOwner);

        var user = BaseUserEntity.builder().id(USER_ID).firstname("John").lastname("Doe").build();
        userCrudService.initWith(List.of(user));

        // When
        var result = useCase.execute(new MigrateApiUseCase.Input(API_ID, null, AUDIT_INFO));

        // Then
        assertThat(result.state()).isEqualTo(MigrationResult.State.MIGRATED);

        var upgradedApiOpt = apiCrudService.findById(API_ID);
        assertThat(upgradedApiOpt).hasValueSatisfying(MigrateApiUseCaseTest::assertApiV4);

        var upgradedPlans = planCrudService.findByApiId(API_ID);
        assertThat(upgradedPlans).hasSize(2).extracting(Plan::getId).containsExactlyInAnyOrder("plan-1", "plan-2");
        assertThat(upgradedPlans).allSatisfy(MigrateApiUseCaseTest::assertPlanV4);
    }

    @Test
    void should_upgrade_api_with_no_plans() {
        // Given
        var v2Api = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).build();
        v2Api.getApiDefinition().setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        apiCrudService.initWith(List.of(v2Api));

        var primaryOwner = PrimaryOwnerEntity.builder().id(USER_ID).displayName("User").type(PrimaryOwnerEntity.Type.USER).build();
        primaryOwnerDomainService.add(API_ID, primaryOwner);

        var user = BaseUserEntity.builder().id(USER_ID).firstname("John").lastname("Doe").build();
        userCrudService.initWith(List.of(user));

        // When
        var result = useCase.execute(new MigrateApiUseCase.Input(API_ID, null, AUDIT_INFO));

        // Then
        assertThat(result.state()).isEqualTo(MigrationResult.State.MIGRATED);

        var upgradedApiOpt = apiCrudService.findById(API_ID);
        assertThat(upgradedApiOpt).hasValueSatisfying(MigrateApiUseCaseTest::assertApiV4);

        assertThat(planCrudService.storage()).isEmpty();
    }

    private static void assertApiV4(Api upgradedApi) {
        assertSoftly(softly -> {
            softly.assertThat(upgradedApi.getId()).as("id").isEqualTo(API_ID);
            softly.assertThat(upgradedApi.getName()).as("name").isEqualTo("My Api");
            softly.assertThat(upgradedApi.getEnvironmentId()).as("environment id").isEqualTo("environment-id");
            softly.assertThat(upgradedApi.getCrossId()).as("cross id").isEqualTo("my-api-crossId");
            softly.assertThat(upgradedApi.getDescription()).as("description").isEqualTo("api-description");
            softly.assertThat(upgradedApi.getVersion()).as("version").isEqualTo("1.0.0");
            softly
                .assertThat(upgradedApi.getOriginContext())
                .as("origin context")
                .isEqualTo(new io.gravitee.rest.api.model.context.OriginContext.Management());
            softly.assertThat(upgradedApi.getDefinitionVersion()).as("definition version").isEqualTo(DefinitionVersion.V4);
            softly.assertThat(upgradedApi.getType()).as("type").isEqualTo(io.gravitee.definition.model.v4.ApiType.PROXY);
            softly.assertThat(upgradedApi.getVisibility()).as("visibility").isEqualTo(Api.Visibility.PUBLIC);
            softly.assertThat(upgradedApi.getApiLifecycleState()).as("api lifecycle state").isEqualTo(Api.ApiLifecycleState.PUBLISHED);
            softly.assertThat(upgradedApi.getPicture()).as("picture").isEqualTo("api-picture");
            softly.assertThat(upgradedApi.getBackground()).as("background").isEqualTo("api-background");
            softly.assertThat(upgradedApi.getGroups()).as("groups").containsOnly("group-1");
            softly.assertThat(upgradedApi.getCategories()).as("categories").containsOnly("category-1");
            softly.assertThat(upgradedApi.getLabels()).as("labels").containsOnly("label-1");
            softly.assertThat(upgradedApi.isDisableMembershipNotifications()).as("disable membership notifications").isTrue();
        });
    }

    private static void assertPlanV4(Plan upgradedPlan) {
        assertSoftly(softly -> {
            softly.assertThat(upgradedPlan.getName()).as("plan name").isEqualTo("My plan");
            softly.assertThat(upgradedPlan.getDescription()).as("plan description").isEqualTo("Description");
            softly.assertThat(upgradedPlan.getApiId()).as("plan api id").isEqualTo(API_ID);
            softly.assertThat(upgradedPlan.getCrossId()).as("plan cross id").isNotBlank();
            softly.assertThat(upgradedPlan.getOrder()).as("plan order").isEqualTo(1);
            softly.assertThat(upgradedPlan.getType()).as("plan type").isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanType.API);
            softly
                .assertThat(upgradedPlan.getValidation())
                .as("plan validation")
                .isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.AUTO);
            softly.assertThat(upgradedPlan.getDefinitionVersion()).as("plan definition version").isEqualTo(DefinitionVersion.V4);
            softly.assertThat(upgradedPlan.getApiType()).as("plan api type").isEqualTo(io.gravitee.definition.model.v4.ApiType.PROXY);
            softly.assertThat(upgradedPlan.getPlanDefinitionV2()).as("plan v2 definition").isNull();
            softly.assertThat(upgradedPlan.getPlanDefinitionHttpV4()).as("plan v4 definition").isNotNull();
            softly.assertThat(upgradedPlan.getPlanDefinitionHttpV4().getSecurity()).as("plan v4 security").isNotNull();
            softly.assertThat(upgradedPlan.getPlanDefinitionHttpV4().getStatus()).as("plan v4 status").isNotNull();
            softly.assertThat(upgradedPlan.getPlanMode()).as("plan mode").isEqualTo(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
        });
    }
}
