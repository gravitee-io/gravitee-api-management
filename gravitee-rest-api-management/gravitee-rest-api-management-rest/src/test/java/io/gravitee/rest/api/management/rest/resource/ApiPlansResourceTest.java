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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Plan;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.util.Arrays;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
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
        Mockito.reset(planService, apiService);
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
        when(planService.create(any())).thenReturn(createdPlanEntity);

        final Response response = envTarget().path(API).path("plans").request().post(Entity.json(newPlanEntity));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path(API).path("plans").path("new-plan-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }

    @Test
    public void shouldCloseApiV1Plan() {
        ApiEntity api = getApi(DefinitionVersion.V1);

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApi(API);

        PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId("closed-plan-id");
        when(apiService.findById(API)).thenReturn(api);
        when(planService.findById(PLAN)).thenReturn(existingPlan);
        when(planService.close(any(), any())).thenReturn(closedPlan);

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(apiService, never()).update(eq(API), any());
    }

    @Test
    public void shouldCloseApiV2Plan() {
        ApiEntity api = getApi(DefinitionVersion.V2);

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApi(API);

        PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId("plan-1");
        when(apiService.findById(API)).thenReturn(api);
        when(planService.findById(PLAN)).thenReturn(existingPlan);
        when(planService.close(any(), any())).thenReturn(closedPlan);

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(apiService, times(1)).update(eq(API), any());
    }

    @Test
    public void shouldDeleteApiV1Plan() {
        ApiEntity api = getApi(DefinitionVersion.V1);

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApi(API);

        when(apiService.findById(API)).thenReturn(api);
        when(planService.findById(PLAN)).thenReturn(existingPlan);

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(apiService, never()).update(eq(API), any());
    }

    @Test
    public void shouldDeleteApiV2Plan() {
        ApiEntity api = getApi(DefinitionVersion.V2);

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setApi(API);

        when(apiService.findById(API)).thenReturn(api);
        when(planService.findById(PLAN)).thenReturn(existingPlan);

        final Response response = envTarget().path(API).path("plans").path(PLAN).path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(apiService, times(1)).update(eq(API), any());
    }

    private ApiEntity getApi(DefinitionVersion version) {
        ApiEntity api = new ApiEntity();
        api.setId(API);
        api.setGraviteeDefinitionVersion(version.getLabel());

        if (DefinitionVersion.V2.equals(version)) {
            Plan plan1 = new Plan();
            plan1.setId(PLAN);

            Plan plan2 = new Plan();
            plan2.setId("plan-2");
            api.setPlans(Arrays.asList(plan1, plan2));
        }

        return api;
    }
}
