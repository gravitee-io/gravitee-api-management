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

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.definition.Plan;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.gravitee.gateway.standalone.junit.rules.ApiPublisher;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.policy.PolicyRegister;
import io.gravitee.gateway.standalone.policy.ApiKeyPolicy;
import io.gravitee.gateway.standalone.policy.KeylessPolicy;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.policy.PolicyPluginManager;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractGatewayTest implements PolicyRegister, ApiLoaderInterceptor {

    @Rule
    public final TestRule chain = RuleChain
            .outerRule(new ApiPublisher())
            .around(new ApiDeployer(this));

    @Override
    public void register(PolicyPluginManager policyPluginManager) {
        PolicyPlugin apiKey = PolicyBuilder.build("api-key", ApiKeyPolicy.class);
        policyPluginManager.register(apiKey);

        PolicyPlugin unsecuredPolicy = PolicyBuilder.build("key-less", KeylessPolicy.class);
        policyPluginManager.register(unsecuredPolicy);

        PolicyPlugin oauth2Policy = PolicyBuilder.build("oauth2", KeylessPolicy.class);
        policyPluginManager.register(oauth2Policy);

        PolicyPlugin jwtPolicy = PolicyBuilder.build("jwt", KeylessPolicy.class);
        policyPluginManager.register(jwtPolicy);
    }

    @Override
    public void before(Api api) {
        // By default, add a keyless plan to the API
        Plan plan = new Plan();
        plan.setId("default_plan");
        plan.setName("Default plan");
        plan.setSecurity("key_less");

        api.setPlans(Collections.singletonList(plan));
    }

    @Override
    public void after(Api api) {

    }
}
