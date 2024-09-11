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

import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.apim.integration.tests.plan.apikey.PlanApiKeyV4EmulationIntegrationTest;
import io.gravitee.apim.integration.tests.plan.jwt.PlanJwtV4EmulationIntegrationTest;
import io.gravitee.apim.integration.tests.plan.keyless.PlanKeylessV4EmulationIntegrationTest;
import io.gravitee.apim.integration.tests.plan.oauth2.MockOAuth2Resource;
import io.gravitee.apim.integration.tests.plan.oauth2.PlanOAuth2V4EmulationIntegrationTest;
import io.gravitee.definition.model.Api;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.jwt.JWTPolicy;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.gravitee.policy.keyless.KeylessPolicy;
import io.gravitee.policy.oauth2.Oauth2Policy;
import io.gravitee.policy.oauth2.configuration.OAuth2PolicyConfiguration;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.provider.Arguments;

/**
 * @author GraviteeSource Team
 */
public class PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest {

    public static void configureApi(Api api) {
        configurePlans(api, Set.of("KEY_LESS", "API_KEY", "JWT", "OAUTH2"));
    }

    public static void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("key-less", PolicyBuilder.build("key-less", KeylessPolicy.class));
        policies.put("api-key", PolicyBuilder.build("api-key", ApiKeyPolicy.class, null, ApiKeyPolicyInitializer.class));
        policies.put("jwt", PolicyBuilder.build("jwt", JWTPolicy.class, JWTPolicyConfiguration.class));
        policies.put("oauth2", PolicyBuilder.build("oauth2", Oauth2Policy.class, OAuth2PolicyConfiguration.class));
    }

    public static void configureResources(Map<String, ResourcePlugin> resources) {
        resources.put("mock-oauth2-resource", ResourceBuilder.build("mock-oauth2-resource", MockOAuth2Resource.class));
    }

    public static class AbstractSelectKeylessTest extends PlanKeylessV4EmulationIntegrationTest {

        @Override
        public void configureResources(Map<String, ResourcePlugin> resources) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configureResources(resources);
        }

        @Override
        public void configureApi(Api api) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configureApi(api);
        }

        @Override
        public void configurePolicies(final Map<String, PolicyPlugin> policies) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configurePolicies(policies);
        }

        @Disabled("Disabled as all security type are defined on the api")
        @Override
        protected void should_access_api_and_ignore_security(
            final String apiId,
            boolean requireWiremock,
            final String headerName,
            final String headerValue,
            HttpClient client
        ) {
            // This is disabled as all security type are defined on the api
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/plan/v2-api.json")
    public class SelectKeylessTest extends AbstractSelectKeylessTest {}

    public static class AbstractSelectApiKeyTest extends PlanApiKeyV4EmulationIntegrationTest {

        @Override
        public void configureResources(Map<String, ResourcePlugin> resources) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configureResources(resources);
        }

        @Override
        public void configureApi(Api api) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configureApi(api);
        }

        @Override
        public void configurePolicies(final Map<String, PolicyPlugin> policies) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configurePolicies(policies);
        }

        protected Stream<Arguments> provideWrongSecurityHeaders() {
            return super
                .provideWrongSecurityHeaders()
                .filter(arguments -> arguments.get()[1] != null && arguments.get()[2] != null && arguments.get()[2] != "");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/plan/v2-api.json")
    public class SelectApiKeyTest extends AbstractSelectApiKeyTest {}

    public static class AbstractSelectJwtTest extends PlanJwtV4EmulationIntegrationTest {

        @Override
        public void configureResources(Map<String, ResourcePlugin> resources) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configureResources(resources);
        }

        @Override
        public void configureApi(Api api) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configureApi(api);
        }

        @Override
        public void configurePolicies(final Map<String, PolicyPlugin> policies) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configurePolicies(policies);
        }

        protected Stream<Arguments> provideWrongSecurityHeaders() {
            return provideApis()
                .flatMap(arguments -> {
                    String apiId = (String) arguments.get()[0];
                    return Stream.of(
                        Arguments.of(apiId, "X-Gravitee-Api-Key", "an-api-key"),
                        Arguments.of(apiId, "X-Gravitee-Api-Key", ""),
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

    public static class AbstractSelectOAuth2Test extends PlanOAuth2V4EmulationIntegrationTest {

        @Override
        public void configureResources(Map<String, ResourcePlugin> resources) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configureResources(resources);
        }

        @Override
        public void configureApi(Api api) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configureApi(api);
        }

        @Override
        public void configurePolicies(final Map<String, PolicyPlugin> policies) {
            PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.configurePolicies(policies);
        }

        protected Stream<Arguments> provideWrongSecurityHeaders() {
            return provideApis()
                .flatMap(arguments -> {
                    String apiId = (String) arguments.get()[0];
                    return Stream.of(
                        Arguments.of(apiId, "X-Gravitee-Api-Key", "an-api-key"),
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
    public class SelectOauth2Test extends AbstractSelectOAuth2Test {}
}
