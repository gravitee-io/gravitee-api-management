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
package io.gravitee.rest.api.management.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromotionsResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String API_ID = "api-id";
    private static final String API_ENV_ID = "api-env-id";

    @Override
    protected String contextPath() {
        return "organizations/" + ORGANIZATION_ID + "/promotions/_search";
    }

    @BeforeEach
    void init() {
        var api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId(API_ENV_ID);
        when(apiSearchServiceV4.findRepositoryApiById(any(ExecutionContext.class), eq(API_ID))).thenReturn(api);
        givenApiEnvironmentBelongsTo(ORGANIZATION_ID);

        var promotion = new PromotionEntity();
        promotion.setId("promotion-id");
        promotion.setApiId(API_ID);
        when(promotionService.search(any(), any(), any())).thenReturn(new Page<>(List.of(promotion), 1, 1, 1));
    }

    @AfterEach
    void clean() {
        reset(apiSearchServiceV4, environmentService, promotionService, permissionService);
    }

    @Test
    void should_return_promotions_of_an_api_the_user_can_read() {
        Response response = search();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(response.readEntity(PromotionEntity[].class)).extracting(PromotionEntity::getId).containsExactly("promotion-id");
        verify(permissionService).hasPermission(
            argThat(context -> API_ENV_ID.equals(context.getEnvironmentId()) && ORGANIZATION_ID.equals(context.getOrganizationId())),
            eq(RolePermission.API_DEFINITION),
            eq(API_ID),
            eq(RolePermissionAction.READ)
        );
    }

    @Test
    void should_return_403_without_read_access_to_the_api() {
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                eq(RolePermission.API_DEFINITION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(false);

        Response response = search();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(promotionService, never()).search(any(), any(), any());
    }

    @Test
    void should_return_404_for_an_api_of_another_organization() {
        givenApiEnvironmentBelongsTo("another-organization");

        Response response = search();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(promotionService, never()).search(any(), any(), any());
    }

    @Test
    void should_return_404_for_an_unknown_api() {
        when(apiSearchServiceV4.findRepositoryApiById(any(ExecutionContext.class), eq(API_ID))).thenThrow(new ApiNotFoundException(API_ID));

        Response response = search();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(promotionService, never()).search(any(), any(), any());
    }

    private Response search() {
        return rootTarget("").queryParam("apiId", API_ID).queryParam("statuses", "TO_BE_VALIDATED").request().post(null);
    }

    private void givenApiEnvironmentBelongsTo(String organizationId) {
        var environment = new EnvironmentEntity();
        environment.setId(API_ENV_ID);
        environment.setOrganizationId(organizationId);
        when(environmentService.findById(API_ENV_ID)).thenReturn(environment);
    }
}
