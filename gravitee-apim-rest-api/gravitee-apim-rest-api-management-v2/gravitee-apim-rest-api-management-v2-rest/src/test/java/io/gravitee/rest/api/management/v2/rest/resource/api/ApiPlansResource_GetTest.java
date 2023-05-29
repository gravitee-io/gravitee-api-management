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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import fixtures.PlanFixtures;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiPlansResource_GetTest extends ApiPlansResourceTest {

    @Autowired
    private PlanSearchService planSearchService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans" + "/" + PLAN;
    }

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
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_return_v4_plan() {
        final PlanEntity planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).apiId(API).build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().get();
        assertEquals(OK_200, response.getStatus());

        var plan = response.readEntity(PlanV4.class);
        assertEquals(planEntity.getId(), plan.getId());
    }

    @Test
    public void should_return_v2_plan() {
        final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanEntityV2().toBuilder().id(PLAN).api(API).build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().get();
        assertEquals(OK_200, response.getStatus());

        var plan = response.readEntity(PlanV2.class);
        assertEquals(planEntity.getId(), plan.getId());
    }
}
