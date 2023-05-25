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
import static org.mockito.Mockito.*;

import fixtures.PlanFixtures;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Plan;
import io.gravitee.rest.api.management.v2.rest.model.PlanValidation;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiPlansResource_CreateTest extends ApiPlansResourceTest {

    @Autowired
    private PlanSearchService planSearchService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans";
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
        var body = response.readEntity(Plan.class);
        var createdPlan = body.getPlanV4();

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
}
