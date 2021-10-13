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
import io.gravitee.gateway.standalone.flow.policy.Stream1Policy;
import io.gravitee.gateway.standalone.flow.policy.Stream2Policy;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.utils.StringUtils;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/flow/simple-request-stream-flow.json")
public class StreamPluginOrderFlowTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldRunFlows() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        final HttpResponse response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();

        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/my_team")).withRequestBody(equalTo("OnRequestContent2Policy")));

        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        assertEquals("OnResponseContent2Policy", responseContent);
    }

    @Override
    public void registerPolicy(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.registerPolicy(policyPluginManager);

        policyPluginManager.register(PolicyBuilder.build("stream-policy1", Stream1Policy.class));
        policyPluginManager.register(PolicyBuilder.build("stream-policy2", Stream2Policy.class));
    }
}
