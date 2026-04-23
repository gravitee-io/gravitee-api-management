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
package io.gravitee.apim.core.plan.model;

import fixtures.core.model.PlanFixtures;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanUpdatesTest {

    @Test
    void applyTo_should_update_port_routing_fields_on_native_plan() {
        var oldPlan = PlanFixtures.NativeV4.aKeyless()
            .toBuilder()
            .planDefinitionNativeV4(
                NativePlan.builder()
                    .security(PlanSecurity.builder().type("key-less").build())
                    .mode(PlanMode.STANDARD)
                    .status(PlanStatus.PUBLISHED)
                    .bootstrapPort(9092)
                    .brokerRangeStart(9100)
                    .brokerRangeEnd(9102)
                    .build()
            )
            .build();

        var updates = PlanUpdates.builder()
            .name("new-name")
            .description("new-description")
            .order(1)
            .validation(Plan.PlanValidationType.MANUAL)
            .bootstrapPort(19092)
            .brokerRangeStart(19100)
            .brokerRangeEnd(19102)
            .build();

        var result = updates.applyTo(oldPlan);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getPlanDefinitionNativeV4().getBootstrapPort()).isEqualTo(19092);
            soft.assertThat(result.getPlanDefinitionNativeV4().getBrokerRangeStart()).isEqualTo(19100);
            soft.assertThat(result.getPlanDefinitionNativeV4().getBrokerRangeEnd()).isEqualTo(19102);
        });
    }

    @Test
    void applyTo_should_clear_port_routing_fields_when_updates_are_null() {
        // Switching from port-based routing to host/SNI routing mode
        var oldPlan = PlanFixtures.NativeV4.aKeyless()
            .toBuilder()
            .planDefinitionNativeV4(
                NativePlan.builder()
                    .security(PlanSecurity.builder().type("key-less").build())
                    .mode(PlanMode.STANDARD)
                    .status(PlanStatus.PUBLISHED)
                    .bootstrapPort(9092)
                    .brokerRangeStart(9100)
                    .brokerRangeEnd(9102)
                    .build()
            )
            .build();

        var updates = PlanUpdates.builder()
            .name("new-name")
            .description("new-description")
            .order(1)
            .validation(Plan.PlanValidationType.MANUAL)
            .build();

        var result = updates.applyTo(oldPlan);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getPlanDefinitionNativeV4().getBootstrapPort()).isNull();
            soft.assertThat(result.getPlanDefinitionNativeV4().getBrokerRangeStart()).isNull();
            soft.assertThat(result.getPlanDefinitionNativeV4().getBrokerRangeEnd()).isNull();
        });
    }

    @Test
    void applyTo_should_not_touch_port_routing_fields_on_http_plan() {
        // HTTP plans have no planDefinitionNativeV4 — port fields must be a no-op
        var oldPlan = PlanFixtures.HttpV4.anApiKey();

        var updates = PlanUpdates.builder()
            .name("new-name")
            .description("new-description")
            .order(1)
            .validation(Plan.PlanValidationType.MANUAL)
            .bootstrapPort(9092)
            .brokerRangeStart(9100)
            .brokerRangeEnd(9102)
            .build();

        var result = updates.applyTo(oldPlan);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getPlanDefinitionNativeV4()).isNull();
            soft.assertThat(result.getPlanDefinitionHttpV4()).isNotNull();
        });
    }
}
