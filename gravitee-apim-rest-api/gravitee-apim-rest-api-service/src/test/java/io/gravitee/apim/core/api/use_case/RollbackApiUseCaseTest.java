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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
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
import inmemory.*;
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
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.domain_service.api.ApiStateDomainServiceLegacyWrapper;
import io.gravitee.apim.infra.domain_service.api.UpdateApiDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.v4.ApiService;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RollbackApiUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "my-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final EventCrudInMemory eventCrudService = new EventCrudInMemory();
    private final EventQueryServiceInMemory eventQueryService = new EventQueryServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final SubscriptionQueryServiceInMemory subscriptionCrudService = new SubscriptionQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    private final ApiStateDomainService apiStateDomainService = mock(ApiStateDomainServiceLegacyWrapper.class);
    private final ApiMetadataQueryServiceInMemory metadataQueryService = new ApiMetadataQueryServiceInMemory();
    private final ApiCategoryQueryServiceInMemory apiCategoryQueryService = new ApiCategoryQueryServiceInMemory();
    private final IndexerInMemory indexer = new IndexerInMemory();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    @Mock
    CreatePlanDomainService createPlanDomainService;

    @Mock
    UpdatePlanDomainService updatePlanDomainService;

    ApiService delegateApiService = mock(ApiService.class);
    UpdateApiDomainService updateApiDomainService = new UpdateApiDomainServiceImpl(delegateApiService, apiCrudService);
    ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;

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
        var closeSubscriptionDomainService = mock(CloseSubscriptionDomainService.class);
        var closePlanDomainService = new ClosePlanDomainService(
            planCrudService,
            subscriptionCrudService,
            closeSubscriptionDomainService,
            auditDomainService
        );
        this.apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        var apiIndexerDomainService = new ApiIndexerDomainService(
            new ApiMetadataDecoderDomainService(metadataQueryService, new FreemarkerTemplateProcessor()),
            this.apiPrimaryOwnerDomainService,
            apiCategoryQueryService,
            indexer
        );

        useCase = new RollbackApiUseCase(
            eventQueryService,
            apiCrudService,
            updateApiDomainService,
            planQueryService,
            createPlanDomainService,
            updatePlanDomainService,
            closePlanDomainService,
            planCrudService,
            auditDomainService,
            flowCrudService,
            apiIndexerDomainService,
            this.apiPrimaryOwnerDomainService,
            apiStateDomainService
        );

        this.initializePrimaryOwnerData();

        // Add existing API
        existingApi = apiV4().build();
        apiCrudService.create(ApiAdapter.INSTANCE.toCoreModel(existingApi));
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            auditCrudService,
            eventCrudService,
            eventQueryService,
            planCrudService,
            planQueryService,
            userCrudService,
            subscriptionCrudService,
            apiCrudService,
            subscriptionQueryService,
            flowCrudService
        ).forEach(InMemoryAlternative::reset);
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
            .hasMessage("Cannot rollback an API that is not a V4 or V2 API (%s)".formatted(event.getId()));
    }

    @Test
    void should_not_rollback_api_when_api_definition_version_is_federated() {
        // Given
        var event = Event.builder()
            .id("event-id")
            .type(EventType.PUBLISH_API)
            .environments(Set.of(ENVIRONMENT_ID))
            .payload("{\"definitionVersion\":\"FEDERATED\"}")
            .build();
        eventQueryService.initWith(List.of(event));

        // When
        var throwable = catchThrowable(() -> useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO)));

        // Then
        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot rollback an API that is not a V4 or V2 API (%s)".formatted(event.getId()));
    }

    @Test
    void should_not_rollback_api_when_api_definition_version_is_not_v4() {
        // Given
        var event = Event.builder()
            .id("event-id")
            .type(EventType.PUBLISH_API)
            .environments(Set.of(ENVIRONMENT_ID))
            .payload("{\"definitionVersion\":\"1.0.0\"}")
            .build();
        eventQueryService.initWith(List.of(event));

        // When
        var throwable = catchThrowable(() -> useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO)));

        // Then
        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot rollback an API that is not a V4 or V2 API (%s)".formatted(event.getId()));
    }

    @Test
    void should_rollback_api() throws JsonProcessingException {
        // Given

        // Api definition contained in the Api repository
        var eventApiDefinition = io.gravitee.definition.model.v4.Api.builder()
            .id(existingApi.getId())
            .name("api-previous-name")
            .apiVersion("api-previous-version")
            .listeners(
                List.of(
                    io.gravitee.definition.model.v4.listener.http.HttpListener.builder()
                        .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().path("/api-previous-path").build()))
                        .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                        .build()
                )
            )
            .flows(List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("api-previous-flow-name").build()))
            .build();

        // Api repository contained in the Event payload
        var apiRepositoryModel = io.gravitee.repository.management.model.Api.builder()
            .id(eventApiDefinition.getId())
            .name(eventApiDefinition.getName())
            .version(eventApiDefinition.getApiVersion())
            .definitionVersion(eventApiDefinition.getDefinitionVersion())
            .description("api-previous-api-description")
            .visibility(io.gravitee.repository.management.model.Visibility.PUBLIC)
            .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventApiDefinition))
            .build();

        // Event
        var event = Event.builder()
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
        ).thenAnswer(invocation -> {
            var api = apiCrudService.get(existingApi.getId());
            api.setUpdatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()));
            apiCrudService.update(api);
            return ApiModelFixtures.aModelHttpApiV4()
                .toBuilder()
                .id(existingApi.getId())
                .updatedAt(Date.from(INSTANT_NOW.atZone(ZoneId.systemDefault()).toInstant()))
                .build();
        });

        // When
        useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

        // Then
        verify(delegateApiService).update(
            eq(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID)),
            eq(existingApi.getId()),
            argThat(updateApiEntity -> {
                // Rollbacked with previous values
                assertThat(updateApiEntity.getName()).isEqualTo("api-previous-name");
                assertThat(updateApiEntity.getApiVersion()).isEqualTo("api-previous-version");
                assertThat(updateApiEntity.getListeners().getFirst().getEntrypoints().getFirst()).isEqualTo(
                    Entrypoint.builder().type("http-proxy").configuration("{}").build()
                );
                assertThat(
                    ((io.gravitee.definition.model.v4.listener.http.HttpListener) updateApiEntity.getListeners().getFirst()).getPaths()
                        .getFirst()
                        .getPath()
                ).isEqualTo("/api-previous-path");
                assertThat(updateApiEntity.getFlows()).map(AbstractFlow::getName).first().isEqualTo("api-previous-flow-name");

                // Not rollbacked
                assertThat(updateApiEntity.getDescription()).isEqualTo("api-previous-api-description");
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
            PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id("plan-to-update")
                .apiId(existingApi.getId())
                .name("plan-to-update-name")
                .description("Description not updated")
                .commentMessage("Comment message not updated")
                .build()
        );
        Plan existingPlanToClose = givenExistingPlan(
            PlanFixtures.aPlanHttpV4().toBuilder().id("plan-to-close").apiId(existingApi.getId()).name("plan-to-close-name").build()
        );
        Plan existingPlanToRepublish = givenExistingPlan(
            PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id("plan-to-republish")
                .apiId(existingApi.getId())
                .name("plan-to-republish-name")
                .planDefinitionHttpV4(PlanFixtures.aPlanHttpV4().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.CLOSED).build())
                .build()
        );

        // Api definition contained in the Api repository
        var eventApiDefinition = io.gravitee.definition.model.v4.Api.builder()
            .id(existingApi.getId())
            .name("api-name")
            .apiVersion("1.0.0")
            .plans(
                Map.of(
                    "plan-to-add",
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id("plan-to-add")
                        .name("plan-to-add-name")
                        .status(PlanStatus.PUBLISHED)
                        .flows(List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("flow-name").build()))
                        .tags(Set.of("tag"))
                        .selectionRule("selection-rule")
                        .security(io.gravitee.definition.model.v4.plan.PlanSecurity.builder().type("KEY_LESS").build())
                        .build(),
                    "plan-to-update",
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(existingPlanToUpdate.getId())
                        .name("plan-to-update-name-UPDATED")
                        .status(PlanStatus.PUBLISHED)
                        .flows(List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("plan-to-update-new-flow").build()))
                        .build(),
                    "plan-to-republish",
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(existingPlanToRepublish.getId())
                        .status(PlanStatus.PUBLISHED)
                        .name("plan-to-republish-name")
                        .build()
                )
            )
            .build();

        // Api repository contained in the Event payload
        var apiRepositoryModel = io.gravitee.repository.management.model.Api.builder()
            .id(eventApiDefinition.getId())
            .name(eventApiDefinition.getName())
            .version(eventApiDefinition.getApiVersion())
            .definitionVersion(eventApiDefinition.getDefinitionVersion())
            .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventApiDefinition))
            .build();

        // Event
        var event = Event.builder()
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
        ).thenAnswer(invocation -> {
            var api = apiCrudService.get(existingApi.getId());
            api.setUpdatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()));
            apiCrudService.update(api);

            return ApiModelFixtures.aModelHttpApiV4()
                .toBuilder()
                .id(existingApi.getId())
                .updatedAt(Date.from(INSTANT_NOW.atZone(ZoneId.systemDefault()).toInstant()))
                .build();
        });
        // Simulate Plan creation
        when(createPlanDomainService.create(any(Plan.class), any(), any(), any())).thenAnswer(invocation -> {
            planCrudService.create(invocation.getArgument(0));
            return null;
        });

        // Simulate Plan update
        when(updatePlanDomainService.update(any(Plan.class), any(), eq(Map.of()), any(), any())).thenAnswer(invocation -> {
            planCrudService.update(invocation.getArgument(0));
            return null;
        });

        // When
        useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

        // Then
        verify(delegateApiService).update(
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
        assertThat(updatedPlan.getPlanDefinitionHttpV4().getFlows())
            .map(AbstractFlow::getName)
            .first()
            .isEqualTo("plan-to-update-new-flow");
        assertThat(updatedPlan.getDescription()).isEqualTo("Description not updated");
        assertThat(updatedPlan.getCommentMessage()).isEqualTo("Comment message not updated");

        // Check created plan
        var createdPlan = planCrudService.getById("plan-to-add");
        assertThat(createdPlan.getId()).isEqualTo("plan-to-add");
        assertThat(createdPlan.getName()).isEqualTo("plan-to-add-name");
        assertThat(createdPlan.getPlanDefinitionHttpV4().getFlows()).map(AbstractFlow::getName).first().isEqualTo("flow-name");
        assertThat(createdPlan.getPlanDefinitionHttpV4().getTags()).containsExactly("tag");
        assertThat(createdPlan.getPlanDefinitionHttpV4().getSelectionRule()).isEqualTo("selection-rule");
        assertThat(createdPlan.getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo("KEY_LESS");

        assertClosePlanAuditHasBeenCreated(existingPlanToClose);
        assertRollbackAuditHasBeenCreated();
    }

    @Nested
    class RollbackApiV4toV2Proxy {

        @Test
        void should_rollback_api() throws JsonProcessingException {
            // Given
            var existingV4Api = apiV4().build();
            apiCrudService.update(ApiAdapter.INSTANCE.toCoreModel(existingV4Api));

            givenExistingPlan(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id("plan-to-rollback")
                    .apiId(existingV4Api.getId())
                    .name("plan-current-name")
                    .description("Current plan description")
                    .build()
            );
            Failover failOverV2 = new Failover();
            failOverV2.setMaxAttempts(5);
            failOverV2.setRetryTimeout(1000);

            var resource = new io.gravitee.definition.model.plugins.resources.Resource();
            resource.setName("cache-resource");
            resource.setType("cache");
            resource.setConfiguration("{\"timeToLiveSeconds\": 3600}");
            resource.setEnabled(true);

            var consulDiscoveryService = new io.gravitee.definition.model.services.discovery.EndpointDiscoveryService();
            consulDiscoveryService.setEnabled(true);
            consulDiscoveryService.setProvider("consul-service-discovery");
            consulDiscoveryService.setConfiguration("{\"url\":\"http://localhost:8500\",\"service\":\"my-service\",\"dc\":\"dc1\"}");

            var services = new io.gravitee.definition.model.services.Services();
            services.setDiscoveryService(consulDiscoveryService);

            var eventV2ApiDefinition = io.gravitee.definition.model.Api.builder()
                .id(existingV4Api.getId())
                .name("api-previous-name")
                .version("api-previous-version")
                .definitionVersion(DefinitionVersion.V2)
                .proxy(
                    io.gravitee.definition.model.Proxy.builder()
                        .virtualHosts(List.of(new io.gravitee.definition.model.VirtualHost("/api-previous-path")))
                        .groups(
                            Set.of(
                                EndpointGroup.builder()
                                    .name("default-endpoint")
                                    .endpoints(Set.of(Endpoint.builder().target("https://api.gravitee.io/echo-v2").build()))
                                    .services(services)
                                    .build()
                            )
                        )
                        .logging(new Logging(LoggingMode.CLIENT, LoggingScope.REQUEST, LoggingContent.HEADERS, "someCondition"))
                        .failover(failOverV2)
                        .build()
                )
                .resources(List.of(resource))
                .plans(
                    Map.of(
                        "plan-to-rollback",
                        io.gravitee.definition.model.Plan.builder()
                            .id("plan-to-rollback")
                            .name("plan-previous-name")
                            .status("PUBLISHED")
                            .security("KEY_LESS")
                            .build()
                    )
                )
                .flowMode(FlowMode.BEST_MATCH)
                .build();

            var apiRepositoryModel = io.gravitee.repository.management.model.Api.builder()
                .id(eventV2ApiDefinition.getId())
                .name(eventV2ApiDefinition.getName())
                .version(eventV2ApiDefinition.getVersion())
                .definitionVersion(DefinitionVersion.V2)
                .visibility(io.gravitee.repository.management.model.Visibility.PUBLIC)
                .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventV2ApiDefinition))
                .build();

            var event = Event.builder()
                .id("rollback-event-id")
                .type(EventType.PUBLISH_API)
                .environments(Set.of(ENVIRONMENT_ID))
                .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(apiRepositoryModel))
                .build();
            eventQueryService.initWith(List.of(event));

            // When
            useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

            // Then
            var rolledBackApi = apiCrudService.get(existingV4Api.getId());
            assertSoftly(softly -> {
                softly.assertThat(rolledBackApi.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                softly.assertThat(rolledBackApi.getName()).isEqualTo("api-previous-name");
                softly.assertThat(rolledBackApi.getVersion()).isEqualTo("api-previous-version");
                softly.assertThat(rolledBackApi.getUpdatedAt()).isEqualTo(INSTANT_NOW.atZone(ZoneId.systemDefault()));
            });

            var apiDefinition = rolledBackApi.getApiDefinition();
            assertSoftly(softly -> {
                softly.assertThat(apiDefinition.getName()).isEqualTo("api-previous-name");
                softly.assertThat(apiDefinition.getVersion()).isEqualTo("api-previous-version");
                softly.assertThat(apiDefinition.getProxy().getVirtualHosts().getFirst().getPath()).isEqualTo("/api-previous-path");
                softly
                    .assertThat(apiDefinition.getProxy().getLogging())
                    .extracting(Logging::getMode, Logging::getContent, Logging::getScope, Logging::getCondition)
                    .contains(LoggingMode.CLIENT, LoggingContent.HEADERS, LoggingScope.REQUEST, "someCondition");
                softly.assertThat(apiDefinition.getProxy().failoverEnabled()).isTrue();
                softly.assertThat(apiDefinition.getProxy().getFailover()).isNotNull();
                softly.assertThat(apiDefinition.getProxy().getFailover().getMaxAttempts()).isEqualTo(5);
                softly.assertThat(apiDefinition.getProxy().getFailover().getRetryTimeout()).isEqualTo(1000);
                softly.assertThat(apiDefinition.getFlowMode().name()).isEqualTo(FlowMode.BEST_MATCH.name());

                var rolledBackResource = apiDefinition.getResources().get(0);
                softly.assertThat(rolledBackResource.getName()).isEqualTo("cache-resource");
                softly.assertThat(rolledBackResource.getType()).isEqualTo("cache");
                softly.assertThat(rolledBackResource.getConfiguration()).isEqualTo("{\"timeToLiveSeconds\":3600}");
                softly.assertThat(rolledBackResource.isEnabled()).isTrue();

                var rolledBackDiscoveryService = apiDefinition.getProxy().getGroups().iterator().next().getServices().getDiscoveryService();
                softly.assertThat(rolledBackDiscoveryService).isNotNull();
                softly.assertThat(rolledBackDiscoveryService.getProvider()).isEqualTo("consul-service-discovery");
                softly.assertThat(rolledBackDiscoveryService.isEnabled()).isTrue();
                softly
                    .assertThat(rolledBackDiscoveryService.getConfiguration())
                    .isEqualTo("{\"url\":\"http://localhost:8500\",\"service\":\"my-service\",\"dc\":\"dc1\"}");
            });

            var rolledBackPlan = planCrudService.getById("plan-to-rollback");
            assertSoftly(softly -> {
                softly.assertThat(rolledBackPlan.getName()).isEqualTo("plan-previous-name");
                softly.assertThat(rolledBackPlan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                softly.assertThat(rolledBackPlan.getUpdatedAt()).isEqualTo(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()));
            });

            assertThat(planCrudService.storage()).containsOnly(rolledBackPlan);

            // Verify audit log was created
            assertRollbackAuditHasBeenCreated();
        }

        @Test
        void should_rollback_api_v4_plan_should_be_reopened() throws JsonProcessingException {
            // Given
            var existingV4Api = apiV4().build();
            apiCrudService.update(ApiAdapter.INSTANCE.toCoreModel(existingV4Api));

            givenExistingPlan(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id("plan-to-rollback")
                    .apiId(existingV4Api.getId())
                    .name("plan-current-name")
                    .description("Current plan description")
                    .build()
            ).setPlanStatus(PlanStatus.CLOSED);

            var eventV2ApiDefinition = io.gravitee.definition.model.Api.builder()
                .id(existingV4Api.getId())
                .name("api-previous-name")
                .version("api-previous-version")
                .definitionVersion(DefinitionVersion.V2)
                .proxy(
                    io.gravitee.definition.model.Proxy.builder()
                        .virtualHosts(List.of(new io.gravitee.definition.model.VirtualHost("/api-previous-path")))
                        .groups(
                            Set.of(
                                EndpointGroup.builder()
                                    .name("default-endpoint")
                                    .endpoints(Set.of(Endpoint.builder().target("https://api.gravitee.io/echo-v2").build()))
                                    .build()
                            )
                        )
                        .logging(new Logging(LoggingMode.CLIENT, LoggingScope.REQUEST, LoggingContent.HEADERS, "someCondition"))
                        .build()
                )
                .plans(
                    Map.of(
                        "plan-to-rollback",
                        io.gravitee.definition.model.Plan.builder()
                            .id("plan-to-rollback")
                            .name("plan-previous-name")
                            .status("PUBLISHED")
                            .security("KEY_LESS")
                            .build()
                    )
                )
                .build();

            var apiRepositoryModel = io.gravitee.repository.management.model.Api.builder()
                .id(eventV2ApiDefinition.getId())
                .name(eventV2ApiDefinition.getName())
                .version(eventV2ApiDefinition.getVersion())
                .definitionVersion(DefinitionVersion.V2)
                .visibility(io.gravitee.repository.management.model.Visibility.PUBLIC)
                .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventV2ApiDefinition))
                .build();

            var event = Event.builder()
                .id("rollback-event-id")
                .type(EventType.PUBLISH_API)
                .environments(Set.of(ENVIRONMENT_ID))
                .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(apiRepositoryModel))
                .build();
            eventQueryService.initWith(List.of(event));

            // When
            useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

            // Then
            var rolledBackApi = apiCrudService.get(existingV4Api.getId());
            assertSoftly(softly -> {
                softly.assertThat(rolledBackApi.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                softly.assertThat(rolledBackApi.getName()).isEqualTo("api-previous-name");
                softly.assertThat(rolledBackApi.getVersion()).isEqualTo("api-previous-version");
                softly.assertThat(rolledBackApi.getUpdatedAt()).isEqualTo(INSTANT_NOW.atZone(ZoneId.systemDefault()));
            });

            var apiDefinition = rolledBackApi.getApiDefinition();
            assertSoftly(softly -> {
                softly.assertThat(apiDefinition.getName()).isEqualTo("api-previous-name");
                softly.assertThat(apiDefinition.getVersion()).isEqualTo("api-previous-version");
                softly.assertThat(apiDefinition.getProxy().getVirtualHosts().getFirst().getPath()).isEqualTo("/api-previous-path");
                softly
                    .assertThat(apiDefinition.getProxy().getLogging())
                    .extracting(Logging::getMode, Logging::getContent, Logging::getScope, Logging::getCondition)
                    .contains(LoggingMode.CLIENT, LoggingContent.HEADERS, LoggingScope.REQUEST, "someCondition");
            });

            var rolledBackPlan = planCrudService.getById("plan-to-rollback");
            assertSoftly(softly -> {
                softly.assertThat(rolledBackPlan.getName()).isEqualTo("plan-previous-name");
                softly.assertThat(rolledBackPlan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                softly.assertThat(rolledBackPlan.getUpdatedAt()).isEqualTo(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()));
                softly.assertThat(rolledBackPlan.getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
            });
            assertThat(planCrudService.storage()).containsOnly(rolledBackPlan);

            // Verify audit log was created
            assertRollbackAuditHasBeenCreated();
        }

        @Test
        void should_rollback_api_v4_plan_with_same_name_be_reopened() throws JsonProcessingException {
            // Given
            var existingV4Api = apiV4().build();
            apiCrudService.update(ApiAdapter.INSTANCE.toCoreModel(existingV4Api));

            givenExistingPlan(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id("plan-to-rollback")
                    .apiId(existingV4Api.getId())
                    .name("plan-current-name")
                    .description("Current plan description")
                    .build()
            ).setPlanStatus(PlanStatus.CLOSED);

            var eventV2ApiDefinition = io.gravitee.definition.model.Api.builder()
                .id(existingV4Api.getId())
                .name("api-previous-name")
                .version("api-previous-version")
                .definitionVersion(DefinitionVersion.V2)
                .proxy(
                    io.gravitee.definition.model.Proxy.builder()
                        .virtualHosts(List.of(new io.gravitee.definition.model.VirtualHost("/api-previous-path")))
                        .groups(
                            Set.of(
                                EndpointGroup.builder()
                                    .name("default-endpoint")
                                    .endpoints(Set.of(Endpoint.builder().target("https://api.gravitee.io/echo-v2").build()))
                                    .build()
                            )
                        )
                        .logging(new Logging(LoggingMode.CLIENT, LoggingScope.REQUEST, LoggingContent.HEADERS, "someCondition"))
                        .build()
                )
                .plans(
                    Map.of(
                        "plan-to-rollback",
                        io.gravitee.definition.model.Plan.builder()
                            .id("plan-to-rollback")
                            .name("plan-previous-name")
                            .status("PUBLISHED")
                            .security("KEY_LESS")
                            .build()
                    )
                )
                .build();

            var apiRepositoryModel = io.gravitee.repository.management.model.Api.builder()
                .id(eventV2ApiDefinition.getId())
                .name(eventV2ApiDefinition.getName())
                .version(eventV2ApiDefinition.getVersion())
                .definitionVersion(DefinitionVersion.V2)
                .visibility(io.gravitee.repository.management.model.Visibility.PUBLIC)
                .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventV2ApiDefinition))
                .build();

            var event = Event.builder()
                .id("rollback-event-id")
                .type(EventType.PUBLISH_API)
                .environments(Set.of(ENVIRONMENT_ID))
                .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(apiRepositoryModel))
                .build();
            eventQueryService.initWith(List.of(event));

            // When
            useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

            // Then
            var rolledBackApi = apiCrudService.get(existingV4Api.getId());
            assertSoftly(softly -> {
                softly.assertThat(rolledBackApi.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                softly.assertThat(rolledBackApi.getName()).isEqualTo("api-previous-name");
                softly.assertThat(rolledBackApi.getVersion()).isEqualTo("api-previous-version");
                softly.assertThat(rolledBackApi.getUpdatedAt()).isEqualTo(INSTANT_NOW.atZone(ZoneId.systemDefault()));
            });

            var apiDefinition = rolledBackApi.getApiDefinition();
            assertSoftly(softly -> {
                softly.assertThat(apiDefinition.getName()).isEqualTo("api-previous-name");
                softly.assertThat(apiDefinition.getVersion()).isEqualTo("api-previous-version");
                softly.assertThat(apiDefinition.getProxy().getVirtualHosts().getFirst().getPath()).isEqualTo("/api-previous-path");
                softly
                    .assertThat(apiDefinition.getProxy().getLogging())
                    .extracting(Logging::getMode, Logging::getContent, Logging::getScope, Logging::getCondition)
                    .contains(LoggingMode.CLIENT, LoggingContent.HEADERS, LoggingScope.REQUEST, "someCondition");
            });

            var rolledBackPlan = planCrudService.getById("plan-to-rollback");
            assertSoftly(softly -> {
                softly.assertThat(rolledBackPlan.getName()).isEqualTo("plan-previous-name");
                softly.assertThat(rolledBackPlan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                softly.assertThat(rolledBackPlan.getUpdatedAt()).isEqualTo(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()));
                softly.assertThat(rolledBackPlan.getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
            });
            assertThat(planCrudService.storage()).containsOnly(rolledBackPlan);

            // Verify audit log was created
            assertRollbackAuditHasBeenCreated();
        }

        @Test
        void should_rollback_api_and_flows() throws JsonProcessingException {
            // Given
            var existingV4Api = apiV4()
                .definition(
                    """
                    {"id": "my-id", "name": "api-name", "type": "proxy", "apiVersion": "1.0.0", "definitionVersion": "4.0.0", "listeners": [{"type": "http", "entrypoints": [{ "type": "http-proxy", "configuration": {} }], "paths": [{ "path": "/http_proxy" }]}], "endpointGroups": [{"name": "default-group", "type": "http-proxy", "endpoints": [{"name": "default-endpoint", "type": "http-proxy", "configuration": { "target": "https://api.gravitee.io/echo" }}]}], "flows": [{"name": "v4-api-flow", "enabled": true, "selectors": [ { "type": "HTTP", "path": "/", "pathOperator": "STARTS_WITH" } ], "request": [], "response": [], "subscribe": [], "publish": []}]}
                    """
                )
                .build();
            apiCrudService.update(ApiAdapter.INSTANCE.toCoreModel(existingV4Api));
            flowCrudService.saveApiFlows(
                existingV4Api.getId(),
                List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("v4-api-flow").build())
            );

            givenExistingPlan(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id("plan-to-rollback")
                    .apiId(existingV4Api.getId())
                    .name("plan-current-name")
                    .description("Current plan description")
                    .planDefinitionHttpV4(
                        PlanFixtures.aPlanHttpV4()
                            .getPlanDefinitionHttpV4()
                            .toBuilder()
                            .flows(List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("v4-plan-flow").build()))
                            .build()
                    )
                    .build()
            );
            flowCrudService.savePlanFlows(
                "plan-to-rollback",
                List.of(io.gravitee.definition.model.v4.flow.Flow.builder().name("v4-plan-flow").build())
            );

            var eventV2ApiDefinition = io.gravitee.definition.model.Api.builder()
                .id(existingV4Api.getId())
                .name("api-previous-name")
                .version("api-previous-version")
                .definitionVersion(DefinitionVersion.V2)
                .proxy(
                    Proxy.builder()
                        .virtualHosts(List.of(new VirtualHost("/api-previous-path")))
                        .groups(
                            Set.of(
                                EndpointGroup.builder()
                                    .name("default-endpoint")
                                    .endpoints(Set.of(Endpoint.builder().target("https://api.gravitee.io/echo-v2").build()))
                                    .build()
                            )
                        )
                        .logging(new Logging(LoggingMode.CLIENT, LoggingScope.REQUEST, LoggingContent.HEADERS, "someCondition"))
                        .failover(null)
                        .build()
                )
                .flows(List.of(Flow.builder().name("v2-api-flow").build()))
                .plans(
                    Map.of(
                        "plan-to-rollback",
                        io.gravitee.definition.model.Plan.builder()
                            .id("plan-to-rollback")
                            .name("plan-previous-name")
                            .status("PUBLISHED")
                            .security("KEY_LESS")
                            .flows(List.of(Flow.builder().name("v2-plan-flow").build()))
                            .build()
                    )
                )
                .build();

            var apiRepositoryModel = Api.builder()
                .id(eventV2ApiDefinition.getId())
                .name(eventV2ApiDefinition.getName())
                .version(eventV2ApiDefinition.getVersion())
                .definitionVersion(DefinitionVersion.V2)
                .visibility(Visibility.PUBLIC)
                .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventV2ApiDefinition))
                .build();

            var event = Event.builder()
                .id("rollback-event-id")
                .type(EventType.PUBLISH_API)
                .environments(Set.of(ENVIRONMENT_ID))
                .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(apiRepositoryModel))
                .build();
            eventQueryService.initWith(List.of(event));

            // When
            useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

            // Then
            var rolledBackApi = apiCrudService.get(existingV4Api.getId());
            assertSoftly(softly -> {
                softly.assertThat(rolledBackApi.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                softly.assertThat(rolledBackApi.getName()).isEqualTo("api-previous-name");
                softly.assertThat(rolledBackApi.getVersion()).isEqualTo("api-previous-version");
                softly.assertThat(rolledBackApi.getUpdatedAt()).isEqualTo(INSTANT_NOW.atZone(ZoneId.systemDefault()));
            });

            var apiDefinition = rolledBackApi.getApiDefinition();
            assertSoftly(softly -> {
                softly.assertThat(apiDefinition.getName()).isEqualTo("api-previous-name");
                softly.assertThat(apiDefinition.getVersion()).isEqualTo("api-previous-version");
                softly.assertThat(apiDefinition.getProxy().getVirtualHosts().getFirst().getPath()).isEqualTo("/api-previous-path");
                softly
                    .assertThat(apiDefinition.getProxy().getLogging())
                    .extracting(Logging::getMode, Logging::getContent, Logging::getScope, Logging::getCondition)
                    .contains(LoggingMode.CLIENT, LoggingContent.HEADERS, LoggingScope.REQUEST, "someCondition");
                softly.assertThat(apiDefinition.getProxy().failoverEnabled()).isFalse();
            });

            var rolledBackPlan = planCrudService.getById("plan-to-rollback");
            assertSoftly(softly -> {
                softly.assertThat(rolledBackPlan.getName()).isEqualTo("plan-previous-name");
                softly.assertThat(rolledBackPlan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                softly.assertThat(rolledBackPlan.getUpdatedAt()).isEqualTo(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()));
            });

            assertThat(planCrudService.storage()).containsOnly(rolledBackPlan);

            assertThat(flowCrudService.getApiV4Flows("plan-to-rollback")).isEmpty();
            assertThat(flowCrudService.getApiV2Flows(existingV4Api.getId())).map(Flow::getName).containsOnly("v2-api-flow");
            assertThat(flowCrudService.getPlanV2Flows("plan-to-rollback")).map(Flow::getName).containsOnly("v2-plan-flow");

            // Verify audit log was created
            assertRollbackAuditHasBeenCreated();
        }

        @Test
        void should_delete_v4_api_index_and_create_v2_api_index_during_rollback() throws JsonProcessingException {
            // Given
            var existingV4Api = apiV4().build();
            apiCrudService.update(ApiAdapter.INSTANCE.toCoreModel(existingV4Api));

            var v4IndexableApi = IndexableApi.builder()
                .api(ApiAdapter.INSTANCE.toCoreModel(existingV4Api))
                .primaryOwner(apiPrimaryOwnerDomainService.getApiPrimaryOwner(ORGANIZATION_ID, existingV4Api.getId()))
                .build();

            indexer.initWith(List.of(v4IndexableApi));

            var eventV2ApiDefinition = io.gravitee.definition.model.Api.builder()
                .id(existingV4Api.getId())
                .name("api-previous-name")
                .version("api-previous-version")
                .definitionVersion(DefinitionVersion.V2)
                .proxy(
                    io.gravitee.definition.model.Proxy.builder()
                        .virtualHosts(List.of(new io.gravitee.definition.model.VirtualHost("/api-previous-path")))
                        .groups(
                            Set.of(
                                EndpointGroup.builder()
                                    .name("default-endpoint")
                                    .endpoints(Set.of(Endpoint.builder().target("https://api.gravitee.io/echo-v2").build()))
                                    .build()
                            )
                        )
                        .build()
                )
                .build();

            var apiRepositoryModel = io.gravitee.repository.management.model.Api.builder()
                .id(eventV2ApiDefinition.getId())
                .name(eventV2ApiDefinition.getName())
                .version(eventV2ApiDefinition.getVersion())
                .definitionVersion(DefinitionVersion.V2)
                .visibility(io.gravitee.repository.management.model.Visibility.PUBLIC)
                .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(eventV2ApiDefinition))
                .build();

            var event = Event.builder()
                .id("rollback-event-id")
                .type(EventType.PUBLISH_API)
                .environments(Set.of(ENVIRONMENT_ID))
                .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(apiRepositoryModel))
                .build();
            eventQueryService.initWith(List.of(event));

            // When
            useCase.execute(new RollbackApiUseCase.Input(event.getId(), AUDIT_INFO));

            // Then
            assertThat(indexer.storage()).hasSize(1);

            var updatedIndexedApi = indexer
                .storage()
                .stream()
                .filter(indexable -> indexable.getId().equals(existingV4Api.getId()))
                .findFirst()
                .orElseThrow();
            var updatedApi = ((IndexableApi) updatedIndexedApi).getApi();
            assertThat(updatedApi.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
        }
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
                    plan.getReferenceId(),
                    USER_ID,
                    Map.of("PLAN", plan.getId()),
                    PlanAuditEvent.PLAN_CLOSED.name(),
                    INSTANT_NOW.atZone(ZoneId.systemDefault()),
                    ""
                )
            );
    }

    private Api.ApiBuilder apiV4() {
        return Api.builder()
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

    private void initializePrimaryOwnerData() {
        roleQueryService.initWith(
            List.of(
                Role.builder()
                    .id("role-id")
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId("role-id")
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").email("one_valid@email.com").build()));
    }
}
