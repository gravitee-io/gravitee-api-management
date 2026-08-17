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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EdgeApiTest {

    private static EdgeApi edgeApi(List<Plan> plans) {
        return new EdgeApi(io.gravitee.definition.model.v4.edge.EdgeApi.builder().id("edge-api").plans(plans).build());
    }

    private static Plan plan(String id, String securityType) {
        return Plan.builder().id(id).security(PlanSecurity.builder().type(securityType).build()).build();
    }

    @Nested
    class Subscribable_plans {

        @Test
        void should_return_the_ids_of_the_subscribable_plans() {
            // Given
            EdgeApi api = edgeApi(List.of(plan("mtls-plan", "MTLS"), plan("keyless-plan", "KEY_LESS")));

            // When / Then
            assertThat(api.getSubscribablePlans()).containsExactly("mtls-plan");
        }

        @Test
        void should_return_an_empty_set_when_the_definition_has_no_plan() {
            // Given
            EdgeApi api = edgeApi(null);

            // When / Then
            assertThat(api.getSubscribablePlans()).isEmpty();
        }

        @Test
        void should_return_an_empty_set_when_every_plan_is_keyless() {
            // Given
            EdgeApi api = edgeApi(List.of(plan("keyless-plan", "KEY_LESS")));

            // When / Then
            assertThat(api.getSubscribablePlans()).isEmpty();
        }
    }

    @Nested
    class Api_key_plans {

        @Test
        void should_always_be_empty_since_an_edge_api_cannot_carry_an_api_key_plan() {
            // Given
            EdgeApi api = edgeApi(List.of(plan("apikey-plan", "API_KEY")));

            // When / Then
            assertThat(api.getApiKeyPlans()).isEmpty();
        }
    }
}
