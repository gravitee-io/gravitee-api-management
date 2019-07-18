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

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.definition.Plan;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.gravitee.gateway.standalone.policy.ApiKeyPolicy;
import io.gravitee.gateway.standalone.policy.KeylessPolicy;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.gateway.standalone.policy.PolicyRegister;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractGatewayTest implements PolicyRegister, ApiLoaderInterceptor {

    protected final WireMockRule wireMockRule = getWiremockRule();

    protected Api api;

    @BeforeClass
    public static void init() {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    @Rule
    public final TestRule chain = RuleChain
            .outerRule(wireMockRule)
            .around(new ApiDeployer(this));

    @Override
    public void register(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        PolicyPlugin apiKey = PolicyBuilder.build("api-key", ApiKeyPolicy.class);
        policyPluginManager.register(apiKey);

        PolicyPlugin unsecuredPolicy = PolicyBuilder.build("key-less", KeylessPolicy.class);
        policyPluginManager.register(unsecuredPolicy);

        PolicyPlugin oauth2Policy = PolicyBuilder.build("oauth2", KeylessPolicy.class);
        policyPluginManager.register(oauth2Policy);

        PolicyPlugin jwtPolicy = PolicyBuilder.build("jwt", KeylessPolicy.class);
        policyPluginManager.register(jwtPolicy);
    }

    private String exchangePort(URI uri, int port) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), port, uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    String exchangePort(String sUri, int port) {
            return exchangePort(URI.create(sUri), port);
    }

    protected WireMockRule getWiremockRule() {
        return new WireMockRule(
                wireMockConfig()
                        .dynamicPort()
                        .extensions(new ResponseTemplateTransformer(true)));
    }

    @Override
    public void before(Api api) {
        this.api = api;

        // By default, add a keyless plan to the API
        Plan plan = new Plan();
        plan.setId("default_plan");
        plan.setName("Default plan");
        plan.setSecurity("key_less");

        api.setPlans(Collections.singletonList(plan));

        this.updateEndpoints();
    }

    protected void updateEndpoints() {
        // Define dynamically endpoint port
        for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
            endpoint.setTarget(exchangePort(endpoint.getTarget(), wireMockRule.port()));
        }
    }

    @Override
    public void after(Api api) {

    }
}
