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
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.FilterValuesQueryServiceInMemory;
import inmemory.FilterValuesQueryServiceInMemory.Call;
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
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
 * A request scoped to an AI workspace labels what its reader may count there: the workspace, its APIs, and the ids
 * found in the calls made through it. Another workspace sharing one of its APIs never shows.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ResolveFilterLabelsUseCaseWorkspaceTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    private static final AuditInfo W_READER = caller("w-reader");
    private static final AuditInfo V_READER = caller("v-reader");
    private static final AuditInfo MEMBER = caller("member");

    private static final Api W_API = api("w-api", "W API");
    private static final Api SHARED_API = api("shared-api", "Shared API");
    private static final Api V_API = api("v-api", "V API");

    private final FilterValuesQueryServiceInMemory calls = new FilterValuesQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProducts = new ApiProductQueryServiceInMemory();
    private final ApiQueryServiceInMemory apis = new ApiQueryServiceInMemory();
    private final PermissionDomainService permissions = mock(PermissionDomainService.class);
    private final AnalyticsQueryContextLoaderResolver contextLoader = mock(AnalyticsQueryContextLoaderResolver.class);
    private final FilterValueNameResolver nameResolver = mock(FilterValueNameResolver.class);

    private ResolveFilterLabelsUseCase useCase;

    @BeforeEach
    void setUp() {
        apis.initWith(List.of(W_API, SHARED_API, V_API, api("w-idle-api", "W idle API"), api("member-api", "Member API")));
        apiProducts.initWith(
            List.of(
                workspace("W", ENV_ID, Set.of("w-api", "shared-api", "w-idle-api")),
                workspace("V", ENV_ID, Set.of("shared-api", "v-api")),
                workspace("W-elsewhere", "other-env-id", Set.of("w-api"))
            )
        );
        calls.initWith(
            List.of(
                new Call("w-api", "W", Map.of(FilterSpec.Name.APPLICATION, "app-w", FilterSpec.Name.PLAN, "plan-w")),
                new Call("shared-api", "W", Map.of(FilterSpec.Name.APPLICATION, "app-w-shared", FilterSpec.Name.PLAN, "plan-w-shared")),
                new Call("shared-api", "V", Map.of(FilterSpec.Name.APPLICATION, "app-v", FilterSpec.Name.PLAN, "plan-v")),
                new Call("member-api", null, Map.of(FilterSpec.Name.APPLICATION, "app-member", FilterSpec.Name.PLAN, "plan-member"))
            )
        );
        reads("w-reader", "W");
        reads("w-reader", "W-elsewhere");
        reads("v-reader", "V");
        readsApis(W_READER, Map.of());
        readsApis(V_READER, Map.of());
        readsApis(MEMBER, Map.of("member-api", "Member API"));
        when(nameResolver.resolveNames(any(), any(), anyList())).thenAnswer(invocation ->
            invocation.<List<String>>getArgument(2).stream().collect(Collectors.toMap(id -> id, id -> "Name of " + id))
        );
        useCase = new ResolveFilterLabelsUseCase(
            nameResolver,
            contextLoader,
            calls,
            new AiWorkspaceAnalyticsApisDomainService(apiProducts, apis, permissions)
        );
    }

    @Test
    void should_label_the_workspace_its_apis_and_the_ids_of_its_calls_for_its_reader() {
        var labels = resolve(
            W_READER,
            "W",
            Map.of(
                FilterSpec.Name.API_PRODUCT,
                List.of("W"),
                FilterSpec.Name.API,
                List.of("w-api", "shared-api"),
                FilterSpec.Name.APPLICATION,
                List.of("app-w", "app-w-shared"),
                FilterSpec.Name.PLAN,
                List.of("plan-w", "plan-w-shared")
            )
        );

        assertThat(labels).isEqualTo(
            Map.of(
                "API_PRODUCT",
                Map.of("W", "Name of W"),
                "API",
                Map.of("w-api", "W API", "shared-api", "Shared API"),
                "APPLICATION",
                Map.of("app-w", "Name of app-w", "app-w-shared", "Name of app-w-shared"),
                "PLAN",
                Map.of("plan-w", "Name of plan-w", "plan-w-shared", "Name of plan-w-shared")
            )
        );
    }

    @Test
    void should_not_label_what_another_workspace_holds_on_a_shared_api() {
        var labels = resolve(
            W_READER,
            "W",
            Map.of(
                FilterSpec.Name.API_PRODUCT,
                List.of("V"),
                FilterSpec.Name.API,
                List.of("v-api"),
                FilterSpec.Name.APPLICATION,
                List.of("app-v"),
                FilterSpec.Name.PLAN,
                List.of("plan-v")
            )
        );

        assertThat(labels).allSatisfy((filterName, resolved) -> assertThat(resolved).as(filterName).isEmpty());
    }

    @Test
    void should_not_label_a_workspace_api_without_a_call_made_through_the_workspace() {
        var labels = resolve(W_READER, "W", Map.of(FilterSpec.Name.API, List.of("w-idle-api", "w-api")));

        assertThat(labels).isEqualTo(Map.of("API", Map.of("w-api", "W API")));
    }

    @Test
    void should_label_nothing_of_a_workspace_for_the_reader_of_another_one() {
        var labels = resolve(V_READER, "W", workspaceIds());

        assertThat(labels).allSatisfy((filterName, resolved) -> assertThat(resolved).as(filterName).isEmpty());
    }

    @Test
    void should_label_nothing_of_the_workspace_when_the_request_is_not_scoped_to_it() {
        var labels = resolve(W_READER, null, workspaceIds());

        assertThat(labels).allSatisfy((filterName, resolved) -> assertThat(resolved).as(filterName).isEmpty());
    }

    @Test
    void should_label_nothing_of_a_workspace_of_another_environment() {
        var labels = resolve(W_READER, "W-elsewhere", Map.of(FilterSpec.Name.API, List.of("w-api")));

        assertThat(labels).allSatisfy((filterName, resolved) -> assertThat(resolved).as(filterName).isEmpty());
    }

    @Test
    void should_label_for_a_member_what_their_apis_hold_whatever_the_scope() {
        var ids = Map.of(FilterSpec.Name.API, List.of("member-api", "w-api"), FilterSpec.Name.APPLICATION, List.of("app-member", "app-w"));

        var expected = Map.of("API", Map.of("member-api", "Member API"), "APPLICATION", Map.of("app-member", "Name of app-member"));
        assertThat(resolve(MEMBER, null, ids)).isEqualTo(expected);
        assertThat(resolve(MEMBER, "W", ids)).isEqualTo(expected);
    }

    private static Map<FilterSpec.Name, List<String>> workspaceIds() {
        return Map.of(
            FilterSpec.Name.API_PRODUCT,
            List.of("W"),
            FilterSpec.Name.API,
            List.of("w-api", "shared-api"),
            FilterSpec.Name.APPLICATION,
            List.of("app-w", "app-w-shared"),
            FilterSpec.Name.PLAN,
            List.of("plan-w")
        );
    }

    private Map<String, Map<String, String>> resolve(AuditInfo caller, String apiProductId, Map<FilterSpec.Name, List<String>> ids) {
        var entries = ids
            .entrySet()
            .stream()
            .map(entry -> new ResolveFilterLabelsUseCase.Entry(entry.getKey().name(), entry.getValue()))
            .toList();
        return useCase
            .execute(new ResolveFilterLabelsUseCase.Input(caller, entries, apiProductId))
            .entries()
            .stream()
            .collect(
                Collectors.toMap(ResolveFilterLabelsUseCase.ResolvedEntry::filterName, ResolveFilterLabelsUseCase.ResolvedEntry::labels)
            );
    }

    private void reads(String userId, String workspaceId) {
        when(
            permissions.hasPermission(ORG_ID, userId, RolePermission.AI_WORKSPACE_USER, workspaceId, RolePermissionAction.READ)
        ).thenReturn(true);
    }

    private void readsApis(AuditInfo caller, Map<String, String> apiNamesById) {
        when(contextLoader.load(caller, AnalyticsScope.MANAGEMENT, ApiDataKind.AGGREGATES)).thenReturn(
            new AnalyticsQueryContext(
                caller,
                new ExecutionContext(ORG_ID, ENV_ID),
                apiNamesById.keySet(),
                apiNamesById,
                Map.of(),
                Map.of(),
                Optional.empty()
            )
        );
    }

    private static AuditInfo caller(String userId) {
        return AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).actor(AuditActor.builder().userId(userId).build()).build();
    }

    private static Api api(String id, String name) {
        return ApiFixtures.aProxyApiV4().toBuilder().id(id).name(name).environmentId(ENV_ID).build();
    }

    private static ApiProduct workspace(String id, String environmentId, Set<String> apiIds) {
        return ApiProduct.builder().id(id).environmentId(environmentId).apiIds(apiIds).kind(ApiProductKind.AI_WORKSPACE).build();
    }
}
