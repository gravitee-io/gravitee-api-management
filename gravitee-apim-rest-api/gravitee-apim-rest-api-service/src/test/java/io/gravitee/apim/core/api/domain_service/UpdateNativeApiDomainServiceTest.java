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
package io.gravitee.apim.core.api.domain_service;

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.*;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.notification.model.hook.ApiDeprecatedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApiUpdatedApiHookContext;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateNativeApiDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String USER_ID = "user-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String MY_MEMBER_ID = "my-member-id";
    private static final String MEMBER_EMAIL = "one_valid@email.com";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService;
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    IndexerInMemory indexer = new IndexerInMemory();
    CategoryDomainService categoryDomainService = mock(CategoryDomainService.class);
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    UpdateNativeApiDomainService cut;

    @BeforeEach
    void setUp() {
        planQueryService = new PlanQueryServiceInMemory(planCrudService);

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId("api-po-id-organization-id")
                    .build()
            )
        );
        groupQueryService.initWith(
            List.of(
                Group
                    .builder()
                    .id("group-1")
                    .environmentId("environment-id")
                    .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id(MY_MEMBER_ID).email(MEMBER_EMAIL).build()));

        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        cut =
            new UpdateNativeApiDomainService(
                apiCrudService,
                planQueryService,
                new DeprecatePlanDomainService(planCrudService, auditDomainService),
                triggerNotificationDomainService,
                flowCrudService,
                categoryDomainService,
                auditDomainService,
                new ApiIndexerDomainService(
                    new ApiMetadataDecoderDomainService(
                        new ApiMetadataQueryServiceInMemory(metadataCrudService),
                        new FreemarkerTemplateProcessor()
                    ),
                    apiPrimaryOwnerService,
                    new ApiCategoryQueryServiceInMemory(),
                    indexer
                )
            );
    }

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(seed -> seed != null ? seed : "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    public void update_native_api_with_basic_configuration_info() {
        //given
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        String categoryKey = "categoryKey-1";
        var apiToUpdate = ApiFixtures
            .aNativeApi()
            .toBuilder()
            .name("updated-name")
            .description("updated-description")
            .version("2.0.0")
            .labels(List.of("label-1"))
            .categories(Set.of(categoryKey))
            .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
            .build();

        CategoryEntity categoryEntity = new CategoryEntity();
        String categoryId = "categoryId-1";
        categoryEntity.setId(categoryId);
        when(categoryDomainService.toCategoryId(any(), any())).thenReturn(Set.of(categoryId));
        when(categoryDomainService.toCategoryKey(any(), any())).thenReturn(Set.of(categoryKey));
        var ownerEntity = buildPrimaryOwnerEntity();

        //when
        var updatedApi = cut.update(
            apiToUpdate.getId(),
            old -> apiToUpdate,
            (oldApi, newApi) -> apiToUpdate,
            auditInfo,
            ownerEntity,
            oneShotIndexation(auditInfo)
        );
        //then
        assertThat(apiCrudService.storage().get(0)).extracting("apiDefinitionNativeV4").isNotNull();
        SoftAssertions.assertSoftly(soft -> {
            assertThat(updatedApi.getName()).isEqualTo("updated-name");
            assertThat(updatedApi.getDescription()).isEqualTo("updated-description");
            assertThat(updatedApi.getApiDefinitionNativeV4()).isNotNull();
            assertThat(updatedApi.getVersion()).isEqualTo("2.0.0");
            assertThat(updatedApi.getLabels()).containsExactly("label-1");
            assertThat(updatedApi.getCategories()).containsExactly(categoryKey);
            assertThat(updatedApi.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.PUBLISHED);
        });
    }

    @Test
    public void update_native_api_with_basic_configuration_info_without_category() {
        //given
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures
            .aNativeApi()
            .toBuilder()
            .name("updated-name")
            .description("updated-description")
            .version("2.0.0")
            .labels(List.of("label-1"))
            .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
            .build();

        CategoryEntity categoryEntity = new CategoryEntity();
        String categoryId = "categoryId-1";
        categoryEntity.setId(categoryId);
        var ownerEntity = buildPrimaryOwnerEntity();

        //when
        var updatedApi = cut.update(
            apiToUpdate.getId(),
            old -> apiToUpdate,
            (oldApi, newApi) -> apiToUpdate,
            auditInfo,
            ownerEntity,
            oneShotIndexation(auditInfo)
        );
        //then
        SoftAssertions.assertSoftly(soft -> {
            assertThat(updatedApi.getName()).isEqualTo("updated-name");
            assertThat(updatedApi.getDescription()).isEqualTo("updated-description");
            assertThat(updatedApi.getVersion()).isEqualTo("2.0.0");
            assertThat(updatedApi.getLabels()).containsExactly("label-1");
            assertThat(updatedApi.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.PUBLISHED);
        });
    }

    @Test
    public void update_throws_an_exception_when_update_native_api_not_found() {
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aNativeApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build();
        var ownerEntity = buildPrimaryOwnerEntity();

        assertThatExceptionOfType(ApiNotFoundException.class)
            .isThrownBy(() ->
                cut.update(
                    apiToUpdate.getId(),
                    old -> apiToUpdate,
                    (oldApi, newApi) -> apiToUpdate,
                    auditInfo,
                    ownerEntity,
                    oneShotIndexation(auditInfo)
                )
            )
            .withMessage("Api not found.");
    }

    @Test
    public void update_throws_an_exception_when_sanitizer_throws_error() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aNativeApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build();
        var ownerEntity = buildPrimaryOwnerEntity();

        assertThatExceptionOfType(ValidationDomainException.class)
            .isThrownBy(() ->
                cut.update(
                    apiToUpdate.getId(),
                    old -> apiToUpdate,
                    (oldApi, newApi) -> {
                        throw new ValidationDomainException("a sanitizer error");
                    },
                    auditInfo,
                    ownerEntity,
                    oneShotIndexation(auditInfo)
                )
            )
            .withMessage("a sanitizer error");
    }

    @Test
    public void update_creates_audit_log() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aNativeApi();
        var ownerEntity = buildPrimaryOwnerEntity();

        cut.update(
            apiToUpdate.getId(),
            old -> apiToUpdate,
            (oldApi, newApi) -> apiToUpdate,
            auditInfo,
            ownerEntity,
            oneShotIndexation(auditInfo)
        );

        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                // API Audit
                AuditEntity
                    .builder()
                    .id("generated-id")
                    .organizationId(ORGANIZATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId("my-api")
                    .user(USER_ID)
                    .properties(Collections.emptyMap())
                    .event(ApiAuditEvent.API_UPDATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    public void update_creates_index() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aNativeApi();
        var ownerEntity = buildPrimaryOwnerEntity();

        var updatedApi = cut.update(
            apiToUpdate.getId(),
            old -> apiToUpdate,
            (oldApi, newApi) -> apiToUpdate,
            auditInfo,
            ownerEntity,
            oneShotIndexation(auditInfo)
        );

        assertThat(indexer.storage())
            .isNotEmpty()
            .containsExactly(
                new IndexableApi(
                    updatedApi,
                    new PrimaryOwnerEntity(MY_MEMBER_ID, MEMBER_EMAIL, MEMBER_EMAIL, PrimaryOwnerEntity.Type.USER),
                    Map.of(),
                    Collections.emptySet()
                )
            );
    }

    @Test
    public void update_creates_flows() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aNativeApi();
        var nativeFlow = NativeFlow.builder().id("native-flow").build();
        apiToUpdate.setApiDefinitionNativeV4(apiToUpdate.getApiDefinitionNativeV4().toBuilder().flows(List.of(nativeFlow)).build());
        var ownerEntity = buildPrimaryOwnerEntity();

        var updatedApi = cut.update(
            apiToUpdate.getId(),
            old -> apiToUpdate,
            (oldApi, newApi) -> apiToUpdate,
            auditInfo,
            ownerEntity,
            oneShotIndexation(auditInfo)
        );

        assertThat(flowCrudService.storage()).isNotEmpty().containsExactly(nativeFlow);
        assertThat(updatedApi.getApiDefinitionNativeV4().getFlows()).containsExactly(nativeFlow);
    }

    @Test
    public void update_creates_api_updated_notification() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aNativeApi();
        var ownerEntity = buildPrimaryOwnerEntity();

        cut.update(
            apiToUpdate.getId(),
            old -> apiToUpdate,
            (oldApi, newApi) -> apiToUpdate,
            auditInfo,
            ownerEntity,
            oneShotIndexation(auditInfo)
        );

        assertThat(triggerNotificationDomainService.getApiNotifications())
            .containsExactly(new ApiUpdatedApiHookContext(apiToUpdate.getId()));
    }

    @Test
    public void update_to_be_deprecated_updates_plans() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        planCrudService.initWith(
            List.of(
                aNativePlanWithStatus(PlanStatus.PUBLISHED),
                aNativePlanWithStatus(PlanStatus.CLOSED),
                aNativePlanWithStatus(PlanStatus.STAGING),
                aNativePlanWithStatus(PlanStatus.DEPRECATED)
            )
        );
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aNativeApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.DEPRECATED).build();
        var ownerEntity = buildPrimaryOwnerEntity();

        cut.update(
            apiToUpdate.getId(),
            old -> apiToUpdate,
            (oldApi, newApi) -> apiToUpdate,
            auditInfo,
            ownerEntity,
            oneShotIndexation(auditInfo)
        );

        var planRepository = planCrudService.storage();

        assertThatPlanHasStatus(planRepository, "PUBLISHED", PlanStatus.DEPRECATED);
        assertThatPlanHasStatus(planRepository, "CLOSED", PlanStatus.CLOSED);
        assertThatPlanHasStatus(planRepository, "STAGING", PlanStatus.DEPRECATED);
        assertThatPlanHasStatus(planRepository, "DEPRECATED", PlanStatus.DEPRECATED);
    }

    @Test
    public void update_to_be_deprecated_triggers_deprecated_api_notification() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var apiToUpdate = ApiFixtures.aNativeApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.DEPRECATED).build();
        var ownerEntity = buildPrimaryOwnerEntity();

        cut.update(
            apiToUpdate.getId(),
            old -> apiToUpdate,
            (oldApi, newApi) -> apiToUpdate,
            auditInfo,
            ownerEntity,
            oneShotIndexation(auditInfo)
        );

        assertThat(triggerNotificationDomainService.getApiNotifications())
            .containsExactly(new ApiDeprecatedApiHookContext(apiToUpdate.getId()), new ApiUpdatedApiHookContext(apiToUpdate.getId()));
    }

    PrimaryOwnerEntity buildPrimaryOwnerEntity() {
        return new PrimaryOwnerEntity(MY_MEMBER_ID, MEMBER_EMAIL, MEMBER_EMAIL, PrimaryOwnerEntity.Type.USER);
    }

    Plan aNativePlanWithStatus(PlanStatus status) {
        return Plan
            .builder()
            .id(status.name())
            .definitionVersion(DefinitionVersion.V4)
            .apiId(ApiFixtures.aNativeApi().getId())
            .apiType(ApiType.NATIVE)
            .planDefinitionNativeV4(NativePlan.builder().status(status).build())
            .build();
    }

    private static void assertThatPlanHasStatus(List<Plan> planRepository, String planId, PlanStatus expectedStatus) {
        assertThat(planRepository.stream().filter(p -> Objects.equals(planId, p.getId())).findFirst())
            .isNotEmpty()
            .get()
            .extracting(Plan::getPlanDefinitionNativeV4)
            .hasFieldOrPropertyWithValue("status", expectedStatus);
    }
}
