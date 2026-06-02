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
package io.gravitee.gateway.standalone;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.TemplateEngine;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractWiremockGatewayTest extends AbstractGatewayTest {

    protected Executor executor;

    // WireMock must start before the API is deployed (the API endpoints are wired to the mock port),
    // hence @Order: WireMock (1) runs its beforeEach before the deployer (2), and tears down after it.
    @Order(1)
    @RegisterExtension
    protected final WireMockExtension wireMockRule = getWiremockRule();

    @Order(2)
    @RegisterExtension
    protected final ApiDeployer apiDeployer = new ApiDeployer(this);

    @BeforeEach
    public void initExecutor() throws Exception {
        // Create a dedicated HttpClient for each test with no pooling to avoid side effects.
        final CloseableHttpClient client = HttpClients.custom().build();

        this.executor = Executor.newInstance(client);
    }

    protected WireMockExtension getWiremockRule() {
        FileSource fs = new ClasspathFileSource("src/test/resources/");
        return WireMockExtension.newInstance()
            .options(
                wireMockConfig()
                    .dynamicPort()
                    .extensions(new ResponseTemplateTransformer(TemplateEngine.defaultTemplateEngine(), true, fs, Collections.emptyList()))
            )
            // Point the static WireMock DSL (stubFor(...), verify(...)) at this instance, as the JUnit 4
            // WireMockRule used to. Without this, static-import tests target the default localhost:8080.
            .configureStaticDsl(true)
            .build();
    }

    @Override
    public void before(Api api) {
        super.before(api);

        this.updateEndpoints();
    }

    protected void updateEndpoints() {
        // Define dynamically endpoint port
        for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
            endpoint.setTarget(exchangePort(endpoint.getTarget(), wireMockRule.getPort()));
        }
    }

    public String exchangePort(URL url, int port) {
        try {
            return new URI(
                url.getProtocol(),
                url.getUserInfo(),
                url.getHost(),
                port,
                url.getPath(),
                url.getQuery(),
                url.getRef()
            ).toString();
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

    protected boolean isV2EmulateV4Engine() {
        return api.getExecutionMode() == ExecutionMode.V4_EMULATION_ENGINE;
    }
}
