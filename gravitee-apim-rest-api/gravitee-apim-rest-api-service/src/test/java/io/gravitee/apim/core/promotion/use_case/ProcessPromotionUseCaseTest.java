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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PromotionFixtures;
import initializers.ImportDefinitionCreateDomainServiceTestInitializer;
import inmemory.ApiCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PromotionCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionUpdateDomainServiceTestInitializer;
import io.gravitee.apim.core.api.exception.ApiImportedWithErrorException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cockpit.model.CockpitReplyStatus;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
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
    private CockpitPromotionServiceProvider cockpitPromotionServiceProvider;
    private ProcessPromotionUseCase useCase;

    @BeforeEach
    public void setUp() {
        cockpitPromotionServiceProvider = mock(CockpitPromotionServiceProvider.class);

        useCase = new ProcessPromotionUseCase(
            promotionCrudService,
            cockpitPromotionServiceProvider,
            importDefinitionCreateDomainService.initialize(),
            importDefinitionUpdateDomainService.initialize(ENVIRONMENT_ID)
        );

        UuidString.overrideGenerator(() -> UUID);
    }

    @AfterEach
    public void cleanUp() {
        Stream.of(promotionCrudService, apiCrudServiceInMemory).forEach(InMemoryAlternative::reset);
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

        when(cockpitPromotionServiceProvider.requestPromotion(eq(ORGANIZATION_ID), eq(ENVIRONMENT_ID), any())).thenReturn(
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
    }

    @Test
    void should_throw_exception_when_v4_api_promotion_command_fails() {
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        when(cockpitPromotionServiceProvider.requestPromotion(eq(ORGANIZATION_ID), eq(ENVIRONMENT_ID), any())).thenReturn(
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

        when(cockpitPromotionServiceProvider.requestPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

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
    }

    @Test
    void should_promote_proxy_v4_api() {
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        var v4proxyApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).crossId(CROSS_ID).build();
        apiCrudServiceInMemory.initWith(List.of(v4proxyApi));

        when(cockpitPromotionServiceProvider.requestPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

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

        when(cockpitPromotionServiceProvider.requestPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

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
}
