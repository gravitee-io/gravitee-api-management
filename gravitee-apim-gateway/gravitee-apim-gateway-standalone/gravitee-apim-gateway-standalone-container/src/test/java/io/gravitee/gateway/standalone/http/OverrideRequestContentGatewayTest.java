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
package io.gravitee.gateway.standalone.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.OverrideRequestContentPolicy;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.utils.StringUtils;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor(value = "/io/gravitee/gateway/standalone/http/override-request-content.json")
public class OverrideRequestContentGatewayTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldOverrideRequestContent() throws Exception {
        wireMockRule.stubFor(post("/api").willReturn(ok("{{request.body}}").withTransformers("response-template")));

        HttpResponse response = execute(
            Request.Post("http://localhost:8082/api").bodyString("Request content overriden by policy", ContentType.TEXT_PLAIN)
        ).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        assertEquals(OverrideRequestContentPolicy.STREAM_POLICY_CONTENT, responseContent);

        wireMockRule.verify(
            1,
            postRequestedFor(urlPathEqualTo("/api")).withRequestBody(equalTo(OverrideRequestContentPolicy.STREAM_POLICY_CONTENT))
        );
    }

    @Override
    public void registerPolicy(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.registerPolicy(policyPluginManager);

        PolicyPlugin rewriteRequestStreamPolicy = PolicyBuilder.build("override-request-content", OverrideRequestContentPolicy.class);
        policyPluginManager.register(rewriteRequestStreamPolicy);
    }
}
