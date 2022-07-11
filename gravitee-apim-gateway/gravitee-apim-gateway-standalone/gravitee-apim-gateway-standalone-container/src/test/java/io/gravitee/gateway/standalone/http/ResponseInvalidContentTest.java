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
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.policy.ValidateResponsePolicy;
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
@ApiDescriptor(value = "/io/gravitee/gateway/standalone/http/handle-content-response-error.json")
public class ResponseInvalidContentTest extends AbstractWiremockGatewayTest {

    @Test
    public void call_validate_response_content() throws Exception {
        wireMockRule.stubFor(post("/echo/helloworld").willReturn(ok()));

        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request.Post("http://localhost:8082/echo/helloworld");
        request.bodyString("My request content", ContentType.TEXT_PLAIN);

        HttpResponse response = execute(request).returnResponse();

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        // Check that the stub has never been invoked by the gateway
        wireMockRule.verify(1, postRequestedFor(urlPathEqualTo("/echo/helloworld")));
    }

    @Override
    public void registerPolicy(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.registerPolicy(policyPluginManager);

        PolicyPlugin errorResponseStreamPolicy = PolicyBuilder.build("content-response-error", ValidateResponsePolicy.class);
        policyPluginManager.register(errorResponseStreamPolicy);
    }
}
