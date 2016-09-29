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

import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.gravitee.gateway.standalone.junit.rules.ApiPublisher;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.policy.PolicyRegister;
import io.gravitee.gateway.standalone.policy.apikey.DummyApiKeyPolicy;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.policy.PolicyPluginManager;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractGatewayTest implements PolicyRegister {

    @Rule
    public final TestRule chain = RuleChain
            .outerRule(new ApiPublisher())
            .around(new ApiDeployer(this));

    public void registerPlugin(PolicyPluginManager policyPluginManager) {
        PolicyPlugin apiKey = PolicyBuilder.register("api-key", DummyApiKeyPolicy.class);
        policyPluginManager.register(apiKey);
    }
}
