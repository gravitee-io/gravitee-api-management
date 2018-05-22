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

import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.standalone.junit.annotation.ApiConfiguration;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.servlet.TeamServlet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/teams.json")
@ApiConfiguration(
        servlet = TeamServlet.class,
        contextPath = "/team"
)
public class ServiceUnavailableTest extends AbstractGatewayTest {

    private Api api;

    @Test
    public void call_available_api() throws Exception {
        Request request = Request.Get("http://localhost:8082/test/my_team");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void call_unavailable_api() throws Exception {
        // Set the endpoint as down
        api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().setStatus(Endpoint.Status.DOWN);

        Request request = Request.Get("http://localhost:8082/test/my_team");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void call_availableAndUnavailable_api() throws Exception {
        // Set the endpoint as down
        api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().setStatus(Endpoint.Status.DOWN);

        Request request = Request.Get("http://localhost:8082/test/my_team");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, returnResponse.getStatusLine().getStatusCode());

        // Set the endpoint as up
        api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().setStatus(Endpoint.Status.UP);

        Request request2 = Request.Get("http://localhost:8082/test/my_team");
        Response response2 = request2.execute();
        HttpResponse returnResponse2 = response2.returnResponse();

        assertEquals(HttpStatus.SC_OK, returnResponse2.getStatusLine().getStatusCode());
    }

    @Override
    public void before(Api api) {
        super.before(api);
        this.api = api;
    }
}
