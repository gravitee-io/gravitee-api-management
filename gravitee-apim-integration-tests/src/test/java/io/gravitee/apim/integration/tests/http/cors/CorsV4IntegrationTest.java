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
package io.gravitee.apim.integration.tests.http.cors;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class CorsV4IntegrationTest extends CorsV4EmulationIntegrationTest {

    @Nested
    @GatewayTest
    @DeployApi(
        {
            "/apis/v4/http/cors-running-policies.json",
            "/apis/v4/http/cors-not-running-policies.json",
            "/apis/v4/http/cors-with-response-template.json",
        }
    )
    class CheckingResponseStatus extends CorsV4EmulationIntegrationTest.CheckingResponseStatus {}

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/cors-running-policies.json", "/apis/v4/http/cors-not-running-policies.json" })
    class CheckingPoliciesExecution extends CorsV4EmulationIntegrationTest.CheckingPoliciesExecution {}

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/cors-running-policies.json", "/apis/v4/http/cors-not-running-policies.json" })
    class CheckingRejection extends CorsV4EmulationIntegrationTest.CheckingRejection {}

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/cors-running-policies.json" })
    class CheckingSecurityChainSkip extends CorsV4EmulationIntegrationTest.CheckingSecurityChainSkip {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);

            if (api.getDefinition() instanceof Api) {
                var security = new PlanSecurity();
                security.setType("api-key");

                var plan = Plan.builder().id("plan-id").security(security).status(PlanStatus.PUBLISHED).build();
                ((Api) api.getDefinition()).setPlans(List.of(plan));
            }
        }
    }
}
