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
import io.gravitee.gateway.standalone.policy.DynamicRoutingPolicy;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.servlet.TeamServlet;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.policy.PolicyPluginManager;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor(
        value = "/io/gravitee/gateway/standalone/dynamic-routing.json",
        enhanceHttpPort = true)
@ApiConfiguration(
        servlet = TeamServlet.class,
        contextPath = "/team")
public class DynamicRoutingGatewayTest extends AbstractGatewayTest {

    private Api api;

    @Test
    public void call_dynamic_api() throws Exception {
        String initialTarget = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget();
        String dynamicTarget = create(URI.create("http://localhost:8080/team"), new URL(initialTarget).getPort()).toString();

        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request.Get("http://localhost:8082/test/my_team");
        request.addHeader("X-Dynamic-Routing-URI", dynamicTarget);

        org.apache.http.client.fluent.Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void call_dynamic_api_unavailable() throws Exception {
        String initialTarget = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget();
        String dynamicTarget = create(URI.create("http://localhost:8080/team"), new URL(initialTarget).getPort()).toString();

        api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().setStatus(Endpoint.Status.DOWN);
        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request.Get("http://localhost:8082/test/my_team");
        request.addHeader("X-Dynamic-Routing-URI", dynamicTarget);

        org.apache.http.client.fluent.Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, returnResponse.getStatusLine().getStatusCode());
    }

    @Override
    public void register(PolicyPluginManager policyPluginManager) {
        super.register(policyPluginManager);

        PolicyPlugin dynamicRoutingPolicy = PolicyBuilder.build("dynamic-routing", DynamicRoutingPolicy.class);
        policyPluginManager.register(dynamicRoutingPolicy);
    }

    @Override
    public void before(Api api) {
        super.before(api);
        this.api = api;
    }

    private static URI create(URI uri, int port) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), port, uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            return uri;
        }
    }
}
