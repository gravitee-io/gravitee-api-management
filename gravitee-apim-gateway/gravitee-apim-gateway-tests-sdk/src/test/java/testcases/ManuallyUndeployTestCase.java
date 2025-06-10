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
package testcases;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Header1Policy;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.OnRequestPolicy;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Stream1Policy;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Stream2Policy;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@EnableForGatewayTestingExtensionTesting
public class ManuallyUndeployTestCase extends AbstractGatewayTest {

    public static final String ON_REQUEST_POLICY = "on-request-policy";

    @Test
    @DisplayName("Should undeploy one api")
    @DeployApi({ "/apis/success-flow.json", "/apis/teams.json" })
    void shouldUndeployOneApi() {
        assertThat(deployedApis).hasSize(2);
        undeploy("api-test");
        assertThat(deployedApis).hasSize(1);
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("header-policy1", PolicyBuilder.build("header-policy1", Header1Policy.class));
        policies.put(ON_REQUEST_POLICY, PolicyBuilder.build(ON_REQUEST_POLICY, OnRequestPolicy.class));
        policies.put("stream-policy", PolicyBuilder.build("stream-policy", Stream1Policy.class));
        policies.put("stream-policy2", PolicyBuilder.build("stream-policy2", Stream2Policy.class));
    }
}
