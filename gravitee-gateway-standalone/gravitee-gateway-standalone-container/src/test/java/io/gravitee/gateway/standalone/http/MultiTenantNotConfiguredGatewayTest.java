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
package io.gravitee.gateway.standalone.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * In case of a gateway non-configured for a tenant, all endpoints are selected and are not filtered according to their
 * tenants.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/tenant-unavailable.json")
public class MultiTenantNotConfiguredGatewayTest extends AbstractWiremockGatewayTest {

    private static final String MULTI_TENANT_SYSTEM_PROPERTY = "gravitee.tenant";

    static {
        System.setProperty(MULTI_TENANT_SYSTEM_PROPERTY, "");
    }

    @Test
    public void call_undeployed_api() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        HttpResponse response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/my_team")));
    }
}
