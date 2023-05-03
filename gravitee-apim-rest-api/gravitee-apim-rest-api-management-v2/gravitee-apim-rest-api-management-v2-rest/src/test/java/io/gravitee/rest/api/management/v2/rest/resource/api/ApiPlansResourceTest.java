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
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.Plan;
import io.gravitee.rest.api.management.v2.rest.model.PlanValidation;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApiPlansResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String PLAN = "my-plan";
    private static final String ENVIRONMENT = "my-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans";
    }

    @Before
    public void init() throws TechnicalException {
        Mockito.reset(planService, apiSearchServiceV4);
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
    public void shouldCreateApiPlan() {
        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setName(PLAN);
        newPlanEntity.setDescription("my-plan-description");
        newPlanEntity.setValidation(PlanValidationType.AUTO);
        var planSecurity = new PlanSecurity();
        planSecurity.setType("api-key");
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

        when(planService.create(eq(GraviteeContext.getExecutionContext()), eq(fullNewPlanEntity))).thenReturn(createdPlanEntity);

        final Response response = rootTarget().request().post(Entity.json(newPlanEntity));

        assertEquals(CREATED_201, response.getStatus());
        assertEquals(rootTarget().path("new-plan-id").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
        var createdPlan = response.readEntity(Plan.class);
        assertEquals(API, createdPlan.getApiId());
        assertEquals(PLAN, createdPlan.getName());
        assertEquals("my-plan-description", createdPlan.getDescription());
        assertEquals(PlanValidation.AUTO, createdPlan.getValidation());
        var createdPlanSecurity = createdPlan.getSecurity();
        assertNotNull(createdPlanSecurity);
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType.API_KEY, createdPlanSecurity.getType());
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.PlanType.API, createdPlan.getType());
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.PlanStatus.STAGING, createdPlan.getStatus());
        assertEquals(createdAt.toInstant().atOffset(ZoneOffset.UTC), createdPlan.getCreatedAt());
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

        final Response response = rootTarget().request().post(Entity.json(newPlanEntity));
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

        doReturn(planEntity).when(planService).findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN));

        final Response response = rootTarget().path(PLAN).request().get();

        assertEquals(200, response.getStatus());
        var body = response.readEntity(Plan.class);
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

        final Response response = rootTarget().path(PLAN).request().get();
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
        doReturn(existingPlan).when(planService).findById(GraviteeContext.getExecutionContext(), PLAN);
        when(planService.close(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(closedPlan);

        final Response response = rootTarget().path(PLAN).path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(planService, times(1)).close(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
    }

    @Test
    public void shouldNotClosePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = rootTarget().path(PLAN).path("_close").request().post(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldDeleteApiPlan() {
        ApiEntity api = getApi();

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApiId(API);

        when(apiSearchServiceV4.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(api);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(existingPlan);

        final Response response = rootTarget().path(PLAN).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(planService, times(1)).delete(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
    }

    @Test
    public void shouldNotDeletePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = rootTarget().path(PLAN).request().delete();
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotUpdatePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = rootTarget().path(PLAN).request().put(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotPublishPlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = rootTarget().path(PLAN).path("_publish").request().post(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotDeprecatePlanWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_PLAN), eq(API), any());

        final Response response = rootTarget().path(PLAN).path("_deprecate").request().post(Entity.json(""));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    private ApiEntity getApi() {
        ApiEntity api = new ApiEntity();
        api.setId(API);
        api.setDefinitionVersion(DefinitionVersion.V4);
        return api;
    }
}
