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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import fixtures.PlanFixtures;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
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

public class ApiPlansResource_DeleteTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String PLAN = "my-plan";
    private static final String ENVIRONMENT = "my-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans" + "/" + PLAN;
    }

    @Autowired
    private PlanSearchService planSearchService;

    @Before
    public void init() throws TechnicalException {
        Mockito.reset(planService, planSearchService);
        GraviteeContext.cleanContext();

        Api api = new Api();
        api.setId(API);
        api.setEnvironmentId(ENVIRONMENT);
        doReturn(Optional.of(api)).when(apiRepository).findById(API);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void should_return_404_if_not_found() {
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenThrow(new PlanNotFoundException(PLAN));

        final Response response = rootTarget().request().delete();
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] can not be found.", error.getMessage());

        verify(planService, never()).delete(any(), any());
    }

    @Test
    public void should_return_404_if_plan_associated_to_another_api() {
        final PlanEntity planEntity = PlanFixtures.aPlanV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().delete();
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] can not be found.", error.getMessage());

        verify(planService, never()).delete(any(), any());
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

        final Response response = rootTarget().request().delete();
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());

        verify(planService, never()).delete(any(), any());
    }

    @Test
    public void should_return_no_content_when_v4_plan_deleted() {
        final PlanEntity planEntity = PlanFixtures.aPlanV4().toBuilder().id(PLAN).apiId(API).build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().delete();
        assertEquals(NO_CONTENT_204, response.getStatus());

        assertEquals("", response.readEntity(String.class));
        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN);
    }

    @Test
    public void should_return_no_content_when_v2_plan_deleted() {
        final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanV2().toBuilder().id(PLAN).api(API).build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().delete();
        assertEquals(NO_CONTENT_204, response.getStatus());

        assertEquals("", response.readEntity(String.class));
        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN);
    }
}
