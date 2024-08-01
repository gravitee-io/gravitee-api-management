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
package io.gravitee.apim.core.shared_policy_group.model;

import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SharedPolicyGroup {

    /**
     * The shared policy group ID
     */
    private String id;
    /**
     * The ID of the environment attached
     */
    private String environmentId;
    /**
     * The ID of the organization attached
     */
    private String organizationId;
    /**
     * The shared policy group crossId uniquely identifies a shared policy group across environments.
     * Apis promoted between environments will share the same crossId. Gateways will use this crossId to identify the shared policy group
     */
    private String crossId;
    /**
     * The shared policy group name.
     */
    private String name;
    /**
     * the shared policy group description.
     */
    private String description;
    /**
     * The shared policy group version.
     */
    private Integer version;
    /**
     * Tha API type compatible with the shared policy group
     */
    private ApiType apiType;
    /**
     * The shared policy group phase
     */
    private PolicyPlugin.ExecutionPhase phase;
    /**
     * The shared policy group steps
     */
    private List<Step> steps;
    /**
     * Deployment date
     */
    private ZonedDateTime deployedAt;
    /**
     * Creation date
     */
    private ZonedDateTime createdAt;
    /**
     * Last updated date
     */
    private ZonedDateTime updatedAt;
    /**
     * The current runtime life cycle state.
     */
    private SharedPolicyGroupLifecycleState lifecycleState;

    public enum SharedPolicyGroupLifecycleState {
        DEPLOYED,
        UNDEPLOYED,
    }

    public static SharedPolicyGroup from(CreateSharedPolicyGroup createSharedPolicyGroup) {
        return SharedPolicyGroup
            .builder()
            .crossId(createSharedPolicyGroup.getCrossId() == null ? UuidString.generateRandom() : createSharedPolicyGroup.getCrossId())
            .name(createSharedPolicyGroup.getName())
            .description(createSharedPolicyGroup.getDescription())
            .apiType(createSharedPolicyGroup.getApiType())
            .phase(createSharedPolicyGroup.getPhase())
            .steps(createSharedPolicyGroup.getSteps())
            .build();
    }

    public static SharedPolicyGroup from(SharedPolicyGroup existingSharedPolicyGroup, UpdateSharedPolicyGroup updateSharedPolicyGroup) {
        return existingSharedPolicyGroup
            .toBuilder()
            .crossId(
                updateSharedPolicyGroup.getCrossId() == null ? existingSharedPolicyGroup.getCrossId() : updateSharedPolicyGroup.getCrossId()
            )
            .name(updateSharedPolicyGroup.getName() == null ? existingSharedPolicyGroup.getName() : updateSharedPolicyGroup.getName())
            .description(
                updateSharedPolicyGroup.getDescription() == null
                    ? existingSharedPolicyGroup.getDescription()
                    : updateSharedPolicyGroup.getDescription()
            )
            .steps(updateSharedPolicyGroup.getSteps() == null ? existingSharedPolicyGroup.getSteps() : updateSharedPolicyGroup.getSteps())
            .build();
    }

    public boolean hasName() {
        return this.name != null && !this.name.isEmpty();
    }

    public boolean hasValidPhase() {
        if (this.apiType == ApiType.PROXY) {
            return this.phase == PolicyPlugin.ExecutionPhase.REQUEST || this.phase == PolicyPlugin.ExecutionPhase.RESPONSE;
        } else if (this.apiType == ApiType.MESSAGE) {
            return (
                this.phase == PolicyPlugin.ExecutionPhase.REQUEST ||
                this.phase == PolicyPlugin.ExecutionPhase.RESPONSE ||
                this.phase == PolicyPlugin.ExecutionPhase.MESSAGE_REQUEST ||
                this.phase == PolicyPlugin.ExecutionPhase.MESSAGE_RESPONSE
            );
        }
        return false;
    }
}
