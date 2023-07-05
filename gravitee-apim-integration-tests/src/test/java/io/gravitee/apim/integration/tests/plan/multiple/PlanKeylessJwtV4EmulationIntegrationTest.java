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
package io.gravitee.apim.integration.tests.plan.multiple;

import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.plan.jwt.PlanJwtV4EmulationIntegrationTest;
import io.gravitee.apim.integration.tests.plan.keyless.PlanKeylessV4EmulationIntegrationTest;
import io.gravitee.definition.model.Api;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.jwt.JWTPolicy;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.gravitee.policy.keyless.KeylessPolicy;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.provider.Arguments;

/**
 * @author GraviteeSource Team
 */
public class PlanKeylessJwtV4EmulationIntegrationTest {

    public static void configureApi(Api api) {
        configurePlans(api, Set.of("KEY_LESS", "JWT"));
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
            return provideApis()
                .flatMap(arguments -> {
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
            return provideApis()
                .flatMap(arguments -> {
                    String path = (String) arguments.get()[0];
                    boolean requireWiremock = (boolean) arguments.get()[1];
                    return Stream.of(
                        Arguments.of(path, requireWiremock, "X-Gravitee-Api-Key", "an-api-key"),
                        Arguments.of(path, requireWiremock, "Authorization", ""),
                        Arguments.of(path, requireWiremock, "Authorization", "Basic 123456789")
                    );
                });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/plan/v2-api.json")
    public class SelectKeylessTest extends AbstractSelectKeylessTest {}
}
