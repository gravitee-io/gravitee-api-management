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
package io.gravitee.apim.core.analytics_engine.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApplicationQueryServiceInMemory;
import inmemory.FilterValuesQueryServiceInMemory;
import inmemory.FilterValuesQueryServiceInMemory.Call;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * A request scoped to an AI workspace offers its reader the values of the calls made through it, as the workspace's
 * aggregates count them. Another workspace sharing one of its APIs never shows, nor does a request field.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetFilterValuesUseCaseWorkspaceTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    private static final AuditInfo W_READER = caller("w-reader");
    private static final AuditInfo V_READER = caller("v-reader");
    private static final AuditInfo MEMBER = caller("member");
    private static final AuditInfo W_READER_MEMBER = caller("w-reader-member");

    private final FilterValuesQueryServiceInMemory calls = new FilterValuesQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProducts = new ApiProductQueryServiceInMemory();
    private final ApiQueryServiceInMemory apis = new ApiQueryServiceInMemory();
    private final ApplicationQueryServiceInMemory applications = new ApplicationQueryServiceInMemory();
    private final PlanQueryServiceInMemory plans = new PlanQueryServiceInMemory();
    private final PermissionDomainService permissions = mock(PermissionDomainService.class);
    private final AnalyticsQueryContextLoaderResolver contextLoader = mock(AnalyticsQueryContextLoaderResolver.class);
    private final FilterValueNameResolver nameResolver = mock(FilterValueNameResolver.class);

    private GetFilterValuesUseCase useCase;

    @BeforeEach
    void setUp() {
        apis.initWith(
            List.of(api("w-api", "W API"), api("shared-api", "Shared API"), api("w-idle-api", "W idle API"), api("v-api", "V API"))
        );
        apiProducts.initWith(
            List.of(
                workspace("W", ENV_ID, Set.of("w-api", "shared-api", "w-idle-api")),
                workspace("V", ENV_ID, Set.of("shared-api", "v-api")),
                workspace("W-elsewhere", "other-env-id", Set.of("w-api"))
            )
        );
        applications.initWith(
            List.of(
                application("app-w"),
                application("app-w-shared"),
                application("app-v"),
                application("app-member"),
                application("app-elsewhere")
            )
        );
        plans.initWith(
            List.of(
                plan("plan-w", "w-api"),
                plan("plan-w-shared", "shared-api"),
                plan("plan-v", "shared-api"),
                plan("plan-member", "member-api")
            )
        );
        calls.initWith(
            List.of(
                call("w-api", "W", "app-w", "plan-w", "10.0.0.1"),
                call("shared-api", "W", "app-w-shared", "plan-w-shared", "10.0.0.2"),
                call("shared-api", "V", "app-v", "plan-v", "10.0.0.3"),
                call("member-api", null, "app-member", "plan-member", "10.0.0.4"),
                call("w-api", "W-elsewhere", "app-elsewhere", "plan-w", "10.0.0.5")
            )
        );
        reads("w-reader", "W");
        reads("w-reader", "W-elsewhere");
        reads("v-reader", "V");
        reads("w-reader-member", "W");
        readsApis(W_READER, Map.of());
        readsApis(V_READER, Map.of());
        readsApis(MEMBER, Map.of("member-api", "Member API"));
        readsApis(W_READER_MEMBER, Map.of("member-api", "Member API"));
        when(nameResolver.resolveNames(any(), any(), anyList())).thenAnswer(invocation ->
            invocation.<List<String>>getArgument(2).stream().collect(Collectors.toMap(id -> id, id -> "Name of " + id))
        );
        when(nameResolver.planLabels(any(), any())).thenAnswer(invocation ->
            invocation.<Collection<Plan>>getArgument(1).stream().collect(Collectors.toMap(Plan::getId, Plan::getName))
        );
        useCase = new GetFilterValuesUseCase(
            new AnalyticsDefinitionYAMLQueryService(),
            calls,
            nameResolver,
            contextLoader,
            applications,
            plans,
            apiProducts,
            new AiWorkspaceAnalyticsApisDomainService(apiProducts, apis, permissions)
        );
    }

    @Test
    void should_offer_its_reader_the_values_of_the_calls_made_through_the_workspace() {
        assertThat(ids(W_READER, "W", "APPLICATION", null)).containsExactlyInAnyOrder("app-w", "app-w-shared");
        assertThat(ids(W_READER, "W", "API", null)).containsExactlyInAnyOrder("w-api", "shared-api");
        assertThat(ids(W_READER, "W", "API_PRODUCT", null)).containsExactly("W");
    }

    @Test
    void should_search_by_name_among_the_values_of_the_calls_made_through_the_workspace() {
        assertThat(ids(W_READER, "W", "APPLICATION", "app")).containsExactlyInAnyOrder("app-w", "app-w-shared");
        assertThat(ids(W_READER, "W", "API", "api")).containsExactlyInAnyOrder("w-api", "shared-api");
    }

    /** A workspace API without a call through the workspace, or an API read outside it, has no call to count there. */
    @Test
    void should_search_apis_by_name_among_those_with_calls_made_through_the_workspace() {
        assertThat(ids(W_READER_MEMBER, "W", "API", "api")).containsExactlyInAnyOrder("w-api", "shared-api");
    }

    @Test
    void should_not_offer_the_plans_another_workspace_uses_on_a_shared_api() {
        assertThat(ids(W_READER, "W", "PLAN", "plan")).containsExactlyInAnyOrder("plan-w", "plan-w-shared");
    }

    @Test
    void should_offer_nothing_of_a_workspace_of_another_environment() {
        assertThat(ids(W_READER, "W-elsewhere", "APPLICATION", null)).isEmpty();
        assertThat(ids(W_READER, "W-elsewhere", "APPLICATION", "app")).isEmpty();
    }

    @Test
    void should_offer_nothing_of_a_workspace_to_the_reader_of_another_one() {
        assertThat(ids(V_READER, "W", "APPLICATION", null)).isEmpty();
        assertThat(ids(V_READER, "W", "APPLICATION", "app")).isEmpty();
    }

    @Test
    void should_offer_nothing_of_the_workspace_when_the_request_is_not_scoped_to_it() {
        assertThat(ids(W_READER, null, "APPLICATION", null)).isEmpty();
        assertThat(ids(W_READER, null, "API", null)).isEmpty();
    }

    @Test
    void should_never_offer_the_workspace_reader_a_field_identifying_callers() {
        assertThat(ids(W_READER, "W", "CONSUMER_IP", null)).isEmpty();
    }

    @Test
    void should_offer_a_member_the_values_of_their_apis_when_the_request_is_not_scoped() {
        assertThat(ids(MEMBER, null, "APPLICATION", null)).containsExactly("app-member");
        assertThat(ids(MEMBER, null, "API", null)).containsExactly("member-api");
    }

    private List<String> ids(AuditInfo caller, String apiProductId, String filterName, String query) {
        return useCase
            .execute(
                new GetFilterValuesUseCase.Input(
                    caller,
                    filterName,
                    null,
                    null,
                    1,
                    100,
                    query,
                    Set.of(),
                    AnalyticsScope.MANAGEMENT,
                    Set.of(),
                    apiProductId
                )
            )
            .valuesPage()
            .data()
            .stream()
            .map(value -> value.id() != null ? value.id() : value.value())
            .toList();
    }

    private void reads(String userId, String workspaceId) {
        when(
            permissions.hasPermission(ORG_ID, userId, RolePermission.AI_WORKSPACE_USER, workspaceId, RolePermissionAction.READ)
        ).thenReturn(true);
    }

    /** The same APIs for both data kinds: the identifying field is closed by the workspace rule, not by the rows. */
    private void readsApis(AuditInfo caller, Map<String, String> apiNamesById) {
        var context = new AnalyticsQueryContext(
            caller,
            new ExecutionContext(ORG_ID, ENV_ID),
            apiNamesById.keySet(),
            apiNamesById,
            Map.of(),
            Map.of(),
            Optional.empty()
        );
        when(contextLoader.load(caller, AnalyticsScope.MANAGEMENT, ApiDataKind.AGGREGATES)).thenReturn(context);
        when(contextLoader.load(caller, AnalyticsScope.MANAGEMENT, ApiDataKind.PER_REQUEST)).thenReturn(context);
    }

    private static Call call(String apiId, String apiProductId, String applicationId, String planId, String consumerIp) {
        return new Call(
            apiId,
            apiProductId,
            Map.of(FilterSpec.Name.APPLICATION, applicationId, FilterSpec.Name.PLAN, planId, FilterSpec.Name.CONSUMER_IP, consumerIp)
        );
    }

    private static AuditInfo caller(String userId) {
        return AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).actor(AuditActor.builder().userId(userId).build()).build();
    }

    private static Api api(String id, String name) {
        return ApiFixtures.aProxyApiV4().toBuilder().id(id).name(name).environmentId(ENV_ID).build();
    }

    private static ApiProduct workspace(String id, String environmentId, Set<String> apiIds) {
        return ApiProduct.builder().id(id).name(id).environmentId(environmentId).apiIds(apiIds).kind(ApiProductKind.AI_WORKSPACE).build();
    }

    private static Plan plan(String id, String apiId) {
        return PlanFixtures.aPlanHttpV4().toBuilder().id(id).name(id).referenceId(apiId).environmentId(ENV_ID).build();
    }

    private static BaseApplicationEntity application(String id) {
        var application = new BaseApplicationEntity();
        application.setId(id);
        application.setName(id);
        application.setEnvironmentId(ENV_ID);
        return application;
    }
}
