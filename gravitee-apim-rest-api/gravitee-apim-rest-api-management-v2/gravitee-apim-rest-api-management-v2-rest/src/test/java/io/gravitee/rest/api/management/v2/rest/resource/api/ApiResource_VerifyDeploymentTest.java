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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.management.v2.rest.model.VerifyApiDeploymentResponse;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.exceptions.InvalidLicenseException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ApiResource_VerifyDeploymentTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/deployments/_verify";
    }

    @Test
    public void shouldVerifyValidApi() {
        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var body = response.readEntity(VerifyApiDeploymentResponse.class);
        assertTrue(body.getOk());
        assertNull(body.getReason());
    }

    @Test
    public void shouldHandleInvalidLicense() {
        doThrow(new InvalidLicenseException("Bad license")).when(apiLicenseService).checkLicense(any(), anyString());

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var body = response.readEntity(VerifyApiDeploymentResponse.class);
        assertFalse(body.getOk());
        assertEquals("Bad license", body.getReason());
    }

    @Test
    public void shouldHandleForbiddenFeature() {
        doThrow(new ForbiddenFeatureException("http-get")).when(apiLicenseService).checkLicense(any(), anyString());

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var body = response.readEntity(VerifyApiDeploymentResponse.class);
        assertFalse(body.getOk());
        assertEquals("Feature 'http-get' is not available with your license tier", body.getReason());
    }

    @Test
    public void shouldHandleMultipleForbiddenFeatures() {
        doThrow(new ForbiddenFeatureException(List.of("http-get", "http-post"))).when(apiLicenseService).checkLicense(any(), anyString());

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var body = response.readEntity(VerifyApiDeploymentResponse.class);
        assertFalse(body.getOk());
        assertEquals("Features [http-get, http-post] are not available with your license tier", body.getReason());
    }

    @Test
    public void shouldHandleInvalidApiDefinition() {
        doThrow(new TechnicalManagementException("not good")).when(apiLicenseService).checkLicense(any(), anyString());

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var body = response.readEntity(VerifyApiDeploymentResponse.class);
        assertFalse(body.getOk());
        assertEquals("not good", body.getReason());
    }
}
