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
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.ApiModelFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.EventQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.domain_service.api.UpdateApiDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.v4.ApiService;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RollbackApiUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final EventCrudInMemory eventCrudService = new EventCrudInMemory();
    private final EventQueryServiceInMemory eventQueryService = new EventQueryServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final SubscriptionQueryServiceInMemory subscriptionCrudService = new SubscriptionQueryServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    CreatePlanDomainService createPlanDomainService = mock(CreatePlanDomainService.class);
    UpdatePlanDomainService updatePlanDomainService = mock(UpdatePlanDomainService.class);
    ApiService delegateApiService = mock(ApiService.class);
    UpdateApiDomainService updateApiDomainService = new UpdateApiDomainServiceImpl(delegateApiService, apiCrudService);

    RollbackApiUseCase useCase;

    Api existingApi;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var closePlanDomainService = new ClosePlanDomainService(planCrudService, subscriptionCrudService, auditDomainService);

        useCase =
            new RollbackApiUseCase(
                eventQueryService,
                apiCrudService,
                updateApiDomainService,
                planQueryService,
                createPlanDomainService,
                updatePlanDomainService,
                closePlanDomainService,
                auditDomainService
            );

        // Add existing API
        existingApi = apiV4().build();
        apiCrudService.create(ApiAdapter.INSTANCE.toCoreModel(existingApi));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(auditCrudService, eventCrudService, eventQueryService, planCrudService, planQueryService, userCrudService)
            .forEach(InMemoryAlternative::reset);
        reset(delegateApiService);
    }

    @Test
    void should_not_rollback_api_when_no_previous_event() {
        // Given
        var eventId = "event-not-found-id";

        // When
        var throwable = catchThrowable(() -> useCase.execute(new RollbackApiUseCase.Input(eventId, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Cannot rollback an event that is not a publish event!");
    }

    @Test
    void should_not_rollback_api_when_event_is_not_publish() {
        // Given
        var event = Event.builder().id("event-id").type(EventType.START_API).environments(Set.of(ENVIRONMENT_ID)).payload("{}").build();
        eventQueryService.initWith(List.of(event));

        // When
        var throwable = catchThrowable(() -> useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Cannot rollback an event that is not a publish event!");
    }

    @Test
    void should_not_rollback_api_when_event_payload_is_null() {
        // Given
        var event = Event.builder().id("event-id").type(EventType.PUBLISH_API).environments(Set.of(ENVIRONMENT_ID)).build();
        eventQueryService.initWith(List.of(event));

        // When
        var throwable = catchThrowable(() -> useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Cannot rollback an event that is not a publish event!");
    }

    @Test
    void should_not_rollback_api_when_api_definition_version_is_null() {
        // Given
        var event = Event.builder().id("event-id").type(EventType.PUBLISH_API).environments(Set.of(ENVIRONMENT_ID)).payload("{}").build();
        eventQueryService.initWith(List.of(event));

        // Whenx
        var throwable = catchThrowable(() -> useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO)));

        // Then
        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot determine API definition version from event" + event.getId());
    }

    @Test
    void should_not_rollback_api_when_api_definition_version_is_federated() {
        // Given
        var event = Event
            .builder()
            .id("event-id")
            .type(EventType.PUBLISH_API)
            .environments(Set.of(ENVIRONMENT_ID))
            .payload("{\"definitionVersion\":\"FEDERATED\"}")
            .build();
        eventQueryService.initWith(List.of(event));

        // When
        var throwable = catchThrowable(() -> useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Cannot rollback a federated API");
    }

    @Test
    void should_not_rollback_api_when_api_definition_version_is_not_v4() {
        // Given
        var event = Event
            .builder()
            .id("event-id")
            .type(EventType.PUBLISH_API)
            .environments(Set.of(ENVIRONMENT_ID))
            .payload("{\"definitionVersion\":\"2.0.0\"}")
            .build();
        eventQueryService.initWith(List.of(event));

        // When
        var throwable = catchThrowable(() -> useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Cannot rollback an API that is not a V4 API");
    }

    @Test
    void should_rollback_api() throws JsonProcessingException {
        // Given

        // Api definition contained in the Api repository
        var eventApiDefinition = io.gravitee.definition.model.v4.Api
            .builder()
            .id(existingApi.getId())
            .name("api-previous-name")
            .apiVersion("api-previous-version")
            .listeners(
                List.of(
                    io.gravitee.definition.model.v4.listener.http.HttpListener
                        .builder()
                        .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().path("/api-previous-path").build()))
                        .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                        .build()
                )
            )
            .flows(List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("api-previous-flow-name").build()))
            .build();

        // Api repository contained in the Event payload
        var apiRepositoryModel = io.gravitee.repository.management.model.Api
            .builder()
            .id(eventApiDefinition.getId())
            .name(eventApiDefinition.getName())
            .version(eventApiDefinition.getApiVersion())
            .definitionVersion(eventApiDefinition.getDefinitionVersion())
            .description("api-previous-api-description")
            .visibility(io.gravitee.repository.management.model.Visibility.PUBLIC)
            .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventApiDefinition))
            .build();

        // Event
        var event = Event
            .builder()
            .id("event-id")
            .type(EventType.PUBLISH_API)
            .environments(Set.of(ENVIRONMENT_ID))
            .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(apiRepositoryModel))
            .build();
        eventQueryService.initWith(List.of(event));

        // Simulate the update of the API
        when(
            delegateApiService.update(
                eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)),
                eq(existingApi.getId()),
                any(),
                eq(false),
                eq(USER_ID)
            )
        )
            .thenAnswer(invocation -> {
                var api = apiCrudService.get(existingApi.getId());
                api.setUpdatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()));
                apiCrudService.update(api);
                return ApiModelFixtures
                    .aModelApiV4()
                    .toBuilder()
                    .id(existingApi.getId())
                    .updatedAt(Date.from(INSTANT_NOW.atZone(ZoneId.systemDefault()).toInstant()))
                    .build();
            });

        // When
        useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

        // Then
        verify(delegateApiService)
            .update(
                eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)),
                eq(existingApi.getId()),
                argThat(updateApiEntity -> {
                    // Rollbacked with previous values
                    assertThat(updateApiEntity.getName()).isEqualTo("api-previous-name");
                    assertThat(updateApiEntity.getApiVersion()).isEqualTo("api-previous-version");
                    assertThat(updateApiEntity.getListeners().get(0).getEntrypoints().get(0))
                        .isEqualTo(Entrypoint.builder().type("http-proxy").configuration("{}").build());
                    assertThat(
                        ((io.gravitee.definition.model.v4.listener.http.HttpListener) updateApiEntity.getListeners().get(0)).getPaths()
                            .get(0)
                            .getPath()
                    )
                        .isEqualTo("/api-previous-path");
                    assertThat(updateApiEntity.getFlows().get(0).getName()).isEqualTo("api-previous-flow-name");

                    // Not rollbacked
                    assertThat(updateApiEntity.getDescription()).isEqualTo(existingApi.getDescription());
                    assertThat(updateApiEntity.getVisibility().name()).isEqualTo(existingApi.getVisibility().name());
                    return true;
                }),
                eq(false),
                eq(USER_ID)
            );

        assertRollbackAuditHasBeenCreated();
    }

    @Test
    void should_rollback_api_with_plans() throws JsonProcessingException {
        // Given

        // Existing plan
        Plan existingPlanToUpdate = givenExistingPlan(
            PlanFixtures
                .aPlanV4()
                .toBuilder()
                .id("plan-to-update")
                .apiId(existingApi.getId())
                .name("plan-to-update-name")
                .description("Description not updated")
                .commentMessage("Comment message not updated")
                .build()
        );
        Plan existingPlanToClose = givenExistingPlan(
            PlanFixtures.aPlanV4().toBuilder().id("plan-to-close").apiId(existingApi.getId()).name("plan-to-close-name").build()
        );
        Plan existingPlanToRepublish = givenExistingPlan(
            PlanFixtures
                .aPlanV4()
                .toBuilder()
                .id("plan-to-republish")
                .apiId(existingApi.getId())
                .name("plan-to-republish-name")
                .planDefinitionV4(PlanFixtures.aPlanV4().getPlanDefinitionV4().toBuilder().status(PlanStatus.CLOSED).build())
                .build()
        );

        // Api definition contained in the Api repository
        var eventApiDefinition = io.gravitee.definition.model.v4.Api
            .builder()
            .id(existingApi.getId())
            .name("api-name")
            .apiVersion("1.0.0")
            .plans(
                Map.of(
                    "plan-to-add",
                    io.gravitee.definition.model.v4.plan.Plan
                        .builder()
                        .id("plan-to-add")
                        .name("plan-to-add-name")
                        .status(PlanStatus.PUBLISHED)
                        .flows(List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("flow-name").build()))
                        .tags(Set.of("tag"))
                        .selectionRule("selection-rule")
                        .security(io.gravitee.definition.model.v4.plan.PlanSecurity.builder().type("KEY_LESS").build())
                        .build(),
                    "plan-to-update",
                    io.gravitee.definition.model.v4.plan.Plan
                        .builder()
                        .id(existingPlanToUpdate.getId())
                        .name("plan-to-update-name-UPDATED")
                        .status(PlanStatus.PUBLISHED)
                        .flows(List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("plan-to-update-new-flow").build()))
                        .build(),
                    "plan-to-republish",
                    io.gravitee.definition.model.v4.plan.Plan
                        .builder()
                        .id(existingPlanToRepublish.getId())
                        .status(PlanStatus.PUBLISHED)
                        .name("plan-to-republish-name")
                        .build()
                )
            )
            .build();

        // Api repository contained in the Event payload
        var apiRepositoryModel = io.gravitee.repository.management.model.Api
            .builder()
            .id(eventApiDefinition.getId())
            .name(eventApiDefinition.getName())
            .version(eventApiDefinition.getApiVersion())
            .definitionVersion(eventApiDefinition.getDefinitionVersion())
            .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventApiDefinition))
            .build();

        // Event
        var event = Event
            .builder()
            .id("event-id")
            .type(EventType.PUBLISH_API)
            .environments(Set.of(ENVIRONMENT_ID))
            .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(apiRepositoryModel))
            .build();
        eventQueryService.initWith(List.of(event));

        // Simulate the update of the API
        when(
            delegateApiService.update(
                eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)),
                eq(existingApi.getId()),
                any(),
                eq(false),
                eq(USER_ID)
            )
        )
            .thenAnswer(invocation -> {
                var api = apiCrudService.get(existingApi.getId());
                api.setUpdatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()));
                apiCrudService.update(api);

                return ApiModelFixtures
                    .aModelApiV4()
                    .toBuilder()
                    .id(existingApi.getId())
                    .updatedAt(Date.from(INSTANT_NOW.atZone(ZoneId.systemDefault()).toInstant()))
                    .build();
            });
        // Simulate Plan creation
        when(createPlanDomainService.create(any(Plan.class), any(), any(), any()))
            .thenAnswer(invocation -> {
                planCrudService.create(invocation.getArgument(0));
                return null;
            });

        // Simulate Plan update
        when(updatePlanDomainService.update(any(Plan.class), any(), eq(Map.of()), any(), any()))
            .thenAnswer(invocation -> {
                planCrudService.update(invocation.getArgument(0));
                return null;
            });

        // When
        useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

        // Then
        verify(delegateApiService)
            .update(
                eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)),
                eq(existingApi.getId()),
                argThat(updateApiEntity -> {
                    assertThat(updateApiEntity.getName()).isEqualTo("api-name");
                    assertThat(updateApiEntity.getApiVersion()).isEqualTo("1.0.0");
                    return true;
                }),
                eq(false),
                eq(USER_ID)
            );
        assertThat(planCrudService.storage()).hasSize(4);
        assertThat(planCrudService.storage())
            .extracting(Plan::getId, Plan::getPlanStatus)
            .containsExactlyInAnyOrder(
                tuple("plan-to-update", PlanStatus.PUBLISHED),
                tuple("plan-to-close", PlanStatus.CLOSED),
                tuple("plan-to-republish", PlanStatus.PUBLISHED),
                tuple("plan-to-add", PlanStatus.PUBLISHED)
            );

        // Check updated plan
        var updatedPlan = planCrudService.getById(existingPlanToUpdate.getId());
        assertThat(updatedPlan.getName()).isEqualTo("plan-to-update-name-UPDATED");
        assertThat(updatedPlan.getPlanDefinitionV4().getFlows().get(0).getName()).isEqualTo("plan-to-update-new-flow");
        assertThat(updatedPlan.getDescription()).isEqualTo("Description not updated");
        assertThat(updatedPlan.getCommentMessage()).isEqualTo("Comment message not updated");

        // Check created plan
        var createdPlan = planCrudService.getById("plan-to-add");
        assertThat(createdPlan.getId()).isEqualTo("plan-to-add");
        assertThat(createdPlan.getName()).isEqualTo("plan-to-add-name");
        assertThat(createdPlan.getPlanDefinitionV4().getFlows().get(0).getName()).isEqualTo("flow-name");
        assertThat(createdPlan.getPlanDefinitionV4().getTags()).containsExactly("tag");
        assertThat(createdPlan.getPlanDefinitionV4().getSelectionRule()).isEqualTo("selection-rule");
        assertThat(createdPlan.getPlanDefinitionV4().getSecurity().getType()).isEqualTo("KEY_LESS");

        assertClosePlanAuditHasBeenCreated(existingPlanToClose);
        assertRollbackAuditHasBeenCreated();
    }

    private Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        planQueryService.initWith(List.of(plan));
        return plan;
    }

    private void assertRollbackAuditHasBeenCreated() {
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                new AuditEntity(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    existingApi.getId(),
                    USER_ID,
                    Map.of(),
                    ApiAuditEvent.API_ROLLBACKED.name(),
                    INSTANT_NOW.atZone(ZoneId.systemDefault()),
                    ""
                )
            );
    }

    private void assertClosePlanAuditHasBeenCreated(Plan plan) {
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                new AuditEntity(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    plan.getApiId(),
                    USER_ID,
                    Map.of("PLAN", plan.getId()),
                    PlanAuditEvent.PLAN_CLOSED.name(),
                    INSTANT_NOW.atZone(ZoneId.systemDefault()),
                    ""
                )
            );
    }

    private Api.ApiBuilder apiV4() {
        return Api
            .builder()
            .id("my-id")
            .environmentId("env-id")
            .crossId("cross-id")
            .name("api-name")
            .description("api-description")
            .version("1.0.0")
            .origin("management")
            .mode("fully_managed")
            .definitionVersion(DefinitionVersion.V4)
            .definition(
                """
                        {"id": "my-id", "name": "api-name", "type": "proxy", "apiVersion": "1.0.0", "definitionVersion": "4.0.0", "tags": ["tag1"], "listeners": [{"type": "http", "entrypoints": [{ "type": "http-proxy", "qos": "auto", "configuration": {} }], "paths": [{ "path": "/http_proxy" }]}], "endpointGroups": [{"name": "default-group", "type": "http-proxy", "loadBalancer": { "type": "round-robin" }, "sharedConfiguration": {}, "endpoints": [{"name": "default-endpoint", "type": "http-proxy", "secondary": false, "weight": 1, "inheritConfiguration": true, "configuration": { "target": "https://api.gravitee.io/echo" }, "services": {}}], "services": {}}], "analytics": { "enabled": false }, "failover": { "enabled": true, "maxRetries": 7, "slowCallDuration": 500, "openStateDuration": 11000, "maxFailures": 3, "perSubscription": false }, "flowExecution": { "mode": "default", "matchRequired": false }, "flows": []}
                        """
            )
            .type(ApiType.PROXY)
            .createdAt(java.util.Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(java.util.Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .deployedAt(java.util.Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .visibility(io.gravitee.repository.management.model.Visibility.PUBLIC)
            .lifecycleState(LifecycleState.STARTED)
            .picture("my-picture")
            .groups(Set.of("group-1"))
            .categories(Set.of("category-1"))
            .labels(List.of("label-1"))
            .disableMembershipNotifications(true)
            .apiLifecycleState(ApiLifecycleState.PUBLISHED)
            .background("my-background");
    }
}
