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
package io.gravitee.gateway.standalone.flow;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.flow.policy.MyPolicy;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/flow/best-match-flow.json")
public class BestMatchFlowTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldRunBestMatchFlows_getMethod() throws Exception {
        wireMockRule.stubFor(get("/team/teams/team1").willReturn(ok()));

        final HttpResponse response = execute(Request.Get("http://localhost:8082/test/teams/team1")).returnResponse();
        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());
        assertEquals("1", response.getFirstHeader("my-counter").getValue());
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/teams/team1")).withHeader("my-counter", equalTo("1")));
    }

    @Override
    public void register(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.register(policyPluginManager);

        PolicyPlugin myPolicy = PolicyBuilder.build("my-policy", MyPolicy.class);
        MyPolicy.clear();
        policyPluginManager.register(myPolicy);
    }
}
