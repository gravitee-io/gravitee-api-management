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
package io.gravitee.apim.integration.tests.http.pathtraversal;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.definition.model.ExecutionMode;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Settles which execution modes {@code http.pathHandling} actually reaches, for a V2 definition.
 *
 * <p>The dispatcher branches on the reactor, not on the definition version: a V2 definition running
 * in V4 emulation is served by a reactor that implements {@code ApiReactor} exactly like a V4 one,
 * and therefore goes down the same path. The legacy V3 engine does not, and the dispatcher falls
 * back to the raw path for it on purpose, because its request wrapper cannot be rewritten.
 *
 * <p>The API here is deliberately alone on the gateway: a traversal out of its context path lands
 * on no listener, so the status alone says whether the path was resolved before routing.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathHandlingV2IntegrationTest {

    private static final String TRAVERSAL = "/alpha/api/../../elsewhere/resource";
    private static final String REGULAR = "/alpha/api/echo";

    abstract static class PathHandlingV2Test extends AbstractGatewayTest {

        /**
         * Left to the nested classes rather than fixed here. When this base pinned the mode to
         * {@code NORMALIZE}, no V2 definition was ever run under {@code REJECT} — and whether that
         * mode answers before any API is selected, on every definition version, is precisely the
         * question this file exists to settle.
         */
        @Override
        public abstract void configureGateway(GatewayConfigurationBuilder configurationBuilder);

        protected void assertStatus(final HttpClient httpClient, final String rawPath, final int expectedStatus)
            throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.GET, rawPath)
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(expectedStatus);
                    return true;
                })
                .assertNoErrors();
        }

        protected String singleUpstreamRequestUrl() {
            final List<LoggedRequest> upstreamRequests = wiremock.findAll(getRequestedFor(anyUrl()));
            assertThat(upstreamRequests).hasSize(1);
            return upstreamRequests.get(0).getUrl();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/http/pathtraversal/api-v2-alpha.json")
    class With_v4_emulation extends PathHandlingV2Test {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "NORMALIZE");
        }

        @Test
        void should_serve_a_regular_path(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, REGULAR, 200);

            assertThat(singleUpstreamRequestUrl()).isEqualTo("/alpha/api/echo");
        }

        @Test
        void should_resolve_the_path_before_routing(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // Resolved, the request points outside every context path on this gateway, so it is
            // answered as not found instead of being carried to the backend.
            assertStatus(httpClient, TRAVERSAL, 404);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi("/apis/http/pathtraversal/api-v2-alpha.json")
    class With_the_legacy_v3_engine extends PathHandlingV2Test {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "NORMALIZE");
        }

        @Test
        void should_serve_a_regular_path(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, REGULAR, 200);
        }

        @Test
        void should_resolve_the_path_before_routing(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // The setting is applied before the acceptor is resolved, which is upstream of any
            // engine, so the legacy one is covered for routing exactly like the reactive one.
            assertStatus(httpClient, TRAVERSAL, 404);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }

        @Test
        void should_send_upstream_the_path_it_resolved(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // A traversal that lands back inside this API's own context path. Routing is right in
            // both engines; what this pins is the upstream URI, which the legacy engine derives
            // from its own request wrapper rather than from the value seeded by the dispatcher.
            assertStatus(httpClient, "/alpha/api/../../alpha/api/echo", 200);

            assertThat(singleUpstreamRequestUrl()).isEqualTo("/alpha/api/echo");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/http/pathtraversal/api-v2-alpha.json")
    class With_reject_under_v4_emulation extends PathHandlingV2Test {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "REJECT");
        }

        @Test
        void should_refuse_before_any_api_is_selected(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, TRAVERSAL, 400);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }

        @Test
        void should_serve_a_regular_path(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, REGULAR, 200);
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi("/apis/http/pathtraversal/api-v2-alpha.json")
    class With_reject_on_the_legacy_v3_engine extends PathHandlingV2Test {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "REJECT");
        }

        @Test
        void should_refuse_before_any_api_is_selected(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // The refusal happens upstream of the acceptor, so it cannot depend on the engine. That
            // is an argument until a test makes it an observation.
            assertStatus(httpClient, TRAVERSAL, 400);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }

        @Test
        void should_serve_a_regular_path(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, REGULAR, 200);
        }
    }
}
