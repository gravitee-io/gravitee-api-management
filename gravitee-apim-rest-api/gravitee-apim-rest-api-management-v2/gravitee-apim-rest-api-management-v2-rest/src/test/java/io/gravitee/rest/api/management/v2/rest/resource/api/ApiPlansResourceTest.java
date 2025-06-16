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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.core.model.ApiFixtures.aNativeApi;
import static fixtures.core.model.ApiFixtures.aProxyApiV4;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.management.v2.rest.mapper.PlanMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreatePlanV2;
import io.gravitee.rest.api.management.v2.rest.model.CreatePlanV4;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.Plan;
import io.gravitee.rest.api.management.v2.rest.model.PlanMode;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurity;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.PlanV2;
import io.gravitee.rest.api.management.v2.rest.model.PlanV4;
import io.gravitee.rest.api.management.v2.rest.model.PlanValidation;
import io.gravitee.rest.api.management.v2.rest.model.PlansResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanV4;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.PlanType;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiPlansResourceTest extends AbstractResourceTest {

    protected static final String API = "my-api";
    protected static final String APPLICATION = "my-app";
    protected static final String PLAN = "my-plan";
    protected static final String ENVIRONMENT = "my-env";

    @Autowired
    private PlanSearchService planSearchService;

    @Autowired
    private ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Autowired
    private CreatePlanDomainService createPlanDomainService;

    @Autowired
    private SubscriptionQueryServiceInMemory subscriptionQueryService;

    @Autowired
    private UpdatePlanDomainService updatePlanDomainService;

    @Autowired
    private PlanCrudServiceInMemory planCrudServiceInMemory;

    WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans";
    }

    @BeforeEach
    public void init() throws TechnicalException {
        target = rootTarget();

        Api api = Api.builder().id(API).environmentId(ENVIRONMENT).build();
        when(apiRepository.findById(API)).thenReturn(Optional.of(api));

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Mockito.reset(planServiceV4, planServiceV2, apiSearchServiceV4, planSearchService, updatePlanDomainService);
    }

    @Nested
    class ListPlans {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PLAN),
                    eq(API),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_empty_page_if_no_plans() {
            var planQuery = PlanQuery.builder().apiId(API).status(List.of(PlanStatus.PUBLISHED)).build();
            when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
                .thenReturn(new ArrayList<>());

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlansResponse.class)
                .isEqualTo(
                    new PlansResponse().pagination(new Pagination()).data(List.of()).links(new Links().self(target.getUri().toString()))
                );
        }

        @Test
        public void should_return_list_if_no_params_specified_v4() {
            PlanEntity plan1 = PlanFixtures.aPlanEntityV4().toBuilder().id("plan-1").order(3).build();
            PlanEntity plan3 = PlanFixtures.aPlanEntityV4().toBuilder().id("plan-3").order(1).build();

            var planQuery = PlanQuery.builder().apiId(API).securityType(new ArrayList<>()).status(List.of(PlanStatus.PUBLISHED)).build();
            when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
                .thenReturn(List.of(plan1, plan3));

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlansResponse.class)
                .isEqualTo(
                    new PlansResponse()
                        .pagination(new Pagination().page(1).perPage(10).pageItemsCount(2).totalCount(2L).pageCount(1))
                        .data(
                            Stream
                                .of(plan3, plan1)
                                .map(PlanMapper.INSTANCE::map)
                                .map(p -> {
                                    var plan = new Plan();
                                    plan.setActualInstance(p);
                                    return plan;
                                })
                                .toList()
                        )
                        .links(new Links().self(target.getUri().toString()))
                );
        }

        @Test
        public void should_return_list_if_no_params_specified_native_v4() {
            var plan1 = PlanFixtures.aNativePlanEntityV4().toBuilder().id("plan-1").order(3).build();
            var plan2 = PlanFixtures.aNativePlanEntityV4().toBuilder().id("plan-3").order(1).build();

            var planQuery = PlanQuery.builder().apiId(API).securityType(new ArrayList<>()).status(List.of(PlanStatus.PUBLISHED)).build();
            when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
                .thenReturn(List.of(plan1, plan2));

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlansResponse.class)
                .isEqualTo(
                    new PlansResponse()
                        .pagination(new Pagination().page(1).perPage(10).pageItemsCount(2).totalCount(2L).pageCount(1))
                        .data(
                            Stream
                                .of(plan2, plan1)
                                .map(PlanMapper.INSTANCE::map)
                                .map(p -> {
                                    var plan = new Plan();
                                    plan.setActualInstance(p);
                                    return plan;
                                })
                                .toList()
                        )
                        .links(new Links().self(target.getUri().toString()))
                );
        }

        @Test
        public void should_return_list_with_params_specified_v2() {
            io.gravitee.rest.api.model.PlanEntity plan1 = PlanFixtures
                .aPlanEntityV2()
                .toBuilder()
                .id("plan-1")
                .order(1)
                .status(io.gravitee.rest.api.model.PlanStatus.DEPRECATED)
                .paths(
                    Map.of(
                        "path",
                        List.of(Rule.builder().methods(Set.of(HttpMethod.GET)).description("description of a rule").enabled(true).build())
                    )
                )
                .build();
            io.gravitee.rest.api.model.PlanEntity plan3 = PlanFixtures
                .aPlanEntityV2()
                .toBuilder()
                .id("plan-3")
                .order(2)
                .status(io.gravitee.rest.api.model.PlanStatus.DEPRECATED)
                .build();

            var planQuery = PlanQuery
                .builder()
                .apiId(API)
                .securityType(List.of(io.gravitee.rest.api.model.v4.plan.PlanSecurityType.JWT))
                .status(List.of(PlanStatus.DEPRECATED))
                .mode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD)
                .build();
            when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
                .thenReturn(List.of(plan1, plan3));

            target =
                target
                    .queryParam("securities", "JWT")
                    .queryParam("statuses", "DEPRECATED")
                    .queryParam("perPage", 1)
                    .queryParam("mode", "STANDARD");
            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlansResponse.class)
                .isEqualTo(
                    new PlansResponse()
                        .pagination(new Pagination().page(1).perPage(1).pageItemsCount(1).totalCount(2L).pageCount(2))
                        .data(
                            Stream
                                .of(plan1)
                                .map(PlanMapper.INSTANCE::map)
                                .map(p -> {
                                    var plan = new Plan();
                                    plan.setActualInstance(p);
                                    return plan;
                                })
                                .toList()
                        )
                        .links(
                            new Links()
                                .self(target.getUri().toString())
                                .first(target.queryParam("page", 1).getUri().toString())
                                .last(target.queryParam("page", 2).getUri().toString())
                                .next(target.queryParam("page", 2).getUri().toString())
                        )
                );
        }

        @Test
        public void should_return_subscribable_plans() {
            var plan1 = PlanFixtures.aPlanEntityV4().toBuilder().id("plan-1").apiId(API).build();
            var plan2 = PlanFixtures.aPlanEntityV4().toBuilder().id("plan-2").apiId(API).build();
            var plan3 = PlanFixtures
                .aPlanEntityV4()
                .toBuilder()
                .id("plan-3")
                .apiId(API)
                .mode(io.gravitee.definition.model.v4.plan.PlanMode.PUSH)
                .security(null)
                .build();
            var planQuery = PlanQuery.builder().apiId(API).securityType(new ArrayList<>()).status(List.of(PlanStatus.PUBLISHED)).build();
            when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
                .thenReturn(List.of(plan1, plan2, plan3));

            var plan1Subscription = SubscriptionEntity
                .builder()
                .apiId(API)
                .planId(plan2.getId())
                .status(SubscriptionEntity.Status.CLOSED)
                .applicationId(APPLICATION)
                .build();
            var plan2Subscription = SubscriptionEntity
                .builder()
                .apiId(API)
                .planId(plan2.getId())
                .status(SubscriptionEntity.Status.ACCEPTED)
                .applicationId(APPLICATION)
                .build();

            subscriptionQueryService.initWith(List.of(plan1Subscription, plan2Subscription));

            target = target.queryParam("subscribableBy", APPLICATION);
            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlansResponse.class)
                .isEqualTo(
                    new PlansResponse()
                        .pagination(new Pagination().page(1).perPage(10).pageItemsCount(2).totalCount(2L).pageCount(1))
                        .data(
                            Stream
                                .of(plan1, plan3)
                                .map(PlanMapper.INSTANCE::map)
                                .map(p -> {
                                    var plan = new Plan();
                                    plan.setActualInstance(p);
                                    return plan;
                                })
                                .toList()
                        )
                        .links(new Links().self(target.getUri().toString()))
                );
        }

        @Test
        public void should_not_find_any_subscribable_plan() {
            var plan1 = PlanFixtures
                .aPlanEntityV4()
                .toBuilder()
                .id("plan-1")
                .apiId(API)
                .security(
                    io.gravitee.definition.model.v4.plan.PlanSecurity
                        .builder()
                        .type(io.gravitee.rest.api.model.v4.plan.PlanSecurityType.KEY_LESS.getLabel())
                        .build()
                )
                .build();
            var plan2 = PlanFixtures.aPlanEntityV4().toBuilder().id("plan-2").apiId(API).build();
            var planQuery = PlanQuery.builder().apiId(API).securityType(new ArrayList<>()).status(List.of(PlanStatus.PUBLISHED)).build();
            when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
                .thenReturn(List.of(plan1, plan2));

            var subscription = SubscriptionEntity
                .builder()
                .apiId(API)
                .planId(plan2.getId())
                .status(SubscriptionEntity.Status.ACCEPTED)
                .applicationId(APPLICATION)
                .build();
            subscriptionQueryService.initWith(List.of(subscription));

            target = target.queryParam("subscribableBy", APPLICATION);
            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlansResponse.class)
                .extracting(PlansResponse::getData)
                .isEqualTo(Collections.emptyList());
        }

        @Test
        public void should_return_subscribable_plans_without_keyless() {
            var plan1 = PlanFixtures
                .aPlanEntityV4()
                .toBuilder()
                .id("plan-1")
                .apiId(API)
                .security(
                    io.gravitee.definition.model.v4.plan.PlanSecurity
                        .builder()
                        .type(io.gravitee.rest.api.model.v4.plan.PlanSecurityType.KEY_LESS.getLabel())
                        .build()
                )
                .build();
            var plan2 = PlanFixtures
                .aPlanEntityV2()
                .toBuilder()
                .id("plan-2")
                .api(API)
                .security(io.gravitee.rest.api.model.PlanSecurityType.KEY_LESS)
                .build();
            var plan3 = PlanFixtures.aPlanEntityV4().toBuilder().id("plan-3").apiId(API).build();
            var plan4 = PlanFixtures.aPlanEntityV4().toBuilder().id("plan-4").apiId(API).build();
            var plan5 = PlanFixtures.aPlanEntityV4().toBuilder().id("plan-5").apiId(API).build();
            var planQuery = PlanQuery.builder().apiId(API).securityType(new ArrayList<>()).status(List.of(PlanStatus.PUBLISHED)).build();
            when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
                .thenReturn(List.of(plan1, plan2, plan3, plan4, plan5));

            subscriptionQueryService.initWith(
                List.of(
                    SubscriptionEntity
                        .builder()
                        .apiId(API)
                        .planId(plan5.getId())
                        .status(SubscriptionEntity.Status.ACCEPTED)
                        .applicationId(APPLICATION)
                        .build(),
                    SubscriptionEntity
                        .builder()
                        .apiId(API)
                        .planId(plan4.getId())
                        .status(SubscriptionEntity.Status.CLOSED)
                        .applicationId(APPLICATION)
                        .build()
                )
            );

            target = target.queryParam("subscribableBy", APPLICATION);
            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlansResponse.class)
                .isEqualTo(
                    new PlansResponse()
                        .pagination(new Pagination().page(1).perPage(10).pageItemsCount(2).totalCount(2L).pageCount(1))
                        .data(
                            Stream
                                .of(plan3, plan4)
                                .map(PlanMapper.INSTANCE::map)
                                .map(p -> {
                                    var plan = new Plan();
                                    plan.setActualInstance(p);
                                    return plan;
                                })
                                .toList()
                        )
                        .links(new Links().self(target.getUri().toString()))
                );
        }
    }

    @Nested
    class Create {

        @BeforeEach
        void setUp() {
            var proxyApi = aProxyApiV4().toBuilder().id(API).build();
            apiCrudServiceInMemory.initWith(List.of(proxyApi));
        }

        @AfterEach
        void tearDown() {
            apiCrudServiceInMemory.reset();
            reset(createPlanDomainService);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PLAN),
                    eq(API),
                    eq(RolePermissionAction.CREATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(PlanFixtures.aCreatePlanHttpV4()));
            assertThat(response).hasStatus(FORBIDDEN_403).asError().hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_create_v4_plan() {
            var planId = "new-id";
            when(createPlanDomainService.create(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    io.gravitee.apim.core.plan.model.Plan plan = invocation.getArgument(0);
                    return new PlanWithFlows(plan.setPlanId(planId), invocation.getArgument(1));
                });

            final CreatePlanV4 createPlanV4 = PlanFixtures.aCreatePlanHttpV4();
            final Response response = target.request().post(Entity.json(createPlanV4));

            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(PlanV4.class)
                .isEqualTo(
                    new PlanV4()
                        .flows(createPlanV4.getFlows())
                        .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4)
                        .id(planId)
                        .crossId(createPlanV4.getCrossId())
                        .apiId(API)
                        .name(createPlanV4.getName())
                        .description(createPlanV4.getDescription())
                        .order(1)
                        .commentRequired(createPlanV4.getCommentRequired())
                        .commentMessage(createPlanV4.getCommentMessage())
                        .generalConditions(createPlanV4.getGeneralConditions())
                        .validation(createPlanV4.getValidation())
                        .mode(PlanMode.STANDARD)
                        .status(io.gravitee.rest.api.management.v2.rest.model.PlanStatus.STAGING)
                        .security(new PlanSecurity().type(PlanSecurityType.API_KEY).configuration(Map.of("nice", "config")))
                        .selectionRule(createPlanV4.getSelectionRule())
                        .characteristics(createPlanV4.getCharacteristics())
                        .excludedGroups(createPlanV4.getExcludedGroups())
                        .tags(List.of("tag1", "tag2"))
                        .type(io.gravitee.rest.api.management.v2.rest.model.PlanType.API)
                );
        }

        @Test
        public void should_create_native_v4_plan() {
            var planId = "new-id";
            when(createPlanDomainService.create(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    io.gravitee.apim.core.plan.model.Plan plan = invocation.getArgument(0);
                    return new PlanWithFlows(plan.setPlanId(planId), invocation.getArgument(1));
                });

            apiCrudServiceInMemory.initWith(List.of(aNativeApi().toBuilder().id(API).build()));

            final CreatePlanV4 createPlanV4 = PlanFixtures.aCreatePlanNativeV4();
            final Response response = target.request().post(Entity.json(createPlanV4));

            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(PlanV4.class)
                .isEqualTo(
                    new PlanV4()
                        .flows(createPlanV4.getFlows())
                        .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4)
                        .id(planId)
                        .crossId(createPlanV4.getCrossId())
                        .apiId(API)
                        .name(createPlanV4.getName())
                        .description(createPlanV4.getDescription())
                        .order(1)
                        .commentRequired(createPlanV4.getCommentRequired())
                        .commentMessage(createPlanV4.getCommentMessage())
                        .generalConditions(createPlanV4.getGeneralConditions())
                        .validation(createPlanV4.getValidation())
                        .mode(PlanMode.STANDARD)
                        .status(io.gravitee.rest.api.management.v2.rest.model.PlanStatus.STAGING)
                        .security(new PlanSecurity().type(PlanSecurityType.API_KEY).configuration(Map.of("nice", "config")))
                        .selectionRule(createPlanV4.getSelectionRule())
                        .characteristics(createPlanV4.getCharacteristics())
                        .excludedGroups(createPlanV4.getExcludedGroups())
                        .tags(List.of("tag1", "tag2"))
                        .type(io.gravitee.rest.api.management.v2.rest.model.PlanType.API)
                );
        }

        @Test
        public void should_create_v4_plan_accepting_null_as_flow_list() {
            var planId = "new-id";
            when(createPlanDomainService.create(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    io.gravitee.apim.core.plan.model.Plan plan = invocation.getArgument(0);
                    return new PlanWithFlows(plan.setPlanId(planId), invocation.getArgument(1));
                });

            final CreatePlanV4 createPlanV4 = PlanFixtures.aCreatePlanHttpV4().flows(null);
            final Response response = target.request().post(Entity.json(createPlanV4));

            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(PlanV4.class)
                .isEqualTo(
                    new PlanV4()
                        .flows(Collections.emptyList())
                        .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4)
                        .id(planId)
                        .crossId(createPlanV4.getCrossId())
                        .apiId(API)
                        .name(createPlanV4.getName())
                        .description(createPlanV4.getDescription())
                        .order(1)
                        .commentRequired(createPlanV4.getCommentRequired())
                        .commentMessage(createPlanV4.getCommentMessage())
                        .generalConditions(createPlanV4.getGeneralConditions())
                        .validation(createPlanV4.getValidation())
                        .mode(PlanMode.STANDARD)
                        .status(io.gravitee.rest.api.management.v2.rest.model.PlanStatus.STAGING)
                        .security(new PlanSecurity().type(PlanSecurityType.API_KEY).configuration(Map.of("nice", "config")))
                        .selectionRule(createPlanV4.getSelectionRule())
                        .characteristics(createPlanV4.getCharacteristics())
                        .excludedGroups(createPlanV4.getExcludedGroups())
                        .tags(List.of("tag1", "tag2"))
                        .type(io.gravitee.rest.api.management.v2.rest.model.PlanType.API)
                );
        }

        @Test
        public void should_create_v2_plan() {
            when(planServiceV2.create(eq(GraviteeContext.getExecutionContext()), any(io.gravitee.rest.api.model.NewPlanEntity.class)))
                .thenAnswer(invocation -> {
                    io.gravitee.rest.api.model.NewPlanEntity newPlan = invocation.getArgument(1);
                    return io.gravitee.rest.api.model.PlanEntity
                        .builder()
                        .id("new-id")
                        .api(newPlan.getApi())
                        .name(newPlan.getName())
                        .type(PlanType.API)
                        .security(newPlan.getSecurity())
                        .flows(newPlan.getFlows())
                        .build();
                });

            final CreatePlanV2 createPlanV2 = PlanFixtures.aCreatePlanV2();
            final Response response = target.request().post(Entity.json(createPlanV2));

            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(PlanV2.class)
                .isEqualTo(
                    new PlanV2()
                        .flows(createPlanV2.getFlows())
                        .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V2)
                        .id("new-id")
                        .apiId(API)
                        .name(createPlanV2.getName())
                        .type(io.gravitee.rest.api.management.v2.rest.model.PlanType.API)
                        .security(new PlanSecurity().type(PlanSecurityType.API_KEY))
                        .order(0)
                        .commentRequired(false)
                );
        }
    }

    @Nested
    class Get {

        @BeforeEach
        void setUp() {
            target = rootTarget(PLAN);
        }

        @Test
        public void should_return_404_if_not_found() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

            final Response response = target.request().get();
            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
        }

        @Test
        public void should_return_404_if_plan_associated_to_another_api() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build());

            final Response response = target.request().get();
            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PLAN),
                    eq(API),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            final Response response = target.request().get();
            assertThat(response).hasStatus(FORBIDDEN_403).asError().hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_v4_plan() {
            final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

            final Response response = target.request().get();

            assertThat(response).hasStatus(OK_200).asEntity(PlanV4.class).isEqualTo(PlanMapper.INSTANCE.map(planEntity));
        }

        @Test
        public void should_return_native_v4_plan() {
            var planEntity = PlanFixtures.aNativePlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

            final Response response = target.request().get();

            assertThat(response).hasStatus(OK_200).asEntity(PlanV4.class).isEqualTo(PlanMapper.INSTANCE.map(planEntity));
        }

        @Test
        public void should_return_v2_plan() {
            final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

            final Response response = target.request().get();

            assertThat(response).hasStatus(OK_200).asEntity(PlanV2.class).isEqualTo(PlanMapper.INSTANCE.map(planEntity));
        }
    }

    @Nested
    class Update {

        @BeforeEach
        void setUp() {
            target = rootTarget(PLAN);
        }

        @AfterEach
        void tearDown() {
            planCrudServiceInMemory.reset();
            apiCrudServiceInMemory.reset();
        }

        @Test
        public void should_return_404_if_not_found() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

            final Response response = target.request().put(Entity.json(PlanFixtures.anUpdatePlanV4()));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
        }

        @Test
        public void should_return_404_if_plan_associated_to_another_api() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build());

            final Response response = target.request().put(Entity.json(PlanFixtures.anUpdatePlanV4()));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PLAN),
                    eq(API),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().put(Entity.json(PlanFixtures.aPlanEntityV4()));

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_update_v4_plan() {
            planCrudServiceInMemory.initWith(
                List.of(
                    fixtures.core.model.PlanFixtures
                        .aPlanHttpV4()
                        .toBuilder()
                        .apiId(API)
                        .id(PLAN)
                        .validation(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.MANUAL)
                        .build()
                )
            );
            apiCrudServiceInMemory.initWith(List.of(fixtures.core.model.ApiFixtures.aProxyApiV4().toBuilder().id(API).build()));

            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build());
            when(groupService.findAllByEnvironment(anyString())).thenReturn(mockExcludedGroups());
            when(updatePlanDomainService.update(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    io.gravitee.apim.core.plan.model.Plan updated = invocation.getArgument(0);
                    updated.setUpdatedAt(null);
                    updated.setCreatedAt(null);
                    return updated;
                });

            final UpdatePlanV4 updatePlanV4 = PlanFixtures.anUpdatePlanV4();
            final Response response = target.request().put(Entity.json(updatePlanV4));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV4.class)
                .isEqualTo(
                    new PlanV4()
                        .flows(updatePlanV4.getFlows())
                        .id(PLAN)
                        .apiId(API)
                        .status(io.gravitee.rest.api.management.v2.rest.model.PlanStatus.PUBLISHED)
                        .type(io.gravitee.rest.api.management.v2.rest.model.PlanType.API)
                        .generalConditions("General conditions")
                        .mode(PlanMode.STANDARD)
                        .definitionVersion(DefinitionVersion.V4)
                        .name(updatePlanV4.getName())
                        .crossId(updatePlanV4.getCrossId())
                        .name(updatePlanV4.getName())
                        .description(updatePlanV4.getDescription())
                        .validation(updatePlanV4.getValidation())
                        .characteristics(updatePlanV4.getCharacteristics())
                        .order(updatePlanV4.getOrder())
                        .excludedGroups(updatePlanV4.getExcludedGroups())
                        .security(new PlanSecurity().type(PlanSecurityType.KEY_LESS).configuration(Map.of("nice", "config")))
                        .commentRequired(false)
                        .commentMessage(updatePlanV4.getCommentMessage())
                        .tags(updatePlanV4.getTags())
                        .selectionRule(updatePlanV4.getSelectionRule())
                );
        }

        @Test
        public void should_update_native_v4_plan() {
            planCrudServiceInMemory.initWith(
                List.of(fixtures.core.model.PlanFixtures.aPlanNativeV4().toBuilder().apiId(API).id(PLAN).build())
            );
            apiCrudServiceInMemory.initWith(List.of(fixtures.core.model.ApiFixtures.aNativeApi().toBuilder().id(API).build()));
            when(groupService.findAllByEnvironment(anyString())).thenReturn(mockExcludedGroups());
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aNativePlanEntityV4().toBuilder().id(PLAN).apiId(API).build());
            when(updatePlanDomainService.update(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    io.gravitee.apim.core.plan.model.Plan updated = invocation.getArgument(0);
                    updated.setUpdatedAt(null);
                    updated.setCreatedAt(null);
                    return updated;
                });

            final UpdatePlanV4 updatePlanV4 = PlanFixtures.anUpdatePlanNativeV4();
            final Response response = target.request().put(Entity.json(updatePlanV4));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV4.class)
                .isEqualTo(
                    new PlanV4()
                        .flows(updatePlanV4.getFlows())
                        .id(PLAN)
                        .apiId(API)
                        .status(io.gravitee.rest.api.management.v2.rest.model.PlanStatus.PUBLISHED)
                        .type(io.gravitee.rest.api.management.v2.rest.model.PlanType.API)
                        .generalConditions("General conditions")
                        .mode(PlanMode.STANDARD)
                        .definitionVersion(DefinitionVersion.V4)
                        .name(updatePlanV4.getName())
                        .crossId(updatePlanV4.getCrossId())
                        .name(updatePlanV4.getName())
                        .description(updatePlanV4.getDescription())
                        .validation(updatePlanV4.getValidation())
                        .characteristics(updatePlanV4.getCharacteristics())
                        .order(updatePlanV4.getOrder())
                        .excludedGroups(updatePlanV4.getExcludedGroups())
                        .security(new PlanSecurity().type(PlanSecurityType.KEY_LESS).configuration(Map.of("nice", "config")))
                        .commentRequired(false)
                        .commentMessage(updatePlanV4.getCommentMessage())
                        .tags(updatePlanV4.getTags())
                        .selectionRule(updatePlanV4.getSelectionRule())
                );
        }

        @Test
        public void should_return_bad_request_when_setting_definition_to_v2_on_v4_plan() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build());

            final UpdatePlanV4 updatePlanV4 = PlanFixtures.anUpdatePlanV4();
            final Response response = target.request().put(Entity.json(updatePlanV4));

            assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Plan [" + PLAN + "] is not valid.");
        }

        @Test
        public void should_update_v2_plan() {
            final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

            when(planServiceV2.update(eq(GraviteeContext.getExecutionContext()), any(io.gravitee.rest.api.model.UpdatePlanEntity.class)))
                .thenAnswer(invocation -> {
                    final io.gravitee.rest.api.model.UpdatePlanEntity updated = invocation.getArgument(1);
                    return io.gravitee.rest.api.model.PlanEntity
                        .builder()
                        .id(updated.getId())
                        .crossId(updated.getCrossId())
                        .name(updated.getName())
                        .description(updated.getDescription())
                        .validation(updated.getValidation())
                        .characteristics(updated.getCharacteristics())
                        .order(updated.getOrder())
                        .excludedGroups(updated.getExcludedGroups())
                        .commentRequired(updated.isCommentRequired())
                        .commentMessage(updated.getCommentMessage())
                        .tags(updated.getTags())
                        .selectionRule(updated.getSelectionRule())
                        .flows(updated.getFlows())
                        .build();
                });
            final UpdatePlanV2 updatePlanV2 = PlanFixtures.anUpdatePlanV2();
            final Response response = target.request().put(Entity.json(updatePlanV2));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV2.class)
                .isEqualTo(
                    new PlanV2()
                        .flows(updatePlanV2.getFlows())
                        .id(PLAN)
                        .definitionVersion(DefinitionVersion.V2)
                        .name(updatePlanV2.getName())
                        .crossId(updatePlanV2.getCrossId())
                        .name(updatePlanV2.getName())
                        .description(updatePlanV2.getDescription())
                        .validation(updatePlanV2.getValidation())
                        .characteristics(updatePlanV2.getCharacteristics())
                        .order(updatePlanV2.getOrder())
                        .excludedGroups(updatePlanV2.getExcludedGroups())
                        .security(new PlanSecurity())
                        .commentRequired(false)
                        .commentMessage(updatePlanV2.getCommentMessage())
                        .tags(updatePlanV2.getTags())
                        .selectionRule(updatePlanV2.getSelectionRule())
                );
        }

        @Test
        public void should_return_bad_request_when_setting_definition_to_v4_on_v2_plan() {
            final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            final UpdatePlanV2 updatePlanV2 = PlanFixtures.anUpdatePlanV2();

            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

            final Response response = target.request().put(Entity.json(updatePlanV2));

            assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Plan [" + PLAN + "] is not valid.");
        }

        @Test
        public void should_return_group_not_found_exception_when_excluded_group_not_found() {
            String nonExistentGroupId = "unknown-group-id";
            planCrudServiceInMemory.initWith(
                List.of(
                    fixtures.core.model.PlanFixtures
                        .aPlanHttpV4()
                        .toBuilder()
                        .apiId(API)
                        .id(PLAN)
                        .validation(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.MANUAL)
                        .build()
                )
            );
            apiCrudServiceInMemory.initWith(List.of(fixtures.core.model.ApiFixtures.aProxyApiV4().toBuilder().id(API).build()));
            when(groupService.findAllByEnvironment(anyString())).thenReturn(mockExcludedGroups());
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build());
            when(updatePlanDomainService.update(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    io.gravitee.apim.core.plan.model.Plan updated = invocation.getArgument(0);
                    updated.setUpdatedAt(null);
                    updated.setCreatedAt(null);
                    return updated;
                });

            final UpdatePlanV4 updatePlanV4 = PlanFixtures.anUpdatePlanV4();
            updatePlanV4.setExcludedGroups(List.of(nonExistentGroupId));
            final Response response = target.request().put(Entity.json(updatePlanV4));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Group [" + nonExistentGroupId + "] cannot be found.");
        }

        private Set<Group> mockExcludedGroups() {
            Set<Group> mockExcludedGroups = new HashSet<>();
            mockExcludedGroups.add(Group.builder().id("excludedGroup1").environmentId("environmentId").name("Group 1").build());
            mockExcludedGroups.add(Group.builder().id("excludedGroup2").environmentId("environmentId").name("Group 2").build());
            return mockExcludedGroups;
        }
    }

    @Nested
    class Delete {

        @BeforeEach
        void setUp() {
            target = rootTarget(PLAN);
        }

        @Test
        public void should_return_404_if_not_found() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

            final Response response = target.request().delete();

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
            verify(planServiceV4, never()).delete(any(), any());
        }

        @Test
        public void should_return_404_if_plan_associated_to_another_api() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build());

            final Response response = target.request().delete();

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
            verify(planServiceV4, never()).delete(any(), any());
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PLAN),
                    eq(API),
                    eq(RolePermissionAction.DELETE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().delete();

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
            verify(planServiceV4, never()).delete(any(), any());
        }

        @Test
        public void should_return_no_content_when_v4_plan_deleted() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build());

            final Response response = target.request().delete();

            assertThat(response).hasStatus(NO_CONTENT_204);
            verify(planServiceV4, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN);
        }

        @Test
        public void should_return_no_content_when_native_v4_plan_deleted() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aNativePlanEntityV4().toBuilder().id(PLAN).apiId(API).build());

            final Response response = target.request().delete();

            assertThat(response).hasStatus(NO_CONTENT_204);
            verify(planServiceV4, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN);
        }

        @Test
        public void should_return_no_content_when_v2_plan_deleted() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build());

            final Response response = target.request().delete();

            assertThat(response).hasStatus(NO_CONTENT_204);
            verify(planServiceV4, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN);
        }
    }

    @Nested
    class Close {

        @BeforeEach
        void setUp() {
            target = rootTarget(PLAN + "/_close");
        }

        @Test
        public void should_return_404_if_not_found() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
        }

        @Test
        public void should_return_404_if_plan_associated_to_another_api() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build());

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
            verifyNoInteractions(planServiceV4);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PLAN),
                    eq(API),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
            verifyNoInteractions(planServiceV4);
        }

        @Test
        public void should_return_plan_when_v4_plan_closed() {
            final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV4.close(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(PlanStatus.CLOSED).build());

            final Response response = target.request().post(Entity.json(null));
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV4.class)
                .extracting(PlanV4::getId, PlanV4::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.CLOSED);
        }

        @Test
        public void should_return_plan_when_v4_native_plan_closed() {
            var planEntity = PlanFixtures.aNativePlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV4.close(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(PlanStatus.CLOSED).build());

            final Response response = target.request().post(Entity.json(null));
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV4.class)
                .extracting(PlanV4::getId, PlanV4::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.CLOSED);
        }

        @Test
        public void should_return_plan_when_v2_plan_closed() {
            final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV4.close(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(io.gravitee.rest.api.model.PlanStatus.CLOSED).build());

            final Response response = target.request().post(Entity.json(null));
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV2.class)
                .extracting(PlanV2::getId, PlanV2::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.CLOSED);
        }
    }

    @Nested
    class Deprecate {

        @BeforeEach
        void setUp() {
            target = rootTarget(PLAN + "/_deprecate");
        }

        @Test
        public void should_return_404_if_not_found() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
            verifyNoInteractions(planServiceV2, planServiceV4);
        }

        @Test
        public void should_return_404_if_plan_associated_to_another_api() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build());

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
            verifyNoInteractions(planServiceV2, planServiceV4);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PLAN),
                    eq(API),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
            verifyNoInteractions(planServiceV2, planServiceV4);
        }

        @Test
        public void should_deprecate_v4_plan() {
            final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV4.deprecate(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(PlanStatus.DEPRECATED).build());

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV4.class)
                .extracting(PlanV4::getId, PlanV4::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.DEPRECATED);
        }

        @Test
        public void should_deprecate_v4_native_plan() {
            var planEntity = PlanFixtures.aNativePlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV4.deprecate(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(PlanStatus.DEPRECATED).build());

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV4.class)
                .extracting(PlanV4::getId, PlanV4::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.DEPRECATED);
        }

        @Test
        public void should_deprecate_v2_plan() {
            final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV2.deprecate(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(io.gravitee.rest.api.model.PlanStatus.DEPRECATED).build());

            final Response response = target.request().post(Entity.json(null));
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV2.class)
                .extracting(PlanV2::getId, PlanV2::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.DEPRECATED);
        }
    }

    @Nested
    class Publish {

        @BeforeEach
        void setUp() {
            target = rootTarget(PLAN + "/_publish");
        }

        @Test
        public void should_return_404_if_not_found() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
            verifyNoInteractions(planServiceV2, planServiceV4);
        }

        @Test
        public void should_return_404_if_plan_associated_to_another_api() {
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build());

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Plan [" + PLAN + "] cannot be found.");
            verifyNoInteractions(planServiceV2, planServiceV4);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PLAN),
                    eq(API),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
            verifyNoInteractions(planServiceV2, planServiceV4);
        }

        @Test
        public void should_return_plan_when_v4_plan_published() {
            final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV4.publish(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(PlanStatus.PUBLISHED).build());

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV4.class)
                .extracting(PlanV4::getId, PlanV4::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.PUBLISHED);
        }

        @Test
        public void should_return_plan_when_v4_native_plan_published() {
            var planEntity = PlanFixtures.aNativePlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV4.publish(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(PlanStatus.PUBLISHED).build());

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV4.class)
                .extracting(PlanV4::getId, PlanV4::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.PUBLISHED);
        }

        @Test
        public void should_return_plan_when_v2_plan_published() {
            final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();
            when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
            when(planServiceV2.publish(GraviteeContext.getExecutionContext(), PLAN))
                .thenReturn(planEntity.toBuilder().status(io.gravitee.rest.api.model.PlanStatus.PUBLISHED).build());

            final Response response = target.request().post(Entity.json(null));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PlanV2.class)
                .extracting(PlanV2::getId, PlanV2::getStatus)
                .containsExactly(PLAN, io.gravitee.rest.api.management.v2.rest.model.PlanStatus.PUBLISHED);
        }
    }
}
