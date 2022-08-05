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
package testcases;/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi("/apis/conditional-policy-flow.json")
@EnableForGatewayTestingExtensionTesting
public class RegisterTwiceSameApiMethodLevelTestCase extends AbstractGatewayTest {

    @Test
    @DeployApi("/apis/conditional-policy-flow.json")
    void fakeMethod() {
        // do nothing
        fail();
    }

    @Test
    void fakeMethod2() {
        // do nothing
        assertTrue(true);
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("header-policy1", PolicyBuilder.build("header-policy1", Header1Policy.class));
        policies.put("on-request-policy", PolicyBuilder.build("on-request-policy", OnRequestPolicy.class));
        policies.put("stream-policy", PolicyBuilder.build("stream-policy", Stream1Policy.class));
        policies.put("stream-policy2", PolicyBuilder.build("stream-policy2", Stream2Policy.class));
    }
}
