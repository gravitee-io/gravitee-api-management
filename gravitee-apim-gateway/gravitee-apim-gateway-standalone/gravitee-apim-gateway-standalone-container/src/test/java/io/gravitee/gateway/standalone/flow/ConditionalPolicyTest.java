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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.flow.policy.Header1Policy;
import io.gravitee.gateway.standalone.flow.policy.Header2Policy;
import io.gravitee.gateway.standalone.flow.policy.MyPolicy;
import io.gravitee.gateway.standalone.flow.policy.OnRequestPolicy;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * This test validates that conditional policies are run only if condition evaluate to true.
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/flow/conditional-policy-flow.json")
public class ConditionalPolicyTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldRunPoliciesWithPassingCondition() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        final HttpResponse response = execute(
            Request.Get("http://localhost:8082/test/my_team").addHeader("conditionHeader", "condition-ok")
        )
            .returnResponse();
        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());
        assertFalse(response.containsHeader("X-Gravitee-Policy"));
        wireMockRule.verify(
            getRequestedFor(urlPathEqualTo("/team/my_team"))
                .withHeader("X-Gravitee-Policy", equalTo("request-header1"))
                .withHeader("on-request-policy", equalTo("invoked"))
        );
    }

    @Test
    public void shouldRunPoliciesConditionNotMet() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        final HttpResponse response = execute(
            Request.Get("http://localhost:8082/test/my_team").addHeader("conditionHeader", "condition-no")
        )
            .returnResponse();
        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());
        assertFalse(response.containsHeader("X-Gravitee-Policy"));
        wireMockRule.verify(
            getRequestedFor(urlPathEqualTo("/team/my_team"))
                .withoutHeader("X-Gravitee-Policy")
                .withHeader("on-request-policy", equalTo("invoked"))
        );
    }

    @Override
    public void registerPolicy(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.registerPolicy(policyPluginManager);

        PolicyPlugin myPolicyHeader1 = PolicyBuilder.build("header-policy1", Header1Policy.class);
        PolicyPlugin onRequestPolicy = PolicyBuilder.build("on-request-policy", OnRequestPolicy.class);
        policyPluginManager.register(myPolicyHeader1);
        policyPluginManager.register(onRequestPolicy);
    }
}
