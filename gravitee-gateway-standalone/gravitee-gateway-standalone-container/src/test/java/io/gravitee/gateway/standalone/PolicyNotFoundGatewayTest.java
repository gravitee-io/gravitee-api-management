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
package io.gravitee.gateway.standalone;

import io.gravitee.gateway.standalone.junit.annotation.ApiConfiguration;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.servlet.TeamServlet;
import io.gravitee.plugin.policy.PolicyPluginManager;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/teams.json")
@ApiConfiguration(
        servlet = TeamServlet.class,
        contextPath = "/team"
)
public class PolicyNotFoundGatewayTest extends AbstractGatewayTest {
    
    @Test
    public void call_get_started_api() throws Exception {
        Request request = Request.Get("http://localhost:8082/test/my_team");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        // The gateway returns a NOT_FOUND (404) because the API can't be deployed correctly.
        // The API is not correctly deployed because a required policy can not be found
        assertEquals(HttpStatus.SC_NOT_FOUND, returnResponse.getStatusLine().getStatusCode());
    }

    @Override
    public void registerPlugin(PolicyPluginManager policyPluginManager) {
        // Do not install any policy in registry
    }
}
