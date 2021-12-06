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

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.junit.Assert.assertEquals;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.flow.policy.Header1Policy;
import io.gravitee.gateway.standalone.flow.policy.OnRequestPolicy;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * This test validates that the chain fail if a condition on a policy is invalid.
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/flow/invalid-conditional-policy-flow.json")
public class InvalidConditionalPolicyTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldNotRunPoliciesIfConditionIsInvalid() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        final HttpResponse response = execute(
            Request.Get("http://localhost:8082/test/my_team").addHeader("conditionHeader", "condition-ok")
        )
            .returnResponse();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatusLine().getStatusCode());
        wireMockRule.verify(0, anyRequestedFor(anyUrl()));
    }

    @Override
    public void register(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.register(policyPluginManager);

        PolicyPlugin myPolicyHeader1 = PolicyBuilder.build("header-policy1", Header1Policy.class);
        PolicyPlugin onRequestPolicy = PolicyBuilder.build("on-request-policy", OnRequestPolicy.class);
        policyPluginManager.register(myPolicyHeader1);
        policyPluginManager.register(onRequestPolicy);
    }
}
