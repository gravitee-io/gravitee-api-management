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

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import fixtures.PlanFixtures;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

public class ApiPlansResource_CreateTest extends ApiPlansResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans";
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

        final Response response = rootTarget().request().post(Entity.json(PlanFixtures.aCreatePlanV4()));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_create_v4_plan() {
        final CreatePlanV4 createPlanV4 = PlanFixtures.aCreatePlanV4();
        final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();

        when(planServiceV4.create(eq(GraviteeContext.getExecutionContext()), any(NewPlanEntity.class))).thenReturn(planEntity);

        final Response response = rootTarget().request().post(Entity.json(createPlanV4));
        assertEquals(CREATED_201, response.getStatus());

        final PlanV4 planV4 = response.readEntity(PlanV4.class);
        assertEquals(PLAN, planV4.getId());
        assertEquals(API, planV4.getApiId());

        verify(planServiceV4)
            .create(
                eq(GraviteeContext.getExecutionContext()),
                argThat(newPlanEntity -> {
                    assertEquals(createPlanV4.getName(), newPlanEntity.getName());
                    return true;
                })
            );
    }

    @Test
    public void should_create_v2_plan() {
        final CreatePlanV2 createPlanV2 = PlanFixtures.aCreatePlanV2();
        final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();

        when(planServiceV2.create(eq(GraviteeContext.getExecutionContext()), any(io.gravitee.rest.api.model.NewPlanEntity.class)))
            .thenReturn(planEntity);

        final Response response = rootTarget().request().post(Entity.json(createPlanV2));
        assertEquals(CREATED_201, response.getStatus());

        final PlanV2 planV2 = response.readEntity(PlanV2.class);
        assertEquals(PLAN, planV2.getId());
        assertEquals(API, planV2.getApiId());

        verify(planServiceV2)
            .create(
                eq(GraviteeContext.getExecutionContext()),
                argThat(newPlanEntity -> {
                    assertEquals(createPlanV2.getName(), newPlanEntity.getName());
                    return true;
                })
            );
    }
}
