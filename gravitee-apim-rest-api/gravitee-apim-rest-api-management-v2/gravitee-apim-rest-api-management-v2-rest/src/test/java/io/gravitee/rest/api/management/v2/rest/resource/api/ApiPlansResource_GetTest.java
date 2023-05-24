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
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiPlansResource_GetTest extends AbstractResourceTest {

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

        final Response response = rootTarget().request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] can not be found.", error.getMessage());
    }

    @Test
    public void should_return_404_if_plan_associated_to_another_api() {
        final PlanEntity planEntity = PlanFixtures.aPlanV4().toBuilder().id(PLAN).apiId("ANOTHER-API").build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Plan [" + PLAN + "] can not be found.", error.getMessage());
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
        final PlanEntity planEntity = PlanFixtures.aPlanV4().toBuilder().id(PLAN).apiId(API).build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().get();
        assertEquals(OK_200, response.getStatus());

        var plan = response.readEntity(PlanV4.class);
        assertEquals(planEntity.getId(), plan.getId());
        assertEquals(planEntity.getApiId(), plan.getApiId());
        assertEquals(planEntity.getName(), plan.getName());
        assertEquals(planEntity.getDescription(), plan.getDescription());
        assertEquals(planEntity.getOrder(), (int) plan.getOrder());
        assertEquals(planEntity.getCharacteristics(), plan.getCharacteristics());
        assertEquals(planEntity.getCreatedAt().getTime(), plan.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(planEntity.getUpdatedAt().getTime(), plan.getUpdatedAt().toInstant().toEpochMilli());
        assertEquals(planEntity.getCommentMessage(), plan.getCommentMessage());
        assertEquals(planEntity.getCrossId(), plan.getCrossId());
        assertEquals(planEntity.getGeneralConditions(), plan.getGeneralConditions());
        assertEquals(planEntity.getTags(), new HashSet<>(plan.getTags()));
        assertEquals(planEntity.getStatus().name(), plan.getStatus().name());
        assertEquals(planEntity.getType().name(), plan.getType().name());
        assertEquals(planEntity.getExcludedGroups(), plan.getExcludedGroups());
        assertEquals(planEntity.getValidation().name(), plan.getValidation().name());
        assertEquals(planEntity.getSelectionRule(), plan.getSelectionRule());

        final PlanSecurity planEntitySecurity = planEntity.getSecurity();
        final io.gravitee.rest.api.management.v2.rest.model.PlanSecurity planSecurity = plan.getSecurity();

        assertEquals(PlanSecurityType.valueOfLabel(planEntitySecurity.getType()).name(), planSecurity.getType().getValue());
        assertNotNull(planSecurity.getConfiguration());
        assertFlowsV4Equals(planEntity.getFlows(), plan.getFlows());
    }

    @Test
    public void should_return_v2_plan() {
        final io.gravitee.rest.api.model.PlanEntity planEntity = PlanFixtures.aPlanV2().toBuilder().id(PLAN).api(API).build();

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(planEntity);

        final Response response = rootTarget().request().get();
        assertEquals(OK_200, response.getStatus());

        var plan = response.readEntity(PlanV2.class);
        assertEquals(planEntity.getId(), plan.getId());
        assertEquals(planEntity.getApiId(), plan.getApiId());
        assertEquals(planEntity.getName(), plan.getName());
        assertEquals(planEntity.getDescription(), plan.getDescription());
        assertEquals(planEntity.getOrder(), (int) plan.getOrder());
        assertEquals(planEntity.getCharacteristics(), plan.getCharacteristics());
        assertEquals(planEntity.getCreatedAt().getTime(), plan.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(planEntity.getUpdatedAt().getTime(), plan.getUpdatedAt().toInstant().toEpochMilli());
        assertEquals(planEntity.getCommentMessage(), plan.getCommentMessage());
        assertEquals(planEntity.getCrossId(), plan.getCrossId());
        assertEquals(planEntity.getGeneralConditions(), plan.getGeneralConditions());
        assertEquals(planEntity.getTags(), new HashSet<>(plan.getTags()));
        assertEquals(planEntity.getStatus().name(), plan.getStatus().name());
        assertEquals(planEntity.getType().name(), plan.getType().name());
        assertEquals(planEntity.getExcludedGroups(), plan.getExcludedGroups());
        assertEquals(planEntity.getValidation().name(), plan.getValidation().name());
        assertEquals(planEntity.getSelectionRule(), plan.getSelectionRule());
        assertEquals(planEntity.getPlanSecurity().getType(), plan.getSecurity().getType().name());

        assertFlowsV2Equals(planEntity.getFlows(), plan.getFlows());
    }

    private void assertFlowsV4Equals(List<Flow> planEntityFlows, List<FlowV4> planFlows) {
        assertEquals(planEntityFlows.size(), planFlows.size());

        for (int i = 0; i < planEntityFlows.size(); i++) {
            final Flow flow = planEntityFlows.get(i);
            final FlowV4 flowV4 = planFlows.get(i);
            assertEquals(flow.getName(), flowV4.getName());
            assertEquals(flow.getSelectors().size(), flowV4.getSelectors().size());
            assertStepsV4Equals(flow.getRequest(), flowV4.getRequest());
            assertStepsV4Equals(flow.getPublish(), flowV4.getPublish());
            assertStepsV4Equals(flow.getResponse(), flowV4.getResponse());
            assertStepsV4Equals(flow.getSubscribe(), flowV4.getSubscribe());
        }
    }

    private void assertFlowsV2Equals(List<io.gravitee.definition.model.flow.Flow> planEntityFlows, List<FlowV2> planFlows) {
        assertEquals(planEntityFlows.size(), planFlows.size());

        for (int i = 0; i < planEntityFlows.size(); i++) {
            final io.gravitee.definition.model.flow.Flow flow = planEntityFlows.get(i);
            final FlowV2 flowV2 = planFlows.get(i);
            assertEquals(flow.getName(), flowV2.getName());
            assertEquals(flow.getPath(), flowV2.getPathOperator().getPath());
            assertEquals(flow.getCondition(), flowV2.getCondition());
            assertStepsV2Equals(flow.getPre(), flowV2.getPre());
            assertStepsV2Equals(flow.getPost(), flowV2.getPost());
        }
    }

    private void assertStepsV4Equals(List<Step> steps, List<StepV4> stepsV4) {
        assertEquals(steps.size(), steps.size());

        for (int i = 0; i < steps.size(); i++) {
            final Step step = steps.get(i);
            final StepV4 stepV4 = stepsV4.get(i);
            assertEquals(step.getName(), stepV4.getName());
            assertEquals(step.getDescription(), stepV4.getDescription());
            assertEquals(step.getPolicy(), stepV4.getPolicy());
            assertEquals(step.getCondition(), stepV4.getCondition());
            assertEquals(step.getMessageCondition(), stepV4.getMessageCondition());
            assertEquals(step.getConfiguration(), stepV4.getConfiguration());
        }
    }

    private void assertStepsV2Equals(List<io.gravitee.definition.model.flow.Step> steps, List<StepV2> stepsV2) {
        assertEquals(steps.size(), steps.size());

        for (int i = 0; i < steps.size(); i++) {
            final io.gravitee.definition.model.flow.Step step = steps.get(i);
            final StepV2 stepV2 = stepsV2.get(i);
            assertEquals(step.getName(), stepV2.getName());
            assertEquals(step.getDescription(), stepV2.getDescription());
            assertEquals(step.getPolicy(), stepV2.getPolicy());
            assertEquals(step.getCondition(), stepV2.getCondition());
            assertEquals(step.getConfiguration(), stepV2.getConfiguration());
        }
    }
}
