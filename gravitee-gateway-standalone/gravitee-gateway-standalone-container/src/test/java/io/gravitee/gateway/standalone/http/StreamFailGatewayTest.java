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
import static org.junit.Assert.assertTrue;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.policy.StreamFailer2Policy;
import io.gravitee.gateway.standalone.policy.StreamFailerPolicy;
import io.gravitee.gateway.standalone.utils.StringUtils;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor(value = "/io/gravitee/gateway/standalone/http/stream-fail.json")
public class StreamFailGatewayTest extends AbstractWiremockGatewayTest {

    private static final String BODY_CONTENT = "Content to transform:";

    @Test
    public void shouldNotProcessPolicyAfterStreamFail() throws Exception {
        wireMockRule.stubFor(post("/api").willReturn(serverError()));

        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request
            .Post("http://localhost:8082/api")
            .addHeader(HttpHeaders.ACCEPT, "application/json");

        request.bodyString(BODY_CONTENT + " {#request.id}", ContentType.TEXT_PLAIN);

        HttpResponse response = execute(request).returnResponse();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());

        assertEquals("{\"message\":\"stream-fail\",\"http_status_code\":500}", responseContent);
        //    wireMockRule.verify(0, postRequestedFor(urlPathEqualTo("/api")));
    }

    @Test
    public void shouldNotProcessPolicyAfterStreamFail_plainText() throws Exception {
        wireMockRule.stubFor(post("/api").willReturn(serverError()));

        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request.Post("http://localhost:8082/api");

        request.bodyString(BODY_CONTENT + " {#request.id}", ContentType.TEXT_PLAIN);

        HttpResponse response = execute(request).returnResponse();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());

        assertEquals("stream-fail", responseContent);
        //    wireMockRule.verify(0, postRequestedFor(urlPathEqualTo("/api")));
    }

    @Override
    public void registerPolicy(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.registerPolicy(policyPluginManager);

        PolicyPlugin streamFailPolicy = PolicyBuilder.build("stream-fail", StreamFailerPolicy.class);
        policyPluginManager.register(streamFailPolicy);

        PolicyPlugin streamFail2Policy = PolicyBuilder.build("stream-fail-2", StreamFailer2Policy.class);
        policyPluginManager.register(streamFail2Policy);
    }
}
