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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Data;
import io.gravitee.rest.api.portal.rest.model.DatasResponse;
import io.gravitee.rest.api.portal.rest.model.Error;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String FORBIDDEN_API = "my-forbidden-api";

    protected String contextPath() {
        return "apis/";
    }

    private ApiEntity mockApi;
    private ApiEntity forbiddenApi;

    @Before
    public void init() throws IOException {
        resetAllMocks();
        
        mockApi = new ApiEntity();
        mockApi.setId(API);
        mockApi.setVisibility(Visibility.PUBLIC);
        doReturn(mockApi).when(apiService).findById(API);
        
        PlanEntity plan1 = new PlanEntity();
        plan1.setId("A");
        plan1.setSecurity(PlanSecurityType.API_KEY);
        plan1.setValidation(PlanValidationType.AUTO);
        plan1.setStatus(PlanStatus.PUBLISHED);

        
        PlanEntity plan2 = new PlanEntity();
        plan2.setId("B");
        plan2.setSecurity(PlanSecurityType.KEY_LESS);
        plan2.setValidation(PlanValidationType.MANUAL);
        plan2.setStatus(PlanStatus.PUBLISHED);

        PlanEntity planWrongStatus = new PlanEntity();
        planWrongStatus.setId("C");
        planWrongStatus.setSecurity(PlanSecurityType.KEY_LESS);
        planWrongStatus.setValidation(PlanValidationType.MANUAL);
        planWrongStatus.setStatus(PlanStatus.STAGING);
        
        doReturn(new HashSet<PlanEntity>(Arrays.asList(plan1, plan2, planWrongStatus))).when(planService).findByApi(API);
        
        forbiddenApi = new ApiEntity();
        forbiddenApi.setVisibility(Visibility.PRIVATE);
        doReturn(forbiddenApi).when(apiService).findById(FORBIDDEN_API);

    }

    
    @Test
    public void shouldGetForbiddenAccess() {
        final Response response = target(FORBIDDEN_API).path("plans").request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
        
        final Error error = response.readEntity(Error.class);
        
        assertNotNull(error);
        assertEquals("403", error.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.ForbiddenAccessException", error.getTitle());
        assertEquals("You do not have sufficient rights to access this resource", error.getDetail());
    }
    
    @Test
    public void shouldGetApiPlans() {
        final Response response = target(API).path("plans").request().get();

        assertEquals(OK_200, response.getStatus());

        final DatasResponse plansResponse = response.readEntity(DatasResponse.class);
        List<Data> plans = plansResponse.getData();
        assertNotNull(plans);
        assertEquals(2, plans.size());
        
    }
}