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
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ApplicationApiKeyResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-app";
    private static final String APIKEY_ID = "my-apikey";

    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/apikeys/" + APIKEY_ID;
    }

    @Before
    public void initTest() {
        reset(apiKeyService);
        reset(applicationService);
    }

    @Test
    public void revoke_should_return_http_400_if_application_doesnt_use_shared_api_key() {
        mockExistingApplication(ApiKeyMode.EXCLUSIVE);

        ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.setId(APPLICATION_ID);

        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setApplication(applicationEntity);
        apiKeyEntity.setKey("my-api-key-value");

        when(apiKeyService.findById(APIKEY_ID)).thenReturn(apiKeyEntity);

        Response response = envTarget().request().delete();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        verifyNoMoreInteractions(apiKeyService);
    }

    @Test
    public void revoke_should_return_http_400_if_application_doesnt_match_api_key() {
        mockExistingApplication(ApiKeyMode.SHARED);

        ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.setId("another-app");

        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setApplication(applicationEntity);

        when(apiKeyService.findById(APIKEY_ID)).thenReturn(apiKeyEntity);

        Response response = envTarget().request().delete();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        verify(apiKeyService, times(1)).findById(APIKEY_ID);
        verifyNoMoreInteractions(apiKeyService);
    }

    @Test
    public void revoke_should_call_revoke_service_and_return_http_204() {
        mockExistingApplication(ApiKeyMode.SHARED);

        ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.setId(APPLICATION_ID);

        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setApplication(applicationEntity);

        when(apiKeyService.findById(APIKEY_ID)).thenReturn(apiKeyEntity);

        Response response = envTarget().request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        verify(apiKeyService, times(1)).findById(APIKEY_ID);
        verify(apiKeyService, times(1)).revoke(apiKeyEntity, true);
        verifyNoMoreInteractions(apiKeyService);
    }

    private void mockExistingApplication(ApiKeyMode apiKeyMode) {
        ApplicationEntity application = new ApplicationEntity();
        application.setApiKeyMode(apiKeyMode);
        when(applicationService.findById(GraviteeContext.getCurrentEnvironment(), APPLICATION_ID)).thenReturn(application);
    }
}
