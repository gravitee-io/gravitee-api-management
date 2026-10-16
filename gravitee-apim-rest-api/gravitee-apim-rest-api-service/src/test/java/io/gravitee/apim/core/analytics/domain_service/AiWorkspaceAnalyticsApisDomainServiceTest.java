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
package io.gravitee.apim.core.analytics.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AiWorkspaceAnalyticsApisDomainServiceTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "environment-id";
    private static final String OTHER_ENV_ID = "other-env-id";
    private static final String USER_ID = "user-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORG_ID, ENV_ID);

    private static final Api LLM = ApiFixtures.aProxyApiV4().toBuilder().id("llm").environmentId(ENV_ID).build();
    private static final Api MCP = ApiFixtures.aProxyApiV4().toBuilder().id("mcp").environmentId(ENV_ID).build();
    private static final Api ELSEWHERE = ApiFixtures.aProxyApiV4().toBuilder().id("elsewhere").environmentId(OTHER_ENV_ID).build();

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);

    private AiWorkspaceAnalyticsApisDomainService cut;

    @BeforeEach
    void setUp() {
        apiQueryService.initWith(List.of(LLM, MCP, ELSEWHERE));
        apiProductQueryService.initWith(
            List.of(
                workspace("workspace", ENV_ID, Set.of("llm", "elsewhere")),
                workspace("other-workspace", ENV_ID, Set.of("mcp")),
                workspace("workspace-elsewhere", OTHER_ENV_ID, Set.of("elsewhere")),
                ApiProduct.builder().id("product").environmentId(ENV_ID).apiIds(Set.of("mcp")).build()
            )
        );
        cut = new AiWorkspaceAnalyticsApisDomainService(apiProductQueryService, apiQueryService, permissionDomainService);
    }

    @AfterEach
    void tearDown() {
        apiProductQueryService.reset();
        apiQueryService.reset();
    }

    @Test
    void should_return_the_apis_of_the_workspace_its_user_reads_without_any_api_membership() {
        givenUserReadsWorkspace("workspace");

        assertThat(cut.findWorkspaceApis(EXECUTION_CONTEXT, USER_ID, "workspace")).extracting(Api::getId).containsExactly("llm");
    }

    @Test
    void should_return_nothing_for_a_workspace_the_user_does_not_read() {
        givenUserReadsWorkspace("other-workspace");

        assertThat(cut.findWorkspaceApis(EXECUTION_CONTEXT, USER_ID, "workspace")).isEmpty();
    }

    @Test
    void should_return_nothing_for_a_workspace_of_another_environment() {
        givenUserReadsWorkspace("workspace-elsewhere");

        assertThat(cut.findWorkspaceApis(EXECUTION_CONTEXT, USER_ID, "workspace-elsewhere")).isEmpty();
    }

    @Test
    void should_return_nothing_for_an_api_product_that_is_not_a_workspace() {
        givenUserReadsWorkspace("product");

        assertThat(cut.findWorkspaceApis(EXECUTION_CONTEXT, USER_ID, "product")).isEmpty();
    }

    private void givenUserReadsWorkspace(String workspaceId) {
        when(
            permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.AI_WORKSPACE_USER, workspaceId, RolePermissionAction.READ)
        ).thenReturn(true);
    }

    private static ApiProduct workspace(String id, String environmentId, Set<String> apiIds) {
        return ApiProduct.builder().id(id).environmentId(environmentId).apiIds(apiIds).kind(ApiProductKind.AI_WORKSPACE).build();
    }
}
