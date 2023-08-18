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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.PlanFixtures;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PlanV2;
import io.gravitee.rest.api.management.v2.rest.model.PlanV4;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiPlansResource_DeprecateTest extends ApiPlansResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans" + "/" + PLAN + "/_deprecate";
    }

    @Autowired
    private PlanSearchService planSearchService;

    @Test
    public void should_return_404_if_not_found() {
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

        final Response response = rootTarget().request().post(Entity.json(null));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] cannot be found.", error.getMessage());

        verify(planServiceV4, never()).deprecate(any(), any());
        verify(planServiceV2, never()).deprecate(any(), any());
    }

    @Test
    public void should_return_404_if_plan_associated_to_another_api() {
        final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().post(Entity.json(null));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] cannot be found.", error.getMessage());

        verify(planServiceV4, never()).deprecate(any(), any());
        verify(planServiceV2, never()).deprecate(any(), any());
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

        final Response response = rootTarget().request().post(Entity.json(null));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());

        verify(planServiceV4, never()).deprecate(any(), any());
        verify(planServiceV2, never()).deprecate(any(), any());
    }

    @Test
    public void should_return_plan_when_v4_plan_deprecated() {
        final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
        when(planServiceV4.deprecate(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().post(Entity.json(null));
        assertEquals(OK_200, response.getStatus());

        var plan = response.readEntity(PlanV4.class);
        assertEquals(planEntity.getId(), plan.getId());

        verify(planServiceV4, times(1)).deprecate(GraviteeContext.getExecutionContext(), PLAN);
    }

    @Test
    public void should_return_plan_when_v2_plan_deprecated() {
        final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
        when(planServiceV2.deprecate(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().post(Entity.json(null));
        assertEquals(OK_200, response.getStatus());

        var plan = response.readEntity(PlanV2.class);
        assertEquals(planEntity.getId(), plan.getId());

        verify(planServiceV2, times(1)).deprecate(GraviteeContext.getExecutionContext(), PLAN);
    }
}
