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
package io.gravitee.rest.api.management.rest.resource.v4.api;

import static io.gravitee.common.http.HttpStatusCode.*;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApiPlansResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String PLAN = "my-plan";

    @Override
    protected String contextPath() {
        return "v4/apis";
    }

    @Before
    public void init() {
        Mockito.reset(planServiceV4, apiSearchServiceV4);
        GraviteeContext.cleanContext();
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldCreateApiPlan() {
        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setName(PLAN);
        newPlanEntity.setDescription("my-plan-description");
        newPlanEntity.setValidation(PlanValidationType.AUTO);
        var planSecurity = new PlanSecurity();
        planSecurity.setType("planType");
        newPlanEntity.setSecurity(planSecurity);
        newPlanEntity.setStatus(PlanStatus.STAGING);

        var fullNewPlanEntity = newPlanEntity;
        fullNewPlanEntity.setType(PlanType.API);
        fullNewPlanEntity.setApiId(API);

        PlanEntity createdPlanEntity = new PlanEntity();
        createdPlanEntity.setId("new-plan-id");
        createdPlanEntity.setName(PLAN);
        createdPlanEntity.setDescription("my-plan-description");
        createdPlanEntity.setValidation(PlanValidationType.AUTO);
        createdPlanEntity.setSecurity(planSecurity);
        createdPlanEntity.setType(PlanType.API);
        createdPlanEntity.setStatus(PlanStatus.STAGING);
        createdPlanEntity.setApiId(API);

        Date createdAt = new Date();
        createdPlanEntity.setCreatedAt(createdAt);

        when(planServiceV4.create(eq(GraviteeContext.getExecutionContext()), eq(fullNewPlanEntity))).thenReturn(createdPlanEntity);

        final Response response = envTarget().path(API).path("plans").request().post(Entity.json(newPlanEntity));

        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path(API).path("plans").path("new-plan-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
        var createdPlan = response.readEntity(PlanEntity.class);
        assertEquals(API, createdPlan.getApiId());
        assertEquals(PLAN, createdPlan.getName());
        assertEquals("my-plan-description", createdPlan.getDescription());
        assertEquals(PlanValidationType.AUTO, createdPlan.getValidation());
        var createdPlanSecurity = createdPlan.getSecurity();
        assertNotNull(createdPlanSecurity);
        assertEquals("planType", createdPlanSecurity.getType());
        assertEquals(PlanType.API, createdPlan.getType());
        assertEquals(PlanStatus.STAGING, createdPlan.getStatus());
        assertEquals(createdAt, createdPlan.getCreatedAt());
    }

    @Test
    public void shouldNotCreatePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setName(PLAN);
        newPlanEntity.setDescription("my-plan-description");
        newPlanEntity.setValidation(PlanValidationType.AUTO);
        var planSecurity = new PlanSecurity();
        planSecurity.setType("planType");
        newPlanEntity.setSecurity(planSecurity);
        newPlanEntity.setStatus(PlanStatus.STAGING);

        final Response response = envTarget().path(API).path("plans").request().post(Entity.json(newPlanEntity));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldGetApiPlanById() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setVisibility(Visibility.PUBLIC);

        doReturn(apiEntity).when(apiSearchServiceV4).findById(GraviteeContext.getExecutionContext(), API);

        PlanEntity planEntity = new PlanEntity();
        planEntity.setId("new-plan-id");
        planEntity.setName(PLAN);
        planEntity.setDescription("my-plan-description");
        planEntity.setValidation(PlanValidationType.AUTO);
        planEntity.setType(PlanType.API);
        planEntity.setStatus(PlanStatus.STAGING);
        planEntity.setApiId(API);

        doReturn(planEntity).when(planServiceV4).findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN));

        final Response response = envTarget().path(API).path("plans").path(PLAN).request().get();

        assertEquals(200, response.getStatus());
        var body = response.readEntity(PlanEntity.class);
        assertNotNull(body);
    }

    @Test
    public void shouldNotGetPlanWithInsufficientRights() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setVisibility(Visibility.PRIVATE);

        doReturn(apiEntity).when(apiSearchServiceV4).findById(GraviteeContext.getExecutionContext(), API);
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = envTarget().path(API).path("plans").path(PLAN).request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());
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

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(planServiceV4, times(1)).close(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
    }

    @Test
    public void shouldNotClosePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_close").request().post(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldDeleteApiPlan() {
        ApiEntity api = getApi();

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApiId(API);

        when(apiSearchServiceV4.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(api);
        when(planServiceV4.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(existingPlan);

        final Response response = envTarget().path(API).path("plans").path(PLAN).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(planServiceV4, times(1)).delete(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
    }

    @Test
    public void shouldNotDeletePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = envTarget().path(API).path("plans").path(PLAN).request().delete();
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotUpdatePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = envTarget().path(API).path("plans").path(PLAN).request().put(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotPublishPlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_publish").request().post(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotDeprecatePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_deprecate").request().post(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    private ApiEntity getApi() {
        ApiEntity api = new ApiEntity();
        api.setId(API);
        api.setDefinitionVersion(DefinitionVersion.V4);
        return api;
    }
}
