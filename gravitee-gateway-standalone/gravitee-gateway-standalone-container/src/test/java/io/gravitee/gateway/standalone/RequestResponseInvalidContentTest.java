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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.standalone.junit.annotation.ApiConfiguration;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.gravitee.gateway.standalone.junit.rules.ApiPublisher;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.policy.ValidateRequestPolicy;
import io.gravitee.gateway.standalone.policy.ValidateResponsePolicy;
import io.gravitee.gateway.standalone.servlet.EchoServlet;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.policy.PolicyPluginManager;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor(
        value = "/io/gravitee/gateway/standalone/handle-stream-content-error.json",
        enhanceHttpPort = false)
@ApiConfiguration(
        servlet = EchoServlet.class,
        contextPath = "/echo")
public class RequestResponseInvalidContentTest extends AbstractGatewayTest {

    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public final TestRule chain = RuleChain
            .outerRule(new ApiPublisher())
            .outerRule(wireMockRule)
            .around(new ApiDeployer(this));

    @Test
    public void call_validate_request_content() throws Exception {
        stubFor(post(urlEqualTo("/echo/helloworld"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody("{\"key\": \"value\"}")));

        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request.Post("http://localhost:8082/echo/helloworld");
        request.bodyString("Invalid body", ContentType.TEXT_PLAIN);

        org.apache.http.client.fluent.Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_BAD_REQUEST, returnResponse.getStatusLine().getStatusCode());

        // Check that the stub has never been invoked by the gateway
        verify(0, postRequestedFor(urlEqualTo("/echo/helloworld")));
    }

    @Override
    public void before(Api api) {
        super.before(api);

        try {
            Endpoint edpt = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
            URL target = new URL(edpt.getTarget());
            URL newTarget = new URL(target.getProtocol(), target.getHost(), wireMockRule.port(), target.getFile());
            edpt.setTarget(newTarget.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void register(PolicyPluginManager policyPluginManager) {
        super.register(policyPluginManager);

        PolicyPlugin errorRequestStreamPolicy = PolicyBuilder.build("content-request-error", ValidateRequestPolicy.class);
        policyPluginManager.register(errorRequestStreamPolicy);

        PolicyPlugin errorResponseStreamPolicy = PolicyBuilder.build("content-response-error", ValidateResponsePolicy.class);
        policyPluginManager.register(errorResponseStreamPolicy);
    }
}
