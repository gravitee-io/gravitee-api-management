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

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String PLAN = "my-plan";

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Before
    public void init() {
        Mockito.reset(planService, apiService, apiSearchServiceV4, groupService);
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenReturn(true);
        mockLegacyApi();
        GraviteeContext.cleanContext();
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnBadRequestWhenGettingPlansForV4Api() {
        io.gravitee.rest.api.model.v4.api.ApiEntity v4Api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        v4Api.setId(API);
        v4Api.setDefinitionVersion(DefinitionVersion.V4);
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(v4Api);

        final Response response = envTarget().path(API).path("plans").request().get();

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(apiService, never()).findById(any(), eq(API));
        verify(planService, never()).findByApi(any(), any());
    }

    @Test
    public void shouldReturnBadRequestWhenGettingSinglePlanForV4Api() {
        io.gravitee.rest.api.model.v4.api.ApiEntity v4Api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        v4Api.setId(API);
        v4Api.setDefinitionVersion(DefinitionVersion.V4);
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(v4Api);

        final Response response = envTarget().path(API).path("plans").path(PLAN).request().get();

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(planService, never()).findById(any(), any());
    }

    @Test
    public void shouldReturnBadRequestWhenGettingPlansForFederatedApi() {
        io.gravitee.rest.api.model.v4.api.ApiEntity federatedApi = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        federatedApi.setId(API);
        federatedApi.setDefinitionVersion(DefinitionVersion.FEDERATED);
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(federatedApi);

        final Response response = envTarget().path(API).path("plans").request().get();

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(apiService, never()).findById(any(), eq(API));
    }

    @Test
    public void shouldGetApiPlansForV2Api() {
        PlanEntity publishedPlan = buildPlan("published-plan", PlanStatus.PUBLISHED, 1);
        PlanEntity stagingPlan = buildPlan("staging-plan", PlanStatus.STAGING, 2);
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API)).thenReturn(Set.of(publishedPlan, stagingPlan));
        when(groupService.isUserAuthorizedToAccessApiData(any(ApiEntity.class), any(), any())).thenReturn(true);

        final Response response = envTarget().path(API).path("plans").request().get();

        assertEquals(OK_200, response.getStatus());
        List<PlanEntity> plans = response.readEntity(new GenericType<>() {});
        assertNotNull(plans);
        assertEquals(1, plans.size());
        assertEquals("published-plan", plans.get(0).getId());
        verify(apiService).findById(GraviteeContext.getExecutionContext(), API);
    }

    @Test
    public void shouldGetSingleApiPlanForPublicV2Api() {
        PlanEntity planEntity = buildPlan(PLAN, PlanStatus.PUBLISHED, 1);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = envTarget().path(API).path("plans").path(PLAN).request().get();

        assertEquals(OK_200, response.getStatus());
        assertEquals(PLAN, response.readEntity(PlanEntity.class).getId());
        verify(apiService).findById(GraviteeContext.getExecutionContext(), API);
    }

    @Test
    public void shouldCreateApiPlan() {
        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setName(PLAN);
        newPlanEntity.setDescription("my-plan-description");
        newPlanEntity.setValidation(PlanValidationType.AUTO);
        newPlanEntity.setSecurity(PlanSecurityType.KEY_LESS);
        newPlanEntity.setType(PlanType.API);
        newPlanEntity.setStatus(PlanStatus.STAGING);

        PlanEntity createdPlanEntity = new PlanEntity();
        createdPlanEntity.setId("new-plan-id");
        when(planService.create(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(createdPlanEntity);

        final Response response = envTarget().path(API).path("plans").request().post(Entity.json(newPlanEntity));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path(API).path("plans").path("new-plan-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }

    @Test
    public void shouldCloseApiPlan() {
        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApi(API);

        PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId("closed-plan-id");
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(existingPlan);
        when(planService.close(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(closedPlan);

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(planService, times(1)).close(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
        verify(apiService, never()).update(eq(GraviteeContext.getExecutionContext()), eq(API), any());
    }

    @Test
    public void shouldDeleteApiPlan() {
        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApi(API);

        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(existingPlan);

        final Response response = envTarget().path(API).path("plans").path(PLAN).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(planService, times(1)).delete(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
    }

    private void mockLegacyApi() {
        ApiEntity legacyApi = new ApiEntity();
        legacyApi.setId(API);
        legacyApi.setVisibility(Visibility.PUBLIC);
        legacyApi.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        when(apiSearchServiceV4.findGenericById(any(ExecutionContext.class), eq(API), eq(false), eq(false), eq(false))).thenReturn(
            legacyApi
        );
        when(apiService.findById(any(ExecutionContext.class), eq(API))).thenReturn(legacyApi);
    }

    private static PlanEntity buildPlan(String id, PlanStatus status, int order) {
        PlanEntity plan = new PlanEntity();
        plan.setId(id);
        plan.setApi(API);
        plan.setStatus(status);
        plan.setOrder(order);
        plan.setSecurity(PlanSecurityType.KEY_LESS);
        return plan;
    }
}
