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
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.flow.policy.RemoveQueryParameterPolicy;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/flow/expression-language-condition-flow.json")
public class ExpressionLanguageConditionFlowTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldRunFlows_getMethod() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        final HttpResponse response = execute(Request.Get("http://localhost:8082/test/my_team?my-param=value")).returnResponse();

        if (isJupiterMode()) {
            // With Jupiter, flow condition is now evaluated once for the whole flow (EL based on a param suppressed during request policy chain and re-evaluated at response phase).
            assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());
        } else {
            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatusLine().getStatusCode());
        }

        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Override
    public void registerPolicy(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.registerPolicy(policyPluginManager);

        PolicyPlugin myPolicy = PolicyBuilder.build("remove-param-policy", RemoveQueryParameterPolicy.class);
        policyPluginManager.register(myPolicy);
    }

    private boolean isJupiterMode() {
        final Configuration configuration = context.getBean(Configuration.class);
        return (
            configuration.getProperty("api.jupiterMode.enabled", Boolean.class, false) && api.getExecutionMode() == ExecutionMode.JUPITER
        );
    }
}
