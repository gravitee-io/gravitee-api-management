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

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.DynamicRoutingPolicy;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor(value = "/io/gravitee/gateway/standalone/http/dynamic-routing.json")
public class DynamicRoutingGatewayTest extends AbstractWiremockGatewayTest {

    private Api api;

    @Test
    public void call_dynamic_api() throws Exception {
        wireMockRule.stubFor(get("/team").willReturn(ok()));

        HttpResponse response = Request.Get("http://localhost:8082/test/my_team").execute().returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    public void call_dynamic_api_unavailable() throws Exception {
        String initialTarget = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget();

        api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().setStatus(Endpoint.Status.DOWN);

        HttpResponse response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();

        assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, response.getStatusLine().getStatusCode());
        wireMockRule.verify(0, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Override
    public void registerPolicy(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.registerPolicy(policyPluginManager);

        PolicyPlugin dynamicRoutingPolicy = PolicyBuilder.build("dynamic-routing", DynamicRoutingPolicy.class);
        policyPluginManager.register(dynamicRoutingPolicy);
    }

    @Override
    public void before(Api api) {
        super.before(api);
        this.api = api;
    }
}
