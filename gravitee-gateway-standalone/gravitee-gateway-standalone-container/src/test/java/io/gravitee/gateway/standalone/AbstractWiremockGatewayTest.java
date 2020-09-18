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
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.net.URI;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractWiremockGatewayTest extends AbstractGatewayTest {

    protected final WireMockRule wireMockRule = getWiremockRule();

    @Rule
    public final TestRule chain = RuleChain
            .outerRule(wireMockRule)
            .around(new ApiDeployer(this));

    protected WireMockRule getWiremockRule() {
        return new WireMockRule(
                wireMockConfig()
                        .dynamicPort()
                        .extensions(new ResponseTemplateTransformer(true)));
    }

    @Override
    public void before(Api api) {
        super.before(api);

        this.updateEndpoints();
    }

    protected void updateEndpoints() {
        // Define dynamically endpoint port
        for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
            endpoint.setTarget(exchangePort(endpoint.getTarget(), wireMockRule.port()));
        }
    }

    public String exchangePort(URI uri, int port) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), port, uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public String exchangePort(String sUri, int port) {
        return exchangePort(URI.create(sUri), port);
    }
}
