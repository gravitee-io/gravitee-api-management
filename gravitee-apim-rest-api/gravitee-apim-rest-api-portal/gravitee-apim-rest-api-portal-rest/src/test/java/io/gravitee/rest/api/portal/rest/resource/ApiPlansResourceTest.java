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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.PlansResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    protected PlanEntity plan1;
    protected PlanEntity plan2;
    protected PlanEntity planWrongStatus;

    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() throws IOException {
        resetAllMocks();

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setVisibility(Visibility.PUBLIC);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);

        plan1 = new PlanEntity();
        plan1.setId("A");
        plan1.setSecurity(PlanSecurityType.API_KEY);
        plan1.setValidation(PlanValidationType.AUTO);
        plan1.setStatus(PlanStatus.PUBLISHED);

        plan2 = new PlanEntity();
        plan2.setId("B");
        plan2.setSecurity(PlanSecurityType.KEY_LESS);
        plan2.setValidation(PlanValidationType.MANUAL);
        plan2.setStatus(PlanStatus.PUBLISHED);

        planWrongStatus = new PlanEntity();
        planWrongStatus.setId("C");
        planWrongStatus.setSecurity(PlanSecurityType.KEY_LESS);
        planWrongStatus.setValidation(PlanValidationType.MANUAL);
        planWrongStatus.setStatus(PlanStatus.STAGING);

        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API)).thenReturn(Set.of(plan1, plan2, planWrongStatus));

        when(planMapper.convert(any(GenericPlanEntity.class), any())).thenCallRealMethod();
    }

    @Test
    public void shouldGetApiPlansWithPublicAPI() {
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());

        final Response response = target(API).path("plans").request().get();
        assertEquals(OK_200, response.getStatus());

        PlansResponse plansResponse = response.readEntity(PlansResponse.class);

        List<Plan> plans = plansResponse.getData();
        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    @Test
    public void shouldGetApiPlansWithPublicAPI_WithGCU() {
        PageEntity page = new PageEntity();
        final String PAGE_ID = "PAGE_ID_CGU";
        page.setId(PAGE_ID);
        page.setPublished(true);
        page.setContent("Some CGU");
        page.setType(PageType.MARKDOWN.name());
        doReturn(page).when(pageService).findById(any(), any());

        plan1.setGeneralConditions(PAGE_ID);
        plan2.setGeneralConditions(PAGE_ID);

        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());

        final Response response = target(API).path("plans").request().get();
        assertEquals(OK_200, response.getStatus());

        PlansResponse plansResponse = response.readEntity(PlansResponse.class);

        List<Plan> plans = plansResponse.getData();
        assertNotNull(plans);
        assertEquals(2, plans.size());
        for (Plan plan : plans) {
            assertEquals(PAGE_ID, plan.getGeneralConditions());
        }
    }

    @Test
    public void shouldGetApiPlansWithPrivateAPIAndReadPermission() {
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(true).when(permissionService).hasPermission(any(), any(), any());

        final Response response = target(API).path("plans").request().get();
        assertEquals(OK_200, response.getStatus());

        PlansResponse plansResponse = response.readEntity(PlansResponse.class);

        List<Plan> plans = plansResponse.getData();
        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    @Test
    public void shouldGetNoApiPlan() {
        doReturn(false).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());

        final Response response = target(API).path("plans").request().get();
        assertEquals(OK_200, response.getStatus());

        PlansResponse plansResponse = response.readEntity(PlansResponse.class);

        List<Plan> plans = plansResponse.getData();
        assertNotNull(plans);
        assertEquals(0, plans.size());
    }

    @Test
    public void should_have_NotFound_with_private_API_and_no_READ_permission() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        mockApi.setVisibility(Visibility.PRIVATE);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(mockApi);

        final Response response = target(API).path("plans").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + API + "] cannot be found.", error.getMessage());
    }
}
