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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

public class ApiPlansResource_CloseTest extends ApiPlansResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans";
    }

    @Test
    public void shouldCloseApiPlan() {
        ApiEntity api = getApi();

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApiId(API);

        PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId("closed-plan-id");
        when(apiSearchServiceV4.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(api);
        doReturn(existingPlan).when(planServiceV4).findById(GraviteeContext.getExecutionContext(), PLAN);
        when(planServiceV4.close(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(closedPlan);

        final Response response = rootTarget().path(PLAN).path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(planServiceV4, times(1)).close(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
    }

    @Test
    public void shouldNotClosePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = rootTarget().path(PLAN).path("_close").request().post(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }
}
