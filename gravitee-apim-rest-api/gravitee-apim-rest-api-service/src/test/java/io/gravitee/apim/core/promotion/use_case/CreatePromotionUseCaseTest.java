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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.BaseUserEntityFixtures;
import fixtures.core.model.GraviteeDefinitionFixtures;
import fixtures.core.model.PageFixture;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.PromotionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PromotionCrudServiceInMemory;
import inmemory.PromotionQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cockpit.model.CockpitReplyStatus;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.json.GraviteeDefinitionSerializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.promotion.exception.PromotionAlreadyInProgressException;
import io.gravitee.apim.core.promotion.model.PromotionAuthor;
import io.gravitee.apim.core.promotion.model.PromotionRequest;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.apim.infra.domain_service.api.ApiExportDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.GraviteeDefinitionJacksonJsonSerializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePromotionUseCaseTest {

    private static final String API_ID = "api-id";
    private static final Api API = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
    private static final Plan PLAN = PlanFixtures.aPlanHttpV4().toBuilder().apiId(API_ID).build();
    private static final Page PAGE = PageFixture.aPage().toBuilder().referenceId(API_ID).referenceType(Page.ReferenceType.API).build();
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ENVIRONMENT_COCKPIT_ID = "environment-cockpit-id";
    private static final String USER_ID = "user-id";
    private static final String COCKPIT_TARGET_ENV_ID = "cockpit-target-env-id";
    private static final String COCKPIT_TARGET_ENV_NAME = "cockpit-target-env-name";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private CreatePromotionUseCase useCase;

    private ApiExportDomainService apiExportDomainService;
    private AuditDomainService auditService;
    private final CockpitPromotionServiceProvider cockpitPromotionServiceProvider = mock(CockpitPromotionServiceProvider.class);

    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    private final PromotionQueryServiceInMemory promotionQueryService = new PromotionQueryServiceInMemory();
    private final PromotionCrudServiceInMemory promotionCrudService = new PromotionCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudServiceInMemory = new PlanCrudServiceInMemory();
    private final PageCrudServiceInMemory pageCrudServiceInMemory = new PageCrudServiceInMemory();

    @BeforeEach
    public void setUp() {
        auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        apiExportDomainService = mock(ApiExportDomainServiceImpl.class);

        AtomicInteger counter = new AtomicInteger(1);
        UuidString.overrideGenerator(() -> {
            return "generated-id-" + counter.getAndIncrement();
        });

        when(apiExportDomainService.export(eq(API_ID), eq(AUDIT_INFO), anyCollection())).thenReturn(
            GraviteeDefinitionFixtures.aGraviteeDefinitionProxy()
        );
    }

    @AfterEach
    public void cleanUp() {
        Stream.of(
            auditCrudService,
            userCrudService,
            environmentCrudService,
            promotionQueryService,
            promotionCrudService,
            planCrudServiceInMemory,
            pageCrudServiceInMemory
        ).forEach(InMemoryAlternative::reset);
        UuidString.reset();
    }

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    @SneakyThrows
    void should_throw_technical_management_exception_when_api_serialization_fails() {
        initData();
        var serializer = mock(GraviteeDefinitionSerializer.class);
        when(serializer.serialize(any())).thenThrow(new JsonProcessingException("Serialization error"));

        var input = new CreatePromotionUseCase.Input(
            API_ID,
            PromotionRequest.builder().targetEnvName(COCKPIT_TARGET_ENV_NAME).targetEnvCockpitId(COCKPIT_TARGET_ENV_ID).build(),
            AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID)
        );

        useCase = createUseCase(serializer);
        Throwable throwable = catchThrowable(() -> useCase.execute(input));

        assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessage("Fail to serialize api definition");
    }

    @Test
    @SneakyThrows
    void should_create_a_promotion_and_an_audit() {
        initData();
        when(cockpitPromotionServiceProvider.requestPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        var input = new CreatePromotionUseCase.Input(
            API_ID,
            PromotionRequest.builder().targetEnvName(COCKPIT_TARGET_ENV_NAME).targetEnvCockpitId(COCKPIT_TARGET_ENV_ID).build(),
            AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID)
        );

        useCase = createUseCase(new GraviteeDefinitionJacksonJsonSerializer());
        useCase.execute(input);

        var createdPromotion = promotionCrudService
            .storage()
            .stream()
            .filter(p -> p.getApiId().equals(API_ID))
            .findFirst();

        assertThat(createdPromotion).hasValueSatisfying(promotion -> {
            assertThat(promotion.getId()).isEqualTo("generated-id-4");
            assertThat(promotion.getCreatedAt()).isEqualTo(INSTANT_NOW);
            assertThat(promotion.getApiId()).isEqualTo(API_ID);
            assertThat(promotion.getStatus()).isEqualTo(PromotionStatus.TO_BE_VALIDATED);
            assertThat(promotion.getTargetEnvCockpitId()).isEqualTo(COCKPIT_TARGET_ENV_ID);
            assertThat(promotion.getTargetEnvName()).isEqualTo(COCKPIT_TARGET_ENV_NAME);
            assertThat(promotion.getSourceEnvCockpitId()).isEqualTo(ENVIRONMENT_COCKPIT_ID);
            assertThat(promotion.getAuthor())
                .usingRecursiveComparison()
                .isEqualTo(
                    PromotionAuthor.builder()
                        .userId(USER_ID)
                        .displayName("John Smith")
                        .email("user@example.com")
                        .source("test")
                        .sourceId("test-source-id")
                        .build()
                );
        });

        assertThat(auditCrudService.storage())
            .singleElement()
            .satisfies(audit -> {
                assertThat(audit).isNotNull();
                assertThat(audit.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
                assertThat(audit.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                assertThat(audit.getEvent()).isEqualTo("PROMOTION_CREATED");
                assertThat(audit.getReferenceType()).isEqualTo(AuditEntity.AuditReferenceType.API);
                assertThat(audit.getReferenceId()).isEqualTo(API_ID);
                assertThat(audit.getProperties()).isEmpty();
                assertThat(audit.getUser()).isEqualTo(USER_ID);
            });
    }

    @Test
    @SneakyThrows
    void should_throw_an_exception_when_cockpit_command_fails() {
        initData();

        when(cockpitPromotionServiceProvider.requestPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.ERROR);

        var input = new CreatePromotionUseCase.Input(
            API_ID,
            PromotionRequest.builder().targetEnvName(COCKPIT_TARGET_ENV_NAME).targetEnvCockpitId(COCKPIT_TARGET_ENV_ID).build(),
            AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID)
        );

        useCase = createUseCase(new GraviteeDefinitionJacksonJsonSerializer());
        Throwable throwable = catchThrowable(() -> useCase.execute(input));

        assertThat(throwable)
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("An error occurs while sending promotion request to cockpit");
    }

    @Test
    @SneakyThrows
    void should_throw_an_exception_when_api_definition_version_is_not_supported() {
        auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        apiExportDomainService = mock(ApiExportDomainServiceImpl.class);
        environmentCrudService.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).cockpitId(ENVIRONMENT_COCKPIT_ID).build()));
        userCrudService.initWith(List.of(BaseUserEntityFixtures.aBaseUserEntity(USER_ID)));
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().id(API_ID).build()));

        var input = new CreatePromotionUseCase.Input(
            API_ID,
            PromotionRequest.builder().targetEnvName(COCKPIT_TARGET_ENV_NAME).targetEnvCockpitId(COCKPIT_TARGET_ENV_ID).build(),
            AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID)
        );

        useCase = createUseCase(new GraviteeDefinitionJacksonJsonSerializer());
        Throwable throwable = catchThrowable(() -> useCase.execute(input));

        assertThat(throwable).isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    @SneakyThrows
    void should_throw_an_exception_when_api_promotion_is_already_in_progress() {
        initData();

        promotionQueryService.initWith(
            List.of(
                PromotionFixtures.aPromotion()
                    .toBuilder()
                    .apiId(API_ID)
                    .status(PromotionStatus.TO_BE_VALIDATED)
                    .targetEnvCockpitId(COCKPIT_TARGET_ENV_ID)
                    .build()
            )
        );

        var input = new CreatePromotionUseCase.Input(
            API_ID,
            PromotionRequest.builder().targetEnvName(COCKPIT_TARGET_ENV_NAME).targetEnvCockpitId(COCKPIT_TARGET_ENV_ID).build(),
            AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID)
        );

        useCase = createUseCase(new GraviteeDefinitionJacksonJsonSerializer());
        Throwable throwable = catchThrowable(() -> useCase.execute(input));

        assertThat(throwable).isInstanceOf(PromotionAlreadyInProgressException.class);
    }

    @Test
    @SneakyThrows
    void should_generate_cross_ids() {
        initData();

        when(cockpitPromotionServiceProvider.requestPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        var input = new CreatePromotionUseCase.Input(
            API_ID,
            PromotionRequest.builder().targetEnvName(COCKPIT_TARGET_ENV_NAME).targetEnvCockpitId(COCKPIT_TARGET_ENV_ID).build(),
            AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID)
        );

        useCase = createUseCase(new GraviteeDefinitionJacksonJsonSerializer());
        useCase.execute(input);

        assertThat(apiCrudServiceInMemory.get(API_ID).getCrossId()).isEqualTo("generated-id-1");
        assertThat(planCrudServiceInMemory.getById(PLAN.getId()).getCrossId()).isEqualTo("generated-id-2");
        assertThat(pageCrudServiceInMemory.get(PAGE.getId()).getCrossId()).isEqualTo("generated-id-3");
    }

    @Test
    @SneakyThrows
    void should_not_override_existing_cross_ids() {
        environmentCrudService.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).cockpitId(ENVIRONMENT_COCKPIT_ID).build()));
        userCrudService.initWith(List.of(BaseUserEntityFixtures.aBaseUserEntity(USER_ID)));
        apiCrudServiceInMemory.initWith(List.of(API.toBuilder().crossId("my-api-cross-id").build()));
        planCrudServiceInMemory.initWith(List.of(PLAN.toBuilder().crossId("my-plan-cross-id").build()));
        pageCrudServiceInMemory.initWith(List.of(PAGE.toBuilder().crossId("my-page-cross-id").build()));

        when(cockpitPromotionServiceProvider.requestPromotion(any(), any(), any())).thenReturn(CockpitReplyStatus.SUCCEEDED);

        var input = new CreatePromotionUseCase.Input(
            API_ID,
            PromotionRequest.builder().targetEnvName(COCKPIT_TARGET_ENV_NAME).targetEnvCockpitId(COCKPIT_TARGET_ENV_ID).build(),
            AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID)
        );

        useCase = createUseCase(new GraviteeDefinitionJacksonJsonSerializer());
        useCase.execute(input);

        assertThat(apiCrudServiceInMemory.get(API_ID).getCrossId()).isEqualTo("my-api-cross-id");
        assertThat(planCrudServiceInMemory.getById(PLAN.getId()).getCrossId()).isEqualTo("my-plan-cross-id");
        assertThat(pageCrudServiceInMemory.get(PAGE.getId()).getCrossId()).isEqualTo("my-page-cross-id");
    }

    private CreatePromotionUseCase createUseCase(GraviteeDefinitionSerializer serializer) {
        return new CreatePromotionUseCase(
            apiExportDomainService,
            environmentCrudService,
            promotionQueryService,
            serializer,
            promotionCrudService,
            auditService,
            cockpitPromotionServiceProvider,
            userCrudService,
            apiCrudServiceInMemory,
            planCrudServiceInMemory,
            pageCrudServiceInMemory
        );
    }

    private void initData() {
        environmentCrudService.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).cockpitId(ENVIRONMENT_COCKPIT_ID).build()));
        userCrudService.initWith(List.of(BaseUserEntityFixtures.aBaseUserEntity(USER_ID)));
        apiCrudServiceInMemory.initWith(List.of(API.toBuilder().crossId(null).build()));
        planCrudServiceInMemory.initWith(List.of(PLAN.toBuilder().crossId(null).build()));
        pageCrudServiceInMemory.initWith(List.of(PAGE.toBuilder().crossId(null).build()));
    }
}
