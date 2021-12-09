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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractWiremockGatewayTest extends AbstractGatewayTest {

    protected Executor executor;
    protected final WireMockRule wireMockRule = getWiremockRule();

    @Before
    public void initExecutor() throws Exception {
        // Create a dedicated HttpClient for each test with no pooling to avoid side effects.
        final CloseableHttpClient client = HttpClients.custom().build();

        this.executor = Executor.newInstance(client);
    }

    @Rule
    public final TestRule chain = RuleChain.outerRule(wireMockRule).around(new ApiDeployer(this));

    protected WireMockRule getWiremockRule() {
        return new WireMockRule(wireMockConfig().dynamicPort().extensions(new ResponseTemplateTransformer(true)));
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

    public String exchangePort(URL url, int port) {
        try {
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), port, url.getPath(), url.getQuery(), url.getRef())
                .toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public String exchangePort(String sUri, int port) {
        try {
            return exchangePort(new URL(sUri), port);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    protected Response execute(Request request) throws Exception {
        return executor.execute(request);
    }
}
