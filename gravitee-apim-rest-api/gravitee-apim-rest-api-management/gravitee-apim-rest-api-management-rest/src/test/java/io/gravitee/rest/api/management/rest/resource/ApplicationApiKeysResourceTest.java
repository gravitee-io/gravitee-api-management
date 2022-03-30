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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ApplicationApiKeysResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-app";
    private static final String APIKEY_ID = "my-apikey";

    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/apikeys";
    }

    @Before
    public void initTest() {
        reset(apiKeyService);
        reset(applicationService);
    }

    @Test
    public void get_should_list_apikeys_by_application() {
        List<ApiKeyEntity> apiKeys = List.of(new ApiKeyEntity(), new ApiKeyEntity());
        apiKeys.get(0).setId("apikey-1");
        apiKeys.get(1).setId("apikey-2");

        when(apiKeyService.findByApplication(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(apiKeys);

        Response response = envTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        List<ApiKeyEntity> responseEntities = (List<ApiKeyEntity>) (response.readEntity(List.class));
        assertEquals(2, responseEntities.size());
        assertEquals("apikey-1", ((Map<String, String>) (responseEntities.get(0))).get("id"));
        assertEquals("apikey-2", ((Map<String, String>) (responseEntities.get(1))).get("id"));
    }

    @Test
    public void post_renew_should_return_http_400_when_not_shared_api_key_mode() {
        mockExistingApplication(ApiKeyMode.EXCLUSIVE);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId("my-api-key");
        when(apiKeyService.renew(eq(GraviteeContext.getExecutionContext()), any(ApplicationEntity.class))).thenReturn(apiKey);

        Response response = envTarget("/_renew").request().post(null);

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void post_renew_should_renew_when_shared_api_key_mode() {
        ApplicationEntity applicationEntity = mockExistingApplication(ApiKeyMode.SHARED);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId("my-api-key");
        when(apiKeyService.renew(eq(GraviteeContext.getExecutionContext()), any(ApplicationEntity.class))).thenReturn(apiKey);

        Response response = envTarget("/_renew").request().post(null);

        verify(apiKeyService, times(1)).renew(GraviteeContext.getExecutionContext(), applicationEntity);
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
    }

    private ApplicationEntity mockExistingApplication(ApiKeyMode apiKeyMode) {
        ApplicationEntity application = new ApplicationEntity();
        application.setApiKeyMode(apiKeyMode);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        return application;
    }
}
