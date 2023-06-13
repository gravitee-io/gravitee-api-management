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

import static io.gravitee.rest.api.service.v4.GraviteeLicenseService.FEATURE_DEBUG_MODE;
import static jakarta.ws.rs.client.Entity.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.DebugApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.GraviteeLicenseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResourceDebugTest extends AbstractResourceTest {

    private static final String API = "my-api";

    private DebugApiEntity debugApiEntity;

    @Inject
    private GraviteeLicenseService graviteeLicenseService;

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Override
    @Before
    public void setUp() throws Exception {
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        debugApiEntity = new DebugApiEntity();
        debugApiEntity.setId(API);
        debugApiEntity.setName(API);
        debugApiEntity.setVersion("1");
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/test")));
        debugApiEntity.setProxy(proxy);
        debugApiEntity.setUpdatedAt(new Date());
    }

    @Test
    public void shouldReturnUnauthorizedWithoutLicense() {
        when(graviteeLicenseService.isFeatureEnabled(FEATURE_DEBUG_MODE)).thenReturn(false);
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
        final Response response = envTarget(API).path("_debug").request().post(json(debugApiEntity));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldReturnOKWithLicense() {
        when(graviteeLicenseService.isFeatureEnabled(FEATURE_DEBUG_MODE)).thenReturn(true);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        final Response response = envTarget(API).path("_debug").request().post(json(debugApiEntity));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }
}
