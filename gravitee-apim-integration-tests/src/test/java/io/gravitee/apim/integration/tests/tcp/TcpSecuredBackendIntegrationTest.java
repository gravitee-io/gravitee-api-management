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
package io.gravitee.apim.integration.tests.tcp;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.utils.ResourceUtils;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.tcp.proxy.TcpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.tcp.proxy.TcpProxyEntrypointConnectorFactory;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TcpSecuredBackendIntegrationTest {

    @Nested
    @GatewayTest
    class SSLJksTruststore extends TcpTestPreparer {

        private final int backendPort = getAvailablePort();

        @Override
        protected void configureWireMock(WireMockConfiguration configuration) {
            configuration.httpsPort(backendPort).keystorePath(ResourceUtils.toPath("certs/keystore01.jks")).keystorePassword("password");
        }

        @Override
        public void configureApi(ReactableApi<?> reactableApi, Class<?> definitionClass) {
            if (!isV4Api(definitionClass)) {
                throw new AssertionError("TCP api should only be v4 api");
            }
            final Api apiDefinition = (Api) reactableApi.getDefinition();
            final Endpoint tcpEndpoint = apiDefinition.getEndpointGroups().getFirst().getEndpoints().getFirst();

            tcpEndpoint.setConfiguration(tcpEndpoint.getConfiguration().replace("\"secured\":false", "\"secured\":true"));

            tcpEndpoint.setSharedConfigurationOverride(
                """
                          {
                            "ssl": {
                                        "trustAll": false,
                                        "trustStore": {
                                          "type": "JKS",
                                          "path": "<<path>>",
                                          "password": "password"
                                        }
                                      }
                                      }
                          """.replace(
                        "<<path>>",
                        ResourceUtils.toPath("certs/truststore01.jks")
                    )
            );
            updateEndpointsPort(apiDefinition, backendPort);
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api.json" })
        void should_call_secured_backend_via_tcp_proxy(HttpClient httpClient) {
            wiremock.stubFor(get("/test").willReturn(ok("backend response")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(buffer -> {
                    assertThat(buffer).hasToString("backend response");
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/test")));
        }
    }

    @Nested
    @GatewayTest
    class ClientAuthenticationPEMInline extends TcpTestPreparer {

        private final int backendPort = getAvailablePort();

        @Override
        protected void configureWireMock(WireMockConfiguration configuration) {
            configuration
                .httpsPort(backendPort)
                .needClientAuth(true)
                .keystorePath(ResourceUtils.toPath("certs/keystore01.jks"))
                .keystorePassword("password")
                .trustStorePath(ResourceUtils.toPath("certs/truststore01.jks"))
                .trustStorePassword("password");
        }

        @Override
        public void configureApi(ReactableApi<?> reactableApi, Class<?> definitionClass) {
            if (!isV4Api(definitionClass)) {
                throw new AssertionError("TCP api should only be v4 api");
            }
            final Api apiDefinition = (Api) reactableApi.getDefinition();
            final Endpoint tcpEndpoint = apiDefinition.getEndpointGroups().getFirst().getEndpoints().getFirst();

            tcpEndpoint.setConfiguration(tcpEndpoint.getConfiguration().replace("\"secured\":false", "\"secured\":true"));

            tcpEndpoint.setSharedConfigurationOverride(
                """
                          {
                            "ssl": {
                                        "trustAll": false,
                                        "keyStore": {
                                          "type": "PEM",
                                            "certContent": "-----BEGIN CERTIFICATE-----\\nMIICxzCCAa+gAwIBAgIEPvlKwjANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDEwls\\nb2NhbGhvc3QwHhcNMTgxMDA5MTU0MTAyWhcNMTkxMDA0MTU0MTAyWjAUMRIwEAYD\\nVQQDEwlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCM\\nOMvaM1XZzR1HPp127syCvuxnljnxsMLCvfM+8QsdyeuYVURei3z502zxWVTwNbxU\\nbTxOkgIuNYRTyypEMpYajSEv0sMt4d7KBchE0eeQlcMDQ2J6kzfVjHyxLMMu8Jsc\\nBQ5KGvF1vW3Qp16w6C6JebF21Y0LumL9cMEToN6OrDKx9BrUkbTXHfSf+5rvkrnG\\nfIPMulNJS2Onl4zmT0bHs8a/zSIGmcwNIG243LmXTFu67TKbknJahICrRz0uZ8h1\\nHMFbe7yY12s0d0xGJDJcPIBcdWHhB8z6iduTxCYZ2vY3EdFcC2RMO99zJPlWeDGY\\n4lTM8TRFEAvEj4a21CltAgMBAAGjITAfMB0GA1UdDgQWBBQmmZF9umT5DGh4RgYX\\nBQhzb6EkQTANBgkqhkiG9w0BAQsFAAOCAQEAW1QaHW4iYjUtjQik+nWD3Xktbm50\\ns9PeAYSCp9an757dvzfO/vwJZE+1+grmsS0l/jxh8L0qsdjM5Qt4VmjK5CbikE2v\\ne4Vt4o40tQOz8A7fNVVp5S33njgNbp1UMhnrFsHVZ6Aa8HHxisjliluVK1/YPl80\\nKRs57GL4SyvELzmWhh7egndxdGYR9nbAbg1RQ+kJClqSS0BL5oQ4Xn4AGmU5839/\\nZ1+N5qgNq2/BYOi6FsltL91US0FOLNxDBYqjwShGOJ1V6Lvh27YmSHViscph6GeZ\\nkZ2xybRANymp0DSVER5J+D2RuJNtzp/zl//BJ3b19tpVpDTQ1ndzcSGPLg==\\n-----END CERTIFICATE-----",
                                            "keyContent": "-----BEGIN PRIVATE KEY-----\\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCMOMvaM1XZzR1H\\nPp127syCvuxnljnxsMLCvfM+8QsdyeuYVURei3z502zxWVTwNbxUbTxOkgIuNYRT\\nyypEMpYajSEv0sMt4d7KBchE0eeQlcMDQ2J6kzfVjHyxLMMu8JscBQ5KGvF1vW3Q\\np16w6C6JebF21Y0LumL9cMEToN6OrDKx9BrUkbTXHfSf+5rvkrnGfIPMulNJS2On\\nl4zmT0bHs8a/zSIGmcwNIG243LmXTFu67TKbknJahICrRz0uZ8h1HMFbe7yY12s0\\nd0xGJDJcPIBcdWHhB8z6iduTxCYZ2vY3EdFcC2RMO99zJPlWeDGY4lTM8TRFEAvE\\nj4a21CltAgMBAAECggEADu4VNnx0zaX7UhSmq30tpVYy0ay7KrLJafbTqYX8ywUu\\n4p9hkjeD7Q3H8cKzOoheLxcabrs5JDZqiol9TJmeReF1ASSNx5rfH9+RvVIkN87a\\nXsST/b0jGsfElxDPD3Zq7YbUSKupvgGXaboIaQmvus+MR7zhMbh8xcN1q2NbjxE6\\nCLXV+uu8kVPLsUbGFGK53vK7+MHnEKJIbjHR+LRNMxZRzoX7h57UO6uoXwWes2Kr\\nopMzIJdv2ZRvXS62NNCTK9BoPb4OreXSoEJZkXS26ispOLLnM36D4UpNeDl6hITN\\noCSHKiRGFqY5QOir/MP8uqNmGEtprMDmUQt/K5Z8OQKBgQDVvEi7SmHFJLNSsY2l\\nvvxlwIa9rr0d/TFtTXcGMUdLYg7rQtaZ3e0geJv4wU58KahmpgYqpX4s0/Gc4L37\\nyG1kCtPUjHvxxauLRr0OcXz35pTqlGhVGyls9onCxM/nDpoiYDvueuse69rVVGDE\\nTKg2pxqS0Rxh7FARKY1RpJprjwKBgQCn8xsrRSc5QGF/L7b87MK1sf+lQ7Updubt\\nv/XRFq5lhHp888MrIsqcqsDqRChoY2Qoi0vuNv52pgpha6lXkxL5Djd1ypX457KR\\ntaJmx7qoolYW7INU4mvcryyIiW9ELiFrGb8XrwhXwmUBWOGGEDJkgYOj6W7Co38T\\n2RbkBvBNQwKBgGTniv7A0v+bn/0+Tb0eOVJgXjxWrnnl+tu7YqHNyfbQyHJRD7d8\\nimJ2DkyWFlOP5yzu3KJtlu/a74o8n/SqXtqIMhF6cVlnFOGf98lF0tXGSi+k+MyV\\nEi2bBtaoy+4tep8YB7NC3JWwi5ODTlveRNvocCc4CcpBIlu33jvZFf4JAoGATJ0R\\nn8OECRHdZ++UQfyfNdNlEza3xZp/7aTLtf3qwFSWq7lnJp5QXvdl2XgOFtCAOB6T\\nHK/plKZZxece8Nweo45grlMj5s+LHf0FgG1MMPEc5IgvwOEo4xrl7cMEBs4kYH72\\nNQ+bdq0u9lZdSpLI6iBKtNMfu5pptdwqHQstQ5ECgYBQ+XWJHuKLadcMAoAdG+gW\\n5KxlSlin7PqgmzobXggp2aUGVLHs4pvIGA+7Sf/kAPfJ6bimv4dIODqEe8TZnmWp\\n4ZG0jYjFXVnbwbh+hU5EF03ntO9WYqL6rYU3zujz/ZuKBEByIFowTX9uaVKEgML6\\n5oHAF4Sb7zxa2jSEehGQ5Q==\\n-----END PRIVATE KEY-----"
                                        },
                                        "trustStore": {
                                         "type": "PEM",
                                         "content": "-----BEGIN CERTIFICATE-----\\nMIICxzCCAa+gAwIBAgIEPvlKwjANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDEwls\\nb2NhbGhvc3QwHhcNMTgxMDA5MTU0MTAyWhcNMTkxMDA0MTU0MTAyWjAUMRIwEAYD\\nVQQDEwlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCM\\nOMvaM1XZzR1HPp127syCvuxnljnxsMLCvfM+8QsdyeuYVURei3z502zxWVTwNbxU\\nbTxOkgIuNYRTyypEMpYajSEv0sMt4d7KBchE0eeQlcMDQ2J6kzfVjHyxLMMu8Jsc\\nBQ5KGvF1vW3Qp16w6C6JebF21Y0LumL9cMEToN6OrDKx9BrUkbTXHfSf+5rvkrnG\\nfIPMulNJS2Onl4zmT0bHs8a/zSIGmcwNIG243LmXTFu67TKbknJahICrRz0uZ8h1\\nHMFbe7yY12s0d0xGJDJcPIBcdWHhB8z6iduTxCYZ2vY3EdFcC2RMO99zJPlWeDGY\\n4lTM8TRFEAvEj4a21CltAgMBAAGjITAfMB0GA1UdDgQWBBQmmZF9umT5DGh4RgYX\\nBQhzb6EkQTANBgkqhkiG9w0BAQsFAAOCAQEAW1QaHW4iYjUtjQik+nWD3Xktbm50\\ns9PeAYSCp9an757dvzfO/vwJZE+1+grmsS0l/jxh8L0qsdjM5Qt4VmjK5CbikE2v\\ne4Vt4o40tQOz8A7fNVVp5S33njgNbp1UMhnrFsHVZ6Aa8HHxisjliluVK1/YPl80\\nKRs57GL4SyvELzmWhh7egndxdGYR9nbAbg1RQ+kJClqSS0BL5oQ4Xn4AGmU5839/\\nZ1+N5qgNq2/BYOi6FsltL91US0FOLNxDBYqjwShGOJ1V6Lvh27YmSHViscph6GeZ\\nkZ2xybRANymp0DSVER5J+D2RuJNtzp/zl//BJ3b19tpVpDTQ1ndzcSGPLg==\\n-----END CERTIFICATE-----"
                                        }
                                      }
                                      }
                          """
            );
            updateEndpointsPort(apiDefinition, backendPort);
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api.json" })
        void should_call_secured_backend_via_tcp_proxy(HttpClient httpClient) {
            wiremock.stubFor(get("/test").willReturn(ok("backend response")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(buffer -> {
                    assertThat(buffer).hasToString("backend response");
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/test")));
        }
    }

    static class TcpTestPreparer extends AbstractGatewayTest {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("tcp-proxy", EntrypointBuilder.build("tcp-proxy", TcpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("tcp-proxy", EndpointBuilder.build("tcp-proxy", TcpProxyEndpointConnectorFactory.class));
        }

        @Override
        protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
            super.configureGateway(gatewayConfigurationBuilder);
            // enables the TCP proxy
            gatewayConfigurationBuilder.configureTcpGateway(tcpPort());
            gatewayConfigurationBuilder.set("tcp.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED);
        }

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            options
                .setDefaultHost("localhost")
                .setDefaultPort(tcpPort())
                .setForceSni(true)
                .setSsl(true)
                .setVerifyHost(false)
                .setTrustAll(true);
        }
    }
}
