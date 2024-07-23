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
package io.gravitee.apim.integration.tests.http;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.gateway.tests.sdk.utils.HttpClientUtils.extractHeaders;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeploySharedPolicyGroups;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.ThrowingPolicy;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.policy.DefaultSharedPolicyGroupPolicyChainFactory;
import io.gravitee.gateway.handlers.sharedpolicygroup.policy.SharedPolicyGroupPolicy;
import io.gravitee.gateway.handlers.sharedpolicygroup.registry.SharedPolicyGroupRegistry;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.interrupt.InterruptPolicy;
import io.gravitee.policy.interrupt.configuration.InterruptPolicyConfiguration;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupV4IntegrationTest {

    @Nested
    @DeploySharedPolicyGroups(
        { "/sharedpolicygroups/spg-two-headers-on-request.json", "/sharedpolicygroups/spg-two-headers-on-response.json" }
    )
    class GeneralCases extends TestPreparer {

        private ListAppender<ILoggingEvent> listAppender;

        @BeforeAll
        void prepareLogInterception() {
            // Add an appender to Logger in a BeforeAll method as this log is emitted during before the BeforeEach hook
            final Logger logger = (Logger) LoggerFactory.getLogger(DefaultSharedPolicyGroupPolicyChainFactory.class);
            listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);
        }

        @Override
        public void configurePlaceHolderVariables(Map<String, String> variables) {
            variables.put("SHARED_POLICY_GROUP_ID", "spg-nested");
        }

        @Test
        @DeployApi({ "/apis/v4/http/sharedpolicygroup/api-shared-policy-group.json" })
        void should_use_shared_policy_group_on_both_request_and_response(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Inside-0", "Header Inside 0"),
                            Map.entry("X-Response-Header-Inside-1", "Header Inside 1"),
                            Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                        );
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(
                1,
                getRequestedFor(urlPathEqualTo("/endpoint"))
                    .withHeader("X-Request-Header-Outside-0", equalTo("Header Outside 0"))
                    .withHeader("X-Request-Header-Inside-0", equalTo("Header Inside 0"))
                    .withHeader("X-Request-Header-Inside-1", equalTo("Header Inside 1"))
                    .withHeader("X-Request-Header-Outside-1", equalTo("Header Outside 1"))
            );
        }

        @Test
        @DeployApi({ "/apis/v4/http/sharedpolicygroup/api-shared-policy-group-placeholder.json" })
        @DeploySharedPolicyGroups("/sharedpolicygroups/spg-nested.json")
        void should_not_use_nested_shared_policy_group(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                        )
                        .doesNotContainKey("X-Response-Header-Inside-0")
                        .doesNotContainKey("X-Response-Header-Inside-1");
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));

            final List<ILoggingEvent> list = listAppender.list.stream().filter(log -> log.getLevel().equals(Level.WARN)).toList();
            assertThat(list)
                .hasSize(1)
                .element(0)
                .extracting(ILoggingEvent::getFormattedMessage)
                .isEqualTo(
                    "Nested Shared Policy Group is not supported. The Shared Policy Group my-shared-policy-group-policy will be ignored"
                );
        }

        @Test
        @DeployApi({ "/apis/v4/http/sharedpolicygroup/api-shared-policy-group-placeholder.json" })
        void should_not_fail_if_shared_policy_group_does_not_exist(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            final Logger logger = (Logger) LoggerFactory.getLogger(SharedPolicyGroupPolicy.class);
            ListAppender<ILoggingEvent> policyLogs = new ListAppender<>();
            policyLogs.start();
            logger.addAppender(policyLogs);

            final SharedPolicyGroupRegistry sharedPolicyGroupRegistry = getBean(SharedPolicyGroupRegistry.class);
            assertThat(sharedPolicyGroupRegistry.get("spg-unknown", "DEFAULT")).isNull();

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                        )
                        .doesNotContainKey("X-Response-Header-Inside-0")
                        .doesNotContainKey("X-Response-Header-Inside-1");
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));

            final List<ILoggingEvent> list = policyLogs.list.stream().filter(log -> log.getLevel().equals(Level.WARN)).toList();
            assertThat(list)
                .hasSize(1)
                .element(0)
                .extracting(ILoggingEvent::getFormattedMessage)
                .isEqualTo("No Shared Policy Group found for shared-policy-group-policy on RESPONSE phase");
        }
    }

    @Nested
    @DeploySharedPolicyGroups(
        { "/sharedpolicygroups/spg-two-headers-on-response.json", "/sharedpolicygroups/spg-conditional-two-headers-on-response.json" }
    )
    class Conditions extends TestPreparer {

        @Test
        @DeployApi({ "/apis/v4/http/sharedpolicygroup/api-shared-policy-group-conditional.json" })
        void should_not_execute_conditional_shared_policy_group(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(req -> {
                    req.putHeader("execute-shared-policy-group", "false");
                    return req.rxSend();
                })
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                        )
                        .doesNotContainKey("X-Response-Header-Inside-0")
                        .doesNotContainKey("X-Response-Header-Inside-1");
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }

        @Test
        @DeployApi({ "/apis/v4/http/sharedpolicygroup/api-shared-policy-group-conditional.json" })
        void should_not_execute_conditional_policy_of_a_shared_policy_group(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(req -> {
                    req.putHeader("execute-shared-policy-group", "yes");
                    req.putHeader("execute-shared-policy-group-first-step", "no");
                    return req.rxSend();
                })
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Inside-1", "Header Inside 1"),
                            Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                        )
                        .doesNotContainKey("X-Request-Header-Inside-0");
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @Nested
    @DeployApi({ "/apis/v4/http/sharedpolicygroup/api-shared-policy-group-placeholder.json" })
    class Errors extends TestPreparer {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.putIfAbsent(
                "policy-interrupt",
                PolicyBuilder.build("policy-interrupt", InterruptPolicy.class, InterruptPolicyConfiguration.class)
            );
            policies.putIfAbsent("throwing-policy", PolicyBuilder.build("throwing-policy", ThrowingPolicy.class));
        }

        @Override
        public void configurePlaceHolderVariables(Map<String, String> variables) {
            variables.put("SHARED_POLICY_GROUP_ID", "spg-header-on-response-error-case");
        }

        @Test
        @DeploySharedPolicyGroups("/sharedpolicygroups/spg-header-on-response-then-interrupt.json")
        void should_properly_interrupt_flow_from_interruption_of_a_policy_of_the_shared_policy_group(HttpClient httpClient)
            throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(500);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Inside-0", "Header Inside 0")
                        )
                        .doesNotContainKey("X-Response-Header-Inside-1")
                        .doesNotContainKey("X-Response-Header-Outside-1");
                    return response.body();
                })
                .test()
                .await()
                .assertComplete()
                .assertValue(body -> {
                    assertThat(body).hasToString("interruption from inner policy");
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }

        @Test
        @DeploySharedPolicyGroups("/sharedpolicygroups/spg-header-on-response-then-throw.json")
        void should_properly_interrupt_flow_from_exception_of_a_policy_of_the_shared_policy_group(HttpClient httpClient)
            throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(500);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Inside-0", "Header Inside 0")
                        )
                        .doesNotContainKey("X-Response-Header-Inside-1")
                        .doesNotContainKey("X-Response-Header-Outside-1");
                    return response.body();
                })
                .test()
                .await()
                .assertComplete()
                .assertValue(body -> {
                    assertThat(body).hasToString("Internal Server Error");
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @Nested
    class DeploymentLifecycle extends TestPreparer {

        public static final String SHARED_POLICY_GROUP_ID = "spg-header-on-response";

        @Override
        public void configurePlaceHolderVariables(Map<String, String> variables) {
            variables.put("SHARED_POLICY_GROUP_ID", SHARED_POLICY_GROUP_ID);
        }

        @Test
        @DeployApi({ "/apis/v4/http/sharedpolicygroup/api-shared-policy-group-placeholder.json" })
        void should_deploy_and_undeploy_shared_policy_group(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            // First, call the API without having the Shared Policy Group deployed, 'Inside' header is not added
            callApi(
                httpClient,
                List.of(
                    Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                    Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                ),
                List.of("X-Response-Header-Inside-0")
            );

            // Then, deploy the shared policy group and call the API, 'Inside' header is present in the response
            deploySharedPolicyGroup(fakeReactableSharedPolicyGroup());
            callApi(
                httpClient,
                List.of(
                    Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                    Map.entry("X-Response-Header-Inside-0", "Header Inside 0"),
                    Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                ),
                List.of()
            );

            // Finally, undeploy the shared policy group and verify 'Inside' header is not added
            undeploySharedPolicyGroup(SHARED_POLICY_GROUP_ID, "DEFAULT");
            callApi(
                httpClient,
                List.of(
                    Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                    Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                ),
                List.of("X-Response-Header-Inside-0")
            );
        }

        private static void callApi(
            HttpClient httpClient,
            List<Map.Entry<String, String>> expectedHeaders,
            List<String> notContainedHeaders
        ) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    final Map<String, String> headers = extractHeaders(response);
                    assertThat(headers).contains(expectedHeaders.toArray(Map.Entry[]::new));
                    notContainedHeaders.forEach(unexpectedHeader -> assertThat(headers).doesNotContainKey(unexpectedHeader));
                    return true;
                })
                .assertNoErrors();
        }

        private static ReactableSharedPolicyGroup fakeReactableSharedPolicyGroup() {
            return ReactableSharedPolicyGroup
                .builder()
                .id(SHARED_POLICY_GROUP_ID)
                .environmentId("DEFAULT")
                .definition(
                    SharedPolicyGroup
                        .builder()
                        .environmentId("DEFAULT")
                        .phase(SharedPolicyGroup.Phase.RESPONSE)
                        .id(SHARED_POLICY_GROUP_ID)
                        .policies(
                            List.of(
                                Step
                                    .builder()
                                    .enabled(true)
                                    .policy("transform-headers")
                                    .configuration(
                                        """
                                               {
                                                           "scope": "RESPONSE",
                                                           "addHeaders": [
                                                             {
                                                               "name": "X-Response-Header-Inside-0",
                                                               "value": "Header Inside 0"
                                                             }
                                                           ]
                                                         }
                                               """
                                    )
                                    .build()
                            )
                        )
                        .build()
                )
                .build();
        }
    }

    @Nested
    class MultiEnvironment extends TestPreparer {

        @Test
        @DeployApi(
            {
                "/apis/v4/http/sharedpolicygroup/api-shared-policy-group-env-dev.json",
                "/apis/v4/http/sharedpolicygroup/api-shared-policy-group-env-prod.json",
            }
        )
        @DeploySharedPolicyGroups({ "/sharedpolicygroups/spg-env-dev.json", "/sharedpolicygroups/spg-env-prod.json" })
        void should_use_correct_shared_policy_group_depending_on_environment(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test-dev")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Dev-0", "Header Dev 0"),
                            Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                        );
                    return true;
                })
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test-prod")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("X-Response-Header-Outside-0", "Header Outside 0"),
                            Map.entry("X-Response-Header-Prod-0", "Header Prod 0"),
                            Map.entry("X-Response-Header-Outside-1", "Header Outside 1")
                        );
                    return true;
                })
                .assertNoErrors();
        }
    }

    /**
     * A common class to not redeclare plugins for each Nested class
     */
    @GatewayTest
    static class TestPreparer extends AbstractGatewayTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.putIfAbsent(
                "transform-headers",
                PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }
    }
}
