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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.Plan;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class ApiResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(AuthenticationFilter.class);
    }

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {
        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return null;
                }
                @Override
                public boolean isUserInRole(String string) {
                    return false;
                }
                @Override
                public boolean isSecure() { return false; }

                @Override
                public String getAuthenticationScheme() { return "BASIC"; }
            });
        }
    }

    private static final String API = "my-api";

    private ApiEntity mockApi;

    @Before
    public void init() {
        resetAllMocks();

        mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiService).findById(API);

        when(accessControlService.canAccessApiFromPortal(API)).thenReturn(true);
        when(accessControlService.canAccessApiFromPortal(mockApi)).thenReturn(true);

        doReturn(Arrays.asList(new PageEntity())).when(pageService).search(any());

        PlanEntity plan1 = new PlanEntity();
        plan1.setId("A");
        plan1.setStatus(PlanStatus.PUBLISHED);

        PlanEntity plan2 = new PlanEntity();
        plan2.setId("B");
        plan2.setStatus(PlanStatus.PUBLISHED);

        PlanEntity plan3 = new PlanEntity();
        plan3.setId("C");
        plan3.setStatus(PlanStatus.CLOSED);

        doReturn(new HashSet<PlanEntity>(Arrays.asList(plan1, plan2, plan3))).when(planService).findByApi(API);

        doReturn(new Api()).when(apiMapper).convert(any());
        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(new Plan()).when(planMapper).convert(any());

    }

    @Test
    public void shouldGetApiWithPagesAndPlansIncluded() {
        // Useful for plans
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        // For pages
        doReturn(true).when(accessControlService).canAccessPageFromPortal(any());
        callResourceAndCheckResult(1, 2);
    }

    @Test
    public void shouldGetApiWithNoElementsIncluded() {
        // Useful for plans
        doReturn(false).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        // For pages
        doReturn(false).when(accessControlService).canAccessPageFromPortal(any());
        callResourceAndCheckResult(0, 0);
    }

    private void callResourceAndCheckResult(Integer expectedTotalPage, Integer expectedTotalPlan) {
        final Response response = target(API).queryParam("include", "pages","plans") .request().get();
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
