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
package fixtures.core.model;

import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.apim.core.shared_policy_group.model.CreateSharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRDStatus;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Supplier;

public class SharedPolicyGroupFixtures {

    private SharedPolicyGroupFixtures() {}

    private static final Supplier<SharedPolicyGroup.SharedPolicyGroupBuilder> BASE = () ->
        SharedPolicyGroup.builder()
            .id("sharedPolicyGroupId")
            .environmentId("environmentId")
            .organizationId("organizationId")
            .crossId("crossId")
            .name("name")
            .description("description")
            .prerequisiteMessage("prerequisiteMessage")
            .version(1)
            .apiType(ApiType.MESSAGE)
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED)
            .steps(List.of(Step.builder().policy("policyId").name("Step name").configuration("{\"key\":\"value\"}").build()))
            .phase(FlowPhase.REQUEST)
            .deployedAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()));

    public static SharedPolicyGroup aSharedPolicyGroup() {
        return BASE.get().build();
    }

    private static final SharedPolicyGroupCRD.SharedPolicyGroupCRDBuilder CRD_BASE = SharedPolicyGroupCRD.builder()
        .crossId(UUID.random().toString())
        .name("My Shared Policy Group CRD")
        .apiType(io.gravitee.definition.model.v4.ApiType.PROXY)
        .phase(io.gravitee.apim.core.plugin.model.FlowPhase.REQUEST);

    public static SharedPolicyGroupCRD aSharedPolicyGroupCRD() {
        return CRD_BASE.build();
    }

    public static SharedPolicyGroupCRDStatus aSharedPolicyGroupCRDStatus() {
        return new SharedPolicyGroupCRDStatus(UUID.random().toString(), UUID.random().toString(), "organizationId", "environmentId", null);
    }

    private static final Supplier<CreateSharedPolicyGroup.CreateSharedPolicyGroupBuilder> CREATE_BASE = () ->
        CreateSharedPolicyGroup.builder().name("name").apiType(ApiType.MESSAGE).phase(FlowPhase.REQUEST);

    public static CreateSharedPolicyGroup aCreateSharedPolicyGroup() {
        return CREATE_BASE.get().build();
    }

    public static CreateSharedPolicyGroup aCreateSharedPolicyGroupWithAllFields() {
        return CREATE_BASE.get()
            .description("description")
            .crossId("crossId")
            .steps(List.of(Step.builder().policy("policyId").name("Step name").configuration("{\"key\":\"value\"}").build()))
            .build();
    }

    public static SharedPolicyGroupPolicyPlugin aSharedPolicyGroupPolicyPlugin() {
        return SharedPolicyGroupPolicyPlugin.builder()
            .id("sharedPolicyGroupId")
            .name("name")
            .description("description")
            .apiType(ApiType.MESSAGE)
            .phase(FlowPhase.SUBSCRIBE)
            .policyId("shared-policy-group-policy")
            .build();
    }
}
