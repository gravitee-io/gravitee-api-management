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
package io.gravitee.apim.integration.tests.http.post;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class HttpPostProxyV4IntegrationTest {

    @Nested
    @GatewayTest
    class Http2WithClearTextUpgradeTest extends HttpPostProxyIntegrationTest {

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            super.configureHttpClient(options);
            options.setProtocolVersion(HttpVersion.HTTP_2);
            options.setHttp2ClearTextUpgrade(false);
        }

        @Test
        @DeployApi({ "/apis/v4/http/post/api-endpoint-http2.json" })
        void should_proxy_request_body_to_http2_backend_when_using_http2_client(HttpClient httpClient) {
            makePostCall(httpClient);
        }

        @Test
        @DeployApi({ "/apis/v4/http/post/api-endpoint-http11.json" })
        void should_proxy_request_body_to_http11_backend_when_using_http2_client(HttpClient httpClient) {
            makePostCall(httpClient);
        }
    }

    @Nested
    @GatewayTest
    class Http2PriorKnowledgeTest extends HttpPostProxyIntegrationTest {

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            super.configureHttpClient(options);
            options.setProtocolVersion(HttpVersion.HTTP_2);
            options.setHttp2ClearTextUpgrade(true);
        }

        @Test
        @DeployApi({ "/apis/v4/http/post/api-endpoint-http2.json" })
        void should_proxy_request_body_to_http2_backend(HttpClient httpClient) {
            makePostCall(httpClient);
        }

        @Test
        @DeployApi({ "/apis/v4/http/post/api-endpoint-http11.json" })
        void should_proxy_request_body_to_http11_backend(HttpClient httpClient) {
            makePostCall(httpClient);
        }
    }

    @Nested
    @GatewayTest
    class Http11Test extends HttpPostProxyIntegrationTest {

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            super.configureHttpClient(options);
            options.setProtocolVersion(HttpVersion.HTTP_1_1);
        }

        @Test
        @DeployApi({ "/apis/v4/http/post/api-endpoint-http2.json" })
        void should_proxy_request_body_to_http2_backend(HttpClient httpClient) {
            makePostCall(httpClient);
        }

        @Test
        @DeployApi({ "/apis/v4/http/post/api-endpoint-http11.json" })
        void should_proxy_request_body_to_http11_backend(HttpClient httpClient) {
            makePostCall(httpClient);
        }
    }
}
