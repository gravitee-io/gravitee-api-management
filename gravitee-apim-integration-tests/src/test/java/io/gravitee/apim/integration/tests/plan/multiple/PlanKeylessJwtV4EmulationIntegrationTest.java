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
package io.gravitee.apim.integration.tests.plan.multiple;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.plan.PlanHelper;
import io.gravitee.apim.integration.tests.plan.jwt.PlanJwtV4EmulationIntegrationTest;
import io.gravitee.apim.integration.tests.plan.keyless.PlanKeylessV4EmulationIntegrationTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.jwt.JWTPolicy;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.gravitee.policy.keyless.KeylessPolicy;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author GraviteeSource Team
 */
public class PlanKeylessJwtV4EmulationIntegrationTest {

    protected static String CUSTOM_AUDIENCE = "custom-audience";

    public static void configureApi(Api api) {
        configurePlans(api, Set.of("KEY_LESS", "JWT"));
        if (api.getId().endsWith("-selection-rule")) {
            Plan jwtPlan = api
                .getPlans()
                .stream()
                .filter(plan -> plan.getSecurity().equals("JWT"))
                .findFirst()
                .get();
            jwtPlan.setSelectionRule("#context.attributes['jwt'].claims['aud'][0] != '" + CUSTOM_AUDIENCE + "'");
        }
    }

    public static void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("key-less", PolicyBuilder.build("key-less", KeylessPolicy.class));
        policies.put("jwt", PolicyBuilder.build("jwt", JWTPolicy.class, JWTPolicyConfiguration.class));
    }

    public static class AbstractSelectJwtTest extends PlanJwtV4EmulationIntegrationTest {

        @Override
        public void configureApi(Api api) {
            PlanKeylessJwtV4EmulationIntegrationTest.configureApi(api);
        }

        @Override
        public void configurePolicies(final Map<String, PolicyPlugin> policies) {
            PlanKeylessJwtV4EmulationIntegrationTest.configurePolicies(policies);
        }

        protected Stream<Arguments> provideWrongSecurityHeaders() {
            return provideApis().flatMap(arguments -> {
                String apiId = (String) arguments.get()[0];
                return Stream.of(
                    Arguments.of(apiId, "Authorization", "Bearer"),
                    Arguments.of(apiId, "Authorization", "Bearer "),
                    Arguments.of(apiId, "Authorization", "Bearer a-jwt-token")
                );
            });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/plan/v2-api.json")
    public class SelectJwtTest extends AbstractSelectJwtTest {}

    public static class AbstractSelectKeylessTest extends PlanKeylessV4EmulationIntegrationTest {

        @Override
        public void configureApi(Api api) {
            PlanKeylessJwtV4EmulationIntegrationTest.configureApi(api);
        }

        @Override
        public void configurePolicies(final Map<String, PolicyPlugin> policies) {
            PlanKeylessJwtV4EmulationIntegrationTest.configurePolicies(policies);
        }

        protected Stream<Arguments> provideSecurityHeaders() {
            return provideApis().flatMap(arguments -> {
                String path = (String) arguments.get()[0];
                boolean requireWiremock = (boolean) arguments.get()[1];
                return Stream.of(
                    Arguments.of(path, requireWiremock, "X-Gravitee-Api-Key", "an-api-key"),
                    Arguments.of(path, requireWiremock, "Authorization", ""),
                    Arguments.of(path, requireWiremock, "Authorization", "Basic 123456789")
                );
            });
        }

        protected Stream<Arguments> provideSelectionRuleApis() {
            return provideApis().map(arguments -> {
                String apiId = (String) arguments.get()[0];
                return Arguments.of(apiId + "-selection-rule");
            });
        }

        @ParameterizedTest
        @MethodSource("provideSelectionRuleApis")
        @DeployApi("/apis/plan/v2-api-selection-rule.json")
        void should_return_200_success_when_selection_rules_on_jwt_doesnt_match(String apiId, HttpClient client) throws Exception {
            expect_return_200_success_when_selection_rules_on_jwt_doesnt_match(apiId, client);
        }

        /**
         * This method is free of any JUnit annotations to avoid annotation conflicts when used in tests of subclasses.
         */
        protected void expect_return_200_success_when_selection_rules_on_jwt_doesnt_match(String apiId, HttpClient client)
            throws Exception {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
            String jwtToken = PlanHelper.generateJWT(5000, Map.of("aud", CUSTOM_AUDIENCE));

            client
                .rxRequest(GET, getApiPath(apiId))
                .flatMap(request -> {
                    request.putHeader("Authorization", "Bearer " + jwtToken);
                    return request.rxSend();
                })
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(body -> {
                    assertThat(body.toString()).contains("endpoint response");
                    return true;
                });
        }
    }
}
