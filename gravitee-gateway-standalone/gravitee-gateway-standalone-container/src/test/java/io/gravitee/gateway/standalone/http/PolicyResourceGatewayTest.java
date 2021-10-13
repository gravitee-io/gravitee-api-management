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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.policy.ResourcePolicy;
import io.gravitee.gateway.standalone.resource.DummyResource;
import io.gravitee.gateway.standalone.resource.ResourceBuilder;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/policy-resource.json")
public class PolicyResourceGatewayTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldReturnNotFound_unknownPolicy() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));
        HttpResponse response = execute(Request.Get("http://localhost:8082/api/my_team")).returnResponse();

        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());
        assertEquals("Dummy Resource", response.getFirstHeader("X-Resource").getValue());
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Override
    public void registerPolicy(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.registerPolicy(policyPluginManager);

        policyPluginManager.register(PolicyBuilder.build("resource", ResourcePolicy.class));
    }

    @Override
    public void registerResource(ConfigurablePluginManager<ResourcePlugin> resourcePluginManager) {
        super.registerResource(resourcePluginManager);

        resourcePluginManager.register(ResourceBuilder.build("my-resource", DummyResource.class));
    }
}
