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
package io.gravitee.apim.rest.api.automation.helpers;

import static io.gravitee.apim.rest.api.automation.resource.ApisResource.HRID_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.apim.rest.api.automation.model.ApiV4Spec;
import io.gravitee.apim.rest.api.automation.model.FlowV4;
import io.gravitee.apim.rest.api.automation.model.PlanV4;
import io.gravitee.apim.rest.api.automation.model.StepV4;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupIdHelperTest {

    static final String SPG_POLICY = SharedPolicyGroupPolicyPlugin.SHARED_POLICY_GROUP_POLICY_ID;

    private static StepV4 spgStep(String name) {
        return new StepV4().name(name).policy(SPG_POLICY).configuration(new HashMap<>(Map.of("sharedPolicyGroupId", "some-id")));
    }

    private static StepV4 spgStepWithExistingHrid(String name, String hrid) {
        return new StepV4()
            .name(name)
            .policy(SPG_POLICY)
            .configuration(new HashMap<>(Map.of("sharedPolicyGroupId", "some-id", HRID_FIELD, hrid)));
    }

    private static StepV4 nonSpgStep(String name) {
        return new StepV4().name(name).policy("rate-limit").configuration(new HashMap<>(Map.of("rate", "10")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> config(StepV4 step) {
        return (Map<String, Object>) step.getConfiguration();
    }

    @Nested
    class AddHRID {

        @Test
        void should_add_hrid_to_request_phase() {
            var step = spgStep("My SPG");
            var spec = new ApiV4Spec().flows(List.of(new FlowV4().request(List.of(step))));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).containsEntry(HRID_FIELD, "my-spg");
        }

        @Test
        void should_add_hrid_to_response_phase() {
            var step = spgStep("Response SPG");
            var spec = new ApiV4Spec().flows(List.of(new FlowV4().response(List.of(step))));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).containsEntry(HRID_FIELD, "response-spg");
        }

        @Test
        void should_add_hrid_to_subscribe_phase() {
            var step = spgStep("Subscribe SPG");
            var spec = new ApiV4Spec().flows(List.of(new FlowV4().subscribe(List.of(step))));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).containsEntry(HRID_FIELD, "subscribe-spg");
        }

        @Test
        void should_add_hrid_to_publish_phase() {
            var step = spgStep("Publish SPG");
            var spec = new ApiV4Spec().flows(List.of(new FlowV4().publish(List.of(step))));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).containsEntry(HRID_FIELD, "publish-spg");
        }

        @Test
        void should_add_hrid_to_interact_phase() {
            var step = spgStep("Interact SPG");
            var spec = new ApiV4Spec().flows(List.of(new FlowV4().interact(List.of(step))));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).containsEntry(HRID_FIELD, "interact-spg");
        }

        @Test
        void should_add_hrid_to_entrypoint_connect_phase() {
            var step = spgStep("Connect SPG");
            var spec = new ApiV4Spec().flows(List.of(new FlowV4().entrypointConnect(List.of(step))));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).containsEntry(HRID_FIELD, "connect-spg");
        }

        @Test
        void should_not_overwrite_existing_hrid() {
            var step = spgStepWithExistingHrid("My SPG", "custom-hrid");
            var spec = new ApiV4Spec().flows(List.of(new FlowV4().request(List.of(step))));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).containsEntry(HRID_FIELD, "custom-hrid");
        }

        @Test
        void should_ignore_non_spg_steps() {
            var step = nonSpgStep("Rate Limit");
            var spec = new ApiV4Spec().flows(List.of(new FlowV4().request(List.of(step))));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).doesNotContainKey(HRID_FIELD);
        }

        @Test
        void should_add_hrid_to_plan_flows() {
            var step = spgStep("Plan SPG");
            var plan = new PlanV4().flows(List.of(new FlowV4().request(List.of(step))));
            var spec = new ApiV4Spec().plans(List.of(plan));

            SharedPolicyGroupIdHelper.addHRID(spec);

            assertThat(config(step)).containsEntry(HRID_FIELD, "plan-spg");
        }

        @Test
        void should_handle_null_flows() {
            var spec = new ApiV4Spec().flows(null).plans(null);

            assertThatCode(() -> SharedPolicyGroupIdHelper.addHRID(spec)).doesNotThrowAnyException();
        }
    }
}
