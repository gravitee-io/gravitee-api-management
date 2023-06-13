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

import static io.gravitee.rest.api.service.v4.GraviteeLicenseService.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.service.v4.GraviteeLicenseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResourceAuditTest extends AbstractResourceTest {

    private static final String API = "my-api";

    @Inject
    private GraviteeLicenseService gravityLicenseService;

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Test
    public void getAuditShouldReturnUnauthorizedWithoutLicense() {
        when(gravityLicenseService.isFeatureEnabled(FEATURE_AUDIT_TRAIL)).thenReturn(false);
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
        final Response response = envTarget(API).path("audit").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void getAuditShouldReturnOKdWithLicense() {
        when(gravityLicenseService.isFeatureEnabled(FEATURE_AUDIT_TRAIL)).thenReturn(true);
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
        when(auditService.search(any(), any())).thenReturn(mock(MetadataPage.class));
        final Response response = envTarget(API).path("audit").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }
}
