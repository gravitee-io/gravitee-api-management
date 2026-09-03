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
package io.gravitee.apim.core.promotion.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PromotionFixtures;
import initializers.ImportDefinitionCreateDomainServiceTestInitializer;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PromotionCrudServiceInMemory;
import inmemory.PromotionQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionUpdateDomainServiceTestInitializer;
import io.gravitee.apim.core.api.exception.ApiImportedWithErrorException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.cockpit.model.CockpitReplyStatus;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.promotion.domain_service.PromotionContextDomainService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProcessPromotionUseCaseTest {

    private static final String API_ID = "api-id";
    private static final String CROSS_ID = "cross-id";
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "TARGET-ENV-ID";
    private static final Api API_V2 = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).build();
    private static final Promotion PROMOTION = PromotionFixtures.aPromotion()
        .toBuilder()
        .apiId(API_ID)
        .status(PromotionStatus.TO_BE_VALIDATED)
        .build();
    private static final Environment ENVIRONMENT = Environment.builder()
        .id(ENVIRONMENT_ID)
        .cockpitId(PROMOTION.getTargetEnvCockpitId())
        .organizationId(ORGANIZATION_ID)
        .build();

    private static final String USER_NAME = "user";

    private static final AuditInfo AUDIT = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId(USER_NAME).build())
        .build();

    private static final BaseUserEntity BASE_USER_ENTITY = BaseUserEntity.builder().id(USER_NAME).build();
    private static final String UUID = "generated-id";

    private final PromotionCrudServiceInMemory promotionCrudService = new PromotionCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final ImportDefinitionCreateDomainServiceTestInitializer importDefinitionCreateDomainService =
        new ImportDefinitionCreateDomainServiceTestInitializer(apiCrudServiceInMemory);
    private final ImportDefinitionUpdateDomainServiceTestInitializer importDefinitionUpdateDomainService =
        new ImportDefinitionUpdateDomainServiceTestInitializer(apiCrudServiceInMemory);
    private final EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    private final PromotionQueryServiceInMemory promotionQueryService = new PromotionQueryServiceInMemory(promotionCrudService);
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudServiceInMemory);
    private PromotionContextDomainService promotionContextDomainService;
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private AuditDomainService auditService;
    private CockpitPromotionServiceProvider cockpitPromotionServiceProvider;
    private ProcessPromotionUseCase useCase;

    @BeforeEach
    public void setUp() {
        cockpitPromotionServiceProvider = mock(CockpitPromotionServiceProvider.class);
        auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());

        useCase = new ProcessPromotionUseCase(
            promotionCrudService,
            cockpitPromotionServiceProvider,
            importDefinitionCreateDomainService.initialize(),
            importDefinitionUpdateDomainService.initialize(ENVIRONMENT_ID),
            auditService
        );

        promotionContextDomainService = new PromotionContextDomainService(
            promotionCrudService,
            promotionQueryService,
            apiQueryService,
            apiCrudServiceInMemory,
            environmentCrudService,
            new JsonMapper()
        );

        UuidString.overrideGenerator(() -> UUID);
    }

    @AfterEach
    public void cleanUp() {
        Stream.of(promotionCrudService, apiCrudServiceInMemory, auditCrudService, userCrudService).forEach(InMemoryAlternative::reset);
        UuidString.reset();
    }

    @Test
    void should_process_v2_api_promotion() {
        when(cockpitPromotionServiceProvider.process(PROMOTION.getId(), true)).thenReturn(PROMOTION);
        var result = useCase.execute(new ProcessPromotionUseCase.Input(PROMOTION, true, API_V2.getDefinitionVersion()));

        verify(cockpitPromotionServiceProvider).process(eq(PROMOTION.getId()), eq(true));
        assertThat(result.promotion()).isEqualTo(PROMOTION);
    }

    @Test
    void should_throw_exception_when_api_definition_is_not_supported() {
        promotionCrudService.initWith(List.of(PROMOTION));

        Throwable throwable = catchThrowable(() ->
            useCase.execute(new ProcessPromotionUseCase.Input(PROMOTION, true, DefinitionVersion.FEDERATED))
        );

        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Only V2 and V4 API definition are supported");
    }

    @Test
    void should_reject_v4_api_promotion() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        when(cockpitPromotionServiceProvider.processPromotion(eq(ORGANIZATION_ID), eq(ENVIRONMENT_ID), any())).thenReturn(
            CockpitReplyStatus.SUCCEEDED
        );

        var result = useCase.execute(
            new ProcessPromotionUseCase.Input(
                PROMOTION,
                DefinitionVersion.V4,
                false,
                null,
                ImportDefinition.builder().apiExport(ApiExport.builder().id(API_ID).crossId(CROSS_ID).build()).build(),
                AUDIT
            )
        );

        assertThat(result).isNotNull();
        assertThat(result.promotion()).isNotNull();
        assertThat(result.promotion().getStatus()).isEqualTo(PromotionStatus.REJECTED);
        verify(cockpitPromotionServiceProvider, never()).requestPromotion(any(), any(), any());
    }

    @Test
    void should_throw_exception_when_v4_api_promotion_command_fails() {
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        when(cockpitPromotionServiceProvider.processPromotion(eq(ORGANIZATION_ID), eq(ENVIRONMENT_ID), any())).thenReturn(
            CockpitReplyStatus.ERROR
        );

        Throwable throwable = catchThrowable(() ->
            useCase.execute(new ProcessPromotionUseCase.Input(PROMOTION, DefinitionVersion.V4, false, null, null, AUDIT))
        );

        assertThat(throwable)
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("An error occurs while sending promotion promotion-id request to cockpit");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_throw_exception_when_v4_import_definition_cross_id_is_empty(String crossId) {
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        Throwable throwable = catchThrowable(() ->
            useCase.execute(
                new ProcessPromotionUseCase.Input(
                    PROMOTION,
                    DefinitionVersion.V4,
                    true,
                    null,
                    ImportDefinition.builder().apiExport(ApiExport.builder().id(API_ID).crossId(crossId).build()).build(),
                    AUDIT
                )
            )
        );

        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Promotion promotion-id failed. A crossId is required to promote an API");
    }

    @Test
    void should_update_v4_api_using_promotion() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var v4proxyApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).crossId(CROSS_ID).build();
        var aleadyPromotedApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(API_ID)
            .crossId(CROSS_ID)
            .environmentId(ENVIRONMENT.getId())
            .build();
        apiCrudServiceInMemory.initWith(List.of(v4proxyApi, aleadyPromotedApi));

        when(cockpitPromotionServiceProvider.processPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        var result = useCase.execute(
            new ProcessPromotionUseCase.Input(
                PROMOTION,
                DefinitionVersion.V4,
                true,
                ApiFixtures.aProxyApiV4().toBuilder().id("already-promoted-api").crossId(CROSS_ID).environmentId(ENVIRONMENT_ID).build(),
                ImportDefinition.builder().apiExport(ApiExport.builder().id(API_ID).crossId(CROSS_ID).build()).build(),
                AUDIT
            )
        );
        assertThat(result).isNotNull();
        assertThat(result.promotion().getStatus()).isEqualTo(PromotionStatus.ACCEPTED);
        assertThat(result.promotion().getTargetApiId()).isEqualTo("already-promoted-api");
    }

    @Test
    void should_promote_proxy_v4_api() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var v4proxyApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).crossId(CROSS_ID).build();
        apiCrudServiceInMemory.initWith(List.of(v4proxyApi));

        when(cockpitPromotionServiceProvider.processPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        importDefinitionCreateDomainService.parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                ),
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        importDefinitionCreateDomainService.userCrudService.initWith(List.of(BASE_USER_ENTITY));
        when(
            importDefinitionCreateDomainService.validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any())
        ).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(apiCrudServiceInMemory.storage()).hasSize(1);
        var result = useCase.execute(
            new ProcessPromotionUseCase.Input(
                PROMOTION,
                DefinitionVersion.V4,
                true,
                null,
                ImportDefinition.builder().apiExport(ApiExport.builder().crossId(CROSS_ID).build()).build(),
                AUDIT
            )
        );

        assertThat(result).isNotNull();
        assertThat(result.promotion()).isNotNull();
        assertThat(result.promotion().getStatus()).isEqualTo(PromotionStatus.ACCEPTED);
        assertThat(result.promotion().getTargetApiId()).isEqualTo(UUID);
        assertThat(apiCrudServiceInMemory.storage()).hasSize(2);
        assertThat(apiCrudServiceInMemory.get(UUID))
            .isNotNull()
            .satisfies(api -> {
                assertThat(api.getId()).isEqualTo(UUID);
                assertThat(api.getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.CREATED);
                assertThat(api.getLifecycleState()).isEqualTo(Api.LifecycleState.STOPPED);
            });
    }

    @Test
    void should_create_a_new_api_instead_of_updating_when_existing_promoted_api_was_not_resolved() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var alreadyInTarget = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId("different-cross-id")
            .environmentId(ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(alreadyInTarget));

        when(cockpitPromotionServiceProvider.processPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);
        importDefinitionCreateDomainService.parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                ),
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        importDefinitionCreateDomainService.userCrudService.initWith(List.of(BASE_USER_ENTITY));
        when(
            importDefinitionCreateDomainService.validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any())
        ).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(
            new ProcessPromotionUseCase.Input(
                PROMOTION,
                DefinitionVersion.V4,
                true,
                null,
                ImportDefinition.builder().apiExport(ApiExport.builder().crossId(CROSS_ID).build()).build(),
                AUDIT
            )
        );

        assertThat(result.promotion().getStatus()).isEqualTo(PromotionStatus.ACCEPTED);
        assertThat(result.promotion().getTargetApiId()).isEqualTo(UUID);
        assertThat(apiCrudServiceInMemory.storage()).hasSize(2);
        assertThat(apiCrudServiceInMemory.get("already-promoted-api").getCrossId()).isEqualTo("different-cross-id");
        assertThat(apiCrudServiceInMemory.get(UUID)).isNotNull();
    }

    @Test
    void should_update_existing_target_api_when_resolved_even_if_crossId_differs() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var alreadyInTarget = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId("different-cross-id")
            .environmentId(ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(alreadyInTarget));

        when(cockpitPromotionServiceProvider.processPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        var result = useCase.execute(
            new ProcessPromotionUseCase.Input(
                PROMOTION,
                DefinitionVersion.V4,
                true,
                alreadyInTarget,
                ImportDefinition.builder().apiExport(ApiExport.builder().id(API_ID).crossId(CROSS_ID).build()).build(),
                AUDIT
            )
        );

        assertThat(result.promotion().getStatus()).isEqualTo(PromotionStatus.ACCEPTED);
        assertThat(result.promotion().getTargetApiId()).isEqualTo("already-promoted-api");
        assertThat(apiCrudServiceInMemory.storage()).hasSize(1);
        assertThat(apiCrudServiceInMemory.get("already-promoted-api")).isNotNull();
        verify(cockpitPromotionServiceProvider).processPromotion(any(), any(), any());
    }

    @Test
    void should_update_resolved_target_api_in_place_when_export_has_no_id_and_crossId_differs() {
        // Real promotions export the definition without ids (Excludable.IDS). When the target API was resolved through
        // the promotion history, its crossId differs from the definition one: the update must still hit that API.
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var alreadyInTarget = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId("different-cross-id")
            .environmentId(ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(alreadyInTarget));

        when(cockpitPromotionServiceProvider.processPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        var result = useCase.execute(
            new ProcessPromotionUseCase.Input(
                PROMOTION,
                DefinitionVersion.V4,
                true,
                alreadyInTarget,
                ImportDefinition.builder().apiExport(ApiExport.builder().crossId(CROSS_ID).name("promoted name").build()).build(),
                AUDIT
            )
        );

        assertThat(result.promotion().getStatus()).isEqualTo(PromotionStatus.ACCEPTED);
        assertThat(result.promotion().getTargetApiId()).isEqualTo("already-promoted-api");
        assertThat(apiCrudServiceInMemory.storage()).hasSize(1);
        verify(importDefinitionUpdateDomainService.apiService).update(
            any(),
            eq("already-promoted-api"),
            argThat(update -> CROSS_ID.equals(update.getCrossId()) && "promoted name".equals(update.getName())),
            eq(false),
            eq(USER_NAME)
        );
    }

    @Test
    void should_refuse_to_update_a_target_api_that_belongs_to_another_environment() {
        var promotion = aPendingPromotion();
        promotionCrudService.initWith(List.of(promotion));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var apiInAnotherEnv = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("api-in-another-env")
            .crossId(CROSS_ID)
            .environmentId("ANOTHER-ENV-ID")
            .build();
        apiCrudServiceInMemory.initWith(List.of(apiInAnotherEnv));

        Throwable throwable = catchThrowable(() ->
            useCase.execute(
                new ProcessPromotionUseCase.Input(
                    promotion,
                    DefinitionVersion.V4,
                    true,
                    apiInAnotherEnv,
                    ImportDefinition.builder().apiExport(ApiExport.builder().crossId(CROSS_ID).build()).build(),
                    AUDIT
                )
            )
        );

        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("api-in-another-env")
            .hasMessageContaining(ENVIRONMENT_ID);
        assertThat(apiCrudServiceInMemory.storage()).hasSize(1);
        verify(importDefinitionUpdateDomainService.apiService, never()).update(any(), any(), any(), anyBoolean(), any());
        verify(cockpitPromotionServiceProvider, never()).processPromotion(any(), any(), any());
        assertThat(promotionCrudService.storage().getFirst().getStatus()).isEqualTo(PromotionStatus.TO_BE_VALIDATED);
    }

    @Test
    void should_refuse_to_update_a_target_api_with_no_environment_id() {
        var promotion = aPendingPromotion();
        promotionCrudService.initWith(List.of(promotion));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var apiWithNoEnv = ApiFixtures.aProxyApiV4().toBuilder().id("api-with-no-env").crossId(CROSS_ID).environmentId(null).build();
        apiCrudServiceInMemory.initWith(List.of(apiWithNoEnv));

        Throwable throwable = catchThrowable(() ->
            useCase.execute(
                new ProcessPromotionUseCase.Input(
                    promotion,
                    DefinitionVersion.V4,
                    true,
                    apiWithNoEnv,
                    ImportDefinition.builder().apiExport(ApiExport.builder().crossId(CROSS_ID).build()).build(),
                    AUDIT
                )
            )
        );

        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("api-with-no-env")
            .hasMessageContaining(ENVIRONMENT_ID);
        verify(importDefinitionUpdateDomainService.apiService, never()).update(any(), any(), any(), anyBoolean(), any());
        verify(cockpitPromotionServiceProvider, never()).processPromotion(any(), any(), any());
        assertThat(promotionCrudService.storage().getFirst().getStatus()).isEqualTo(PromotionStatus.TO_BE_VALIDATED);
    }

    @Test
    void should_not_accept_nor_notify_cockpit_when_the_target_api_update_fails() {
        var promotion = aPendingPromotion();
        promotionCrudService.initWith(List.of(promotion));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var alreadyInTarget = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId(CROSS_ID)
            .environmentId(ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(alreadyInTarget));
        when(importDefinitionUpdateDomainService.apiService.update(any(), any(), any(), anyBoolean(), any())).thenThrow(
            new IllegalStateException("update failed")
        );

        Throwable throwable = catchThrowable(() ->
            useCase.execute(
                new ProcessPromotionUseCase.Input(
                    promotion,
                    DefinitionVersion.V4,
                    true,
                    alreadyInTarget,
                    ImportDefinition.builder().apiExport(ApiExport.builder().crossId(CROSS_ID).build()).build(),
                    AUDIT
                )
            )
        );

        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("update failed");
        verify(cockpitPromotionServiceProvider, never()).processPromotion(any(), any(), any());
        assertThat(promotionCrudService.storage().getFirst().getStatus()).isEqualTo(PromotionStatus.TO_BE_VALIDATED);
        assertThat(promotionCrudService.storage().getFirst().getTargetApiId()).isNull();
    }

    @Test
    void should_not_persist_promotion_when_cockpit_rejects_the_processed_promotion() {
        var stored = aPendingPromotion();
        var input = aPendingPromotion();
        promotionCrudService.initWith(List.of(stored));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var alreadyInTarget = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId(CROSS_ID)
            .environmentId(ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(alreadyInTarget));
        when(cockpitPromotionServiceProvider.processPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.ERROR);

        Throwable throwable = catchThrowable(() ->
            useCase.execute(
                new ProcessPromotionUseCase.Input(
                    input,
                    DefinitionVersion.V4,
                    true,
                    alreadyInTarget,
                    ImportDefinition.builder().apiExport(ApiExport.builder().crossId(CROSS_ID).build()).build(),
                    AUDIT
                )
            )
        );

        assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        // The stored promotion is untouched: no ACCEPTED status and no targetApiId without a successful Cockpit reply.
        assertThat(promotionCrudService.storage().getFirst().getStatus()).isEqualTo(PromotionStatus.TO_BE_VALIDATED);
        assertThat(promotionCrudService.storage().getFirst().getTargetApiId()).isNull();
    }

    @Test
    void should_create_promotion_audit_log_in_target_environment_when_v4_promotion_is_accepted() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var v4proxyApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).crossId(CROSS_ID).build();
        var existingApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId(CROSS_ID)
            .environmentId(ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(v4proxyApi, existingApi));

        when(cockpitPromotionServiceProvider.processPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        useCase.execute(
            new ProcessPromotionUseCase.Input(
                PROMOTION,
                DefinitionVersion.V4,
                true,
                existingApi,
                ImportDefinition.builder().apiExport(ApiExport.builder().id(API_ID).crossId(CROSS_ID).build()).build(),
                AUDIT
            )
        );

        assertThat(auditCrudService.storage()).anySatisfy(audit -> {
            assertThat(audit.getReferenceId()).isEqualTo(API_ID);
            assertThat(audit.getEvent()).isEqualTo(ApiAuditEvent.PROMOTION_PROCESSED.name());
            assertThat(audit.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
            assertThat(audit.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
            assertThat(audit.getUser()).isEqualTo(USER_NAME);
        });
    }

    @Test
    void should_create_promotion_audit_log_in_target_environment_when_v4_promotion_is_rejected() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        when(cockpitPromotionServiceProvider.processPromotion(eq(ORGANIZATION_ID), eq(ENVIRONMENT_ID), any())).thenReturn(
            CockpitReplyStatus.SUCCEEDED
        );

        useCase.execute(
            new ProcessPromotionUseCase.Input(
                PROMOTION,
                DefinitionVersion.V4,
                false,
                null,
                ImportDefinition.builder().apiExport(ApiExport.builder().id(API_ID).crossId(CROSS_ID).build()).build(),
                AUDIT
            )
        );

        assertThat(auditCrudService.storage())
            .hasSize(1)
            .first()
            .satisfies(audit -> {
                assertThat(audit.getReferenceId()).isEqualTo(API_ID);
                assertThat(audit.getEvent()).isEqualTo(ApiAuditEvent.PROMOTION_PROCESSED.name());
                assertThat(audit.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
                assertThat(audit.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                assertThat(audit.getUser()).isEqualTo(USER_NAME);
            });
    }

    @Test
    void should_return_promotion_subentities_errors() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var v4proxyApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).crossId(CROSS_ID).build();
        var aleadyPromotedApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(API_ID)
            .crossId(CROSS_ID)
            .environmentId(ENVIRONMENT.getId())
            .build();
        apiCrudServiceInMemory.initWith(List.of(v4proxyApi, aleadyPromotedApi));

        Throwable throwable = catchThrowable(() ->
            useCase.execute(
                new ProcessPromotionUseCase.Input(
                    PROMOTION,
                    DefinitionVersion.V4,
                    true,
                    ApiFixtures.aProxyApiV4()
                        .toBuilder()
                        .id("already-promoted-api")
                        .crossId(CROSS_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .build(),
                    ImportDefinition.builder()
                        .apiExport(ApiExport.builder().id(API_ID).crossId(CROSS_ID).build())
                        .plans(Set.of(new PlanWithFlows()))
                        .metadata(Set.of(new NewApiMetadata()))
                        .pages(List.of(new Page()))
                        .build(),
                    AUDIT
                )
            )
        );

        assertThat(throwable)
            .isInstanceOf(ApiImportedWithErrorException.class)
            .hasMessageContainingAll(
                "API imported with error:",
                "(Metadata) null",
                "(Plans) Cannot invoke \"io.gravitee.definition.model.DefinitionVersion.ordinal()\""
            );
    }

    /**
     * The scenario an environment upgrading from an affected version is in: the API was promoted before the pipeline
     * persisted {@code targetApiId}, so every accepted promotion row has {@code target_api_id = NULL}, and the crossId
     * has since diverged. Resolution runs for real here (through {@link PromotionContextDomainService}) rather than
     * being handed in, because the whole question is whether the target gets resolved at all on that path.
     */
    @Test
    void should_update_the_already_promoted_api_in_place_when_upgrading_from_a_version_that_never_stored_target_api_id() {
        var promotion = aPendingPromotion().toBuilder().apiDefinition(A_V4_DEFINITION_SERVING_HTTP_PROXY).build();
        var legacyAccepted = promotion
            .toBuilder()
            .id("legacy-accepted-promotion")
            .status(PromotionStatus.ACCEPTED)
            .targetApiId(null)
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var alreadyPromotedApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId("diverged-cross-id")
            .environmentId(ENVIRONMENT_ID)
            .build();
        apiCrudServiceInMemory.initWith(List.of(alreadyPromotedApi));

        when(cockpitPromotionServiceProvider.processPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        var context = promotionContextDomainService.getPromotionContext(promotion.getId(), true);

        assertThat(context.existingPromotedApi())
            .as("the API already serving the promoted context path must be resolved as the promotion target")
            .isNotNull()
            .extracting(Api::getId)
            .isEqualTo("already-promoted-api");

        var result = useCase.execute(
            new ProcessPromotionUseCase.Input(
                context.promotion(),
                context.expectedDefinitionVersion(),
                true,
                context.existingPromotedApi(),
                ImportDefinition.builder().apiExport(ApiExport.builder().crossId(CROSS_ID).build()).build(),
                AUDIT
            )
        );

        assertThat(result.promotion().getStatus()).isEqualTo(PromotionStatus.ACCEPTED);
        assertThat(result.promotion().getTargetApiId()).isEqualTo("already-promoted-api");
        // No second API, so no entrypoint conflict — and the next promotion resolves by targetApiId.
        assertThat(apiCrudServiceInMemory.storage()).hasSize(1);
    }

    @Test
    void should_create_the_api_rather_than_take_over_an_unrelated_api_serving_the_same_path() {
        // Same shape as above but with no accepted promotion of this API into the environment: the API owning the
        // path is a stranger and must not be overwritten.
        var promotion = aPendingPromotion().toBuilder().apiDefinition(A_V4_DEFINITION_SERVING_HTTP_PROXY).build();
        promotionCrudService.initWith(List.of(promotion));
        environmentCrudService.initWith(List.of(ENVIRONMENT));
        apiCrudServiceInMemory.initWith(
            List.of(
                ApiFixtures.aProxyApiV4()
                    .toBuilder()
                    .id("unrelated-api")
                    .crossId("unrelated-cross-id")
                    .environmentId(ENVIRONMENT_ID)
                    .build()
            )
        );

        var context = promotionContextDomainService.getPromotionContext(promotion.getId(), true);

        assertThat(context.existingPromotedApi()).isNull();
    }

    /**
     * The use case mutates the promotion it receives; {@link #PROMOTION} is shared by all tests so tests asserting on
     * the "not accepted" state need their own instance with a known initial status.
     */
    private static Promotion aPendingPromotion() {
        return PROMOTION.toBuilder().status(PromotionStatus.TO_BE_VALIDATED).targetApiId(null).build();
    }

    private static final String A_V4_DEFINITION_SERVING_HTTP_PROXY = """
        {
            "api": {
                "crossId": "%s",
                "definitionVersion": "V4",
                "name": "My Api",
                "listeners": [ { "type": "HTTP", "paths": [ { "path": "/http_proxy" } ] } ]
            }
        }
        """.formatted(CROSS_ID);
}
