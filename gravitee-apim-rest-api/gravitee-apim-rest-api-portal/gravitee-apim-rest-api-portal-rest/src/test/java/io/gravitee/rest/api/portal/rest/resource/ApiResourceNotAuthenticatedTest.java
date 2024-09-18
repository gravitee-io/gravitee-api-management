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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class ApiResourceNotAuthenticatedTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private ApiEntity mockApi;

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    @Before
    public void init() {
        resetAllMocks();

        mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API);

        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), mockApi)).thenReturn(true);

        doReturn(Arrays.asList(new PageEntity())).when(pageService).search(eq(GraviteeContext.getCurrentEnvironment()), any());

        PlanEntity plan1 = new PlanEntity();
        plan1.setId("A");
        plan1.setStatus(PlanStatus.PUBLISHED);

        PlanEntity plan2 = new PlanEntity();
        plan2.setId("B");
        plan2.setStatus(PlanStatus.PUBLISHED);

        PlanEntity plan3 = new PlanEntity();
        plan3.setId("C");
        plan3.setStatus(PlanStatus.CLOSED);

        doReturn(new HashSet<GenericPlanEntity>(Arrays.asList(plan1, plan2, plan3)))
            .when(planSearchService)
            .findByApi(GraviteeContext.getExecutionContext(), API);

        doReturn(new Api()).when(apiMapper).convert(eq(GraviteeContext.getExecutionContext()), any());
        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(new Plan()).when(planMapper).convert(any(), any());
    }

    @Test
    public void shouldGetApiWithPagesAndPlansIncluded() {
        // Useful for plans
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        // For pages
        doReturn(true).when(accessControlService).canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), any());
        callResourceAndCheckResult(1, 2);
    }

    @Test
    public void shouldGetApiWithNoElementsIncluded() {
        // Useful for plans
        doReturn(false).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        // For pages
        doReturn(false).when(accessControlService).canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), any());
        callResourceAndCheckResult(0, 0);
    }

    private void callResourceAndCheckResult(Integer expectedTotalPage, Integer expectedTotalPlan) {
        final Response response = target(API).queryParam("include", "pages", "plans").request().get();
        assertEquals(OK_200, response.getStatus());

        Api responseApi = response.readEntity(Api.class);
        assertNotNull(responseApi);

        List<Page> pages = responseApi.getPages();
        assertNotNull(pages);
        assertEquals(expectedTotalPage.intValue(), pages.size());

        List<Plan> plans = responseApi.getPlans();
        assertNotNull(plans);
        assertEquals(expectedTotalPlan.intValue(), plans.size());
    }
}
