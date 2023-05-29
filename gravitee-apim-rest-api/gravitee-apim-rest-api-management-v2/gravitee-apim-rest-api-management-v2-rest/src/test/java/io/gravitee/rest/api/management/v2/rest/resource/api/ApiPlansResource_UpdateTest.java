/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import fixtures.PlanFixtures;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiPlansResource_UpdateTest extends ApiPlansResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans" + "/" + PLAN;
    }

    @Autowired
    private PlanSearchService planSearchService;

    @Test
    public void should_return_404_if_not_found() {
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

        final Response response = rootTarget().request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] cannot be found.", error.getMessage());
    }

    @Test
    public void should_return_404_if_plan_associated_to_another_api() {
        final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] cannot be found.", error.getMessage());
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

        final Response response = rootTarget().request().put(Entity.json(PlanFixtures.aPlanEntityV4()));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_update_v4_plan() {
        final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
        final UpdatePlanV4 updatePlanV4 = PlanFixtures.anUpdatePlanV4();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
        when(planServiceV4.update(eq(GraviteeContext.getExecutionContext()), any(UpdatePlanEntity.class))).thenReturn(planEntity);

        final Response response = rootTarget().request().put(Entity.json(updatePlanV4));
        assertEquals(OK_200, response.getStatus());

        final PlanV4 planV4 = response.readEntity(PlanV4.class);
        assertEquals(PLAN, planV4.getId());

        verify(planServiceV4)
            .update(
                eq(GraviteeContext.getExecutionContext()),
                argThat(updatePlanEntity -> {
                    assertEquals(updatePlanV4.getName(), updatePlanEntity.getName());
                    return true;
                })
            );
    }

    @Test
    public void should_return_bad_request_when_setting_definition_to_v2_on_v4_plan() {
        final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();
        final UpdatePlanV4 updatePlanV4 = PlanFixtures.anUpdatePlanV4();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().put(Entity.json(updatePlanV4));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] is not valid.", error.getMessage());
    }

    @Test
    public void should_update_v2_plan() {
        final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();
        final UpdatePlanV2 updatePlanV2 = PlanFixtures.anUpdatePlanV2();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);
        when(planServiceV2.update(eq(GraviteeContext.getExecutionContext()), any(io.gravitee.rest.api.model.UpdatePlanEntity.class)))
            .thenReturn(planEntity);

        final Response response = rootTarget().request().put(Entity.json(updatePlanV2));
        assertEquals(OK_200, response.getStatus());

        final PlanV2 planV2 = response.readEntity(PlanV2.class);
        assertEquals(PLAN, planV2.getId());

        verify(planServiceV2)
            .update(
                eq(GraviteeContext.getExecutionContext()),
                argThat(updatePlanEntity -> {
                    assertEquals(updatePlanV2.getName(), updatePlanEntity.getName());
                    return true;
                })
            );
    }

    @Test
    public void should_return_bad_request_when_setting_definition_to_v4_on_v2_plan() {
        final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();
        final UpdatePlanV2 updatePlanV2 = PlanFixtures.anUpdatePlanV2();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().put(Entity.json(updatePlanV2));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] is not valid.", error.getMessage());
    }
}
