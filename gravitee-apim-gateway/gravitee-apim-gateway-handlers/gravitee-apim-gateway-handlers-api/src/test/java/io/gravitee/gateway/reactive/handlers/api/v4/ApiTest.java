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

import static io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.gateway.reactive.handlers.api.security.plan.SecurityPlan;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ApiTest {

    @ParameterizedTest
    @CsvSource({ "API_KEY,STANDARD", "api-key,STANDARD", "JWT,STANDARD", "OAUTH2,STANDARD", ",PUSH" })
    void shouldReturnSubscribablePlans(String securityType, String mode) {
        final io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        final ArrayList<Plan> plans = new ArrayList<>();
        final PlanSecurity planSecurity = new PlanSecurity();
        final Plan plan = new Plan();
        plan.setMode(PlanMode.valueOf(mode));

        if (plan.getMode() != PlanMode.PUSH) {
            planSecurity.setType(securityType);
            plan.setSecurity(planSecurity);
        }
        plan.setId("subscribable");
        plans.add(plan);

        for (int i = 0; i < 5; i++) {
            final Plan other = new Plan();
            final PlanSecurity otherSecurity = new PlanSecurity();
            other.setMode(PlanMode.STANDARD);
            other.setId("not-subscribable-" + i);
            other.setSecurity(otherSecurity);
            plans.add(other);
        }

        definition.setPlans(plans);

        final Api api = new Api(definition);

        assertThat(api.getSubscribablePlans()).containsExactly("subscribable");
    }
}
