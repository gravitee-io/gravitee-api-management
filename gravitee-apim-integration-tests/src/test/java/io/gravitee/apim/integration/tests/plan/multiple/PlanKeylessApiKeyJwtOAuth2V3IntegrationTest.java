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

import static io.gravitee.definition.model.ExecutionMode.V3;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.mockito.stubbing.OngoingStubbing;

/**
 * @author GraviteeSource Team
 */
public class PlanKeylessApiKeyJwtOAuth2V3IntegrationTest {

    @GatewayTest(v2ExecutionMode = V3)
    @Nested
    public class SelectApiKeyTest extends PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.AbstractSelectApiKeyTest {}

    @Nested
    @GatewayTest(v2ExecutionMode = V3)
    public class SelectKeylessTest extends PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.AbstractSelectKeylessTest {}

    @Nested
    @GatewayTest(v2ExecutionMode = V3)
    public class SelectJwtTest extends PlanKeylessApiKeyJwtOAuth2V4EmulationIntegrationTest.AbstractSelectJwtTest {

        /**
         * This overrides subscription search :
         * - in jupiter its searched with getByApiAndSecurityToken
         * - in V3 its searches with api/clientId/plan
         */
        @Override
        protected OngoingStubbing<Optional<Subscription>> whenSearchingSubscription(String api, String clientId, String plan) {
            return when(getBean(SubscriptionService.class).getByApiAndClientIdAndPlan(api, clientId, plan));
        }
    }
}
