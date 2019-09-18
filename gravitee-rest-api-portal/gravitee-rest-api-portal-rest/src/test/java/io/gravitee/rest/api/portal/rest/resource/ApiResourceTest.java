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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.PortalPermission;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String FORBIDDEN_API = "my-forbidden-api";
    private static final String UNKNOWN_API = "unknown";

    @Override
    protected String contextPath() {
        return "apis/";
    }

    private ApiEntity mockApi;
    private ApiEntity forbiddenApi;
    private InlinePictureEntity mockImage;
    private byte[] apiLogoContent;

    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();
        
        
        mockApi = new ApiEntity();
        mockApi.setId(API);
        mockApi.setVisibility(Visibility.PUBLIC);
        doReturn(mockApi).when(apiService).findById(API);
        doThrow(new ApiNotFoundException(UNKNOWN_API)).when(apiService).findById(UNKNOWN_API);

        mockImage = new InlinePictureEntity();
        apiLogoContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(apiLogoContent);
        mockImage.setType("image/svg");
        doReturn(mockImage).when(apiService).getPicture(API);

        PageEntity pagePublished = new PageEntity();
        pagePublished.setPublished(true);
        pagePublished.setType("SWAGGER");
        pagePublished.setLastModificationDate(Date.from(Instant.now()));
        pagePublished.setContent("some page content");
        doReturn(Arrays.asList(pagePublished)).when(pageService).search(any());
        
        
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

        PlanEntity plan3 = new PlanEntity();
        plan3.setId("C");
        plan3.setSecurity(PlanSecurityType.KEY_LESS);
        plan3.setValidation(PlanValidationType.MANUAL);
        plan3.setStatus(PlanStatus.CLOSED);

        doReturn(new HashSet<PlanEntity>(Arrays.asList(plan1, plan2, plan3))).when(planService).findByApi(API);

        
        forbiddenApi = new ApiEntity();
        forbiddenApi.setVisibility(Visibility.PRIVATE);
        doReturn(forbiddenApi).when(apiService).findById(FORBIDDEN_API);

        Api api = new Api();
        api.setId(API);
        doReturn(api).when(apiMapper).convert(any());
        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(new Plan()).when(planMapper).convert(any(), eq(USER_NAME));
        doReturn(new Rating()).when(ratingMapper).convert(any());


    }

    @Test
    public void shouldGetApiwithoutIncluded() {
        final Response response = target(API).request().get();

        assertEquals(OK_200, response.getStatus());

        final Api responseApi = response.readEntity(Api.class);
        assertNotNull(responseApi);
        
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        Mockito.verify(apiMapper, Mockito.times(1)).computeApiLinks(ac.capture());
        
        String expectedBasePath = target(API).getUriBuilder().build().toString();
        List<String> bastPathList = ac.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath));
        
    }
    
    @Test
    public void shouldGetApiWithIncluded() {
        final Response response = 
                target(API)
                .queryParam("include", "pages","plans")
                .request()
                .get();

        assertEquals(OK_200, response.getStatus());

        final Api responseApi = response.readEntity(Api.class);
        assertNotNull(responseApi);
        
        List<Page> pages = responseApi.getPages();
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertNull(pages.get(0).getContent());
        
        final List<Plan> plans = responseApi.getPlans();
        assertNotNull(plans);
        assertEquals(2, plans.size());
        
    }

    @Test
    public void shouldNotGetApi() {
        final Response response = target(UNKNOWN_API).request().get();

        assertEquals(NOT_FOUND_404, response.getStatus());
        
        final Error error = response.readEntity(Error.class);
        
        assertNotNull(error);
        assertEquals("404", error.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.ApiNotFoundException", error.getTitle());
        assertEquals("Api ["+UNKNOWN_API+"] can not be found.", error.getDetail());
    }
    
    @Test
    public void shouldGetForbiddenAccess() {
        // /apis/{apiId}
        final Response response = target(FORBIDDEN_API).request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());
        
        final Error error = response.readEntity(Error.class);
        assertNotNull(error);
        assertEquals("403", error.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.ForbiddenAccessException", error.getTitle());
        assertEquals("You do not have sufficient rights to access this resource", error.getDetail());
        
        
        // /apis/{apiId}/picture
        final Response anotherResponse = target(FORBIDDEN_API).path("picture").request().get();
        assertEquals(FORBIDDEN_403, anotherResponse.getStatus());
        
        final Error anotherError = anotherResponse.readEntity(Error.class);
        assertNotNull(anotherError);
        assertEquals("403", anotherError.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.ForbiddenAccessException", anotherError.getTitle());
        assertEquals("You do not have sufficient rights to access this resource", anotherError.getDetail());
    }
    
    @Test
    public void shouldGetApiPicture() throws IOException {
        final Response response = target(API).path("picture").request().get();
        assertEquals(OK_200, response.getStatus());
    }
    
}
