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

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
     * The shared policy group prerequisite message.
     * This message is displayed to the user to help understand the prerequisite to use the shared policy group.
     */
    private String prerequisiteMessage;
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

    public static SharedPolicyGroupBuilder builder() {
        return new SharedPolicyGroupBuilder();
    }

    public void deploy() {
        lifecycleState = SharedPolicyGroupLifecycleState.DEPLOYED;
        version = version != null ? version + 1 : 1;
        final ZonedDateTime now = TimeProvider.now();
        deployedAt = now;
        updatedAt = now;
    }

    public void undeploy() {
        lifecycleState = SharedPolicyGroupLifecycleState.UNDEPLOYED;
        final ZonedDateTime now = TimeProvider.now();
        deployedAt = now;
        updatedAt = now;
    }

    public io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup toDefinition() {
        return io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup
            .builder()
            .id(crossId)
            .environmentId(environmentId)
            .policies(steps)
            .phase(io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.valueOf(phase.name()))
            .name(name)
            .deployedAt(Date.from(deployedAt.toInstant()))
            .build();
    }

    public SharedPolicyGroupBuilder toBuilder() {
        return new SharedPolicyGroupBuilder()
            .id(this.id)
            .environmentId(this.environmentId)
            .organizationId(this.organizationId)
            .crossId(this.crossId)
            .name(this.name)
            .description(this.description)
            .prerequisiteMessage(this.prerequisiteMessage)
            .version(this.version)
            .apiType(this.apiType)
            .phase(this.phase)
            .steps(this.steps)
            .deployedAt(this.deployedAt)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .lifecycleState(this.lifecycleState);
    }

    /**
     * Shared policy group life cycle state
     * UNDEPLOYED: The shared policy group is not deployed
     * DEPLOYED: The shared policy group is deployed
     * PENDING: The shared policy group is deployed but last changes are not yet deployed
     *
     * Change lifecycle :
     * - (Create SPG)[UNDEPLOYED] -> (Deploy SPG)[DEPLOYED] -> (Update SPG)[PENDING] -> (Deploy SPG)[DEPLOYED]
     * - (Deployed SPG)[DEPLOYED] -> (Undeploy SPG)[UNDEPLOYED] -> (Update SPG)[UNDEPLOYED] -> (Deploy SPG)[DEPLOYED]
     * - (Updated SPG)[PENDING] -> (Undeploy SPG)[UNDEPLOYED]
     */
    public enum SharedPolicyGroupLifecycleState {
        UNDEPLOYED,
        DEPLOYED,
        PENDING,
    }

    public static SharedPolicyGroup from(CreateSharedPolicyGroup createSharedPolicyGroup) {
        return SharedPolicyGroup
            .builder()
            .crossId(createSharedPolicyGroup.getCrossId() == null ? UuidString.generateRandom() : createSharedPolicyGroup.getCrossId())
            .name(createSharedPolicyGroup.getName())
            .description(createSharedPolicyGroup.getDescription())
            .prerequisiteMessage(createSharedPolicyGroup.getPrerequisiteMessage())
            .apiType(createSharedPolicyGroup.getApiType())
            .phase(createSharedPolicyGroup.getPhase())
            .steps(ofNullable(createSharedPolicyGroup.getSteps()).orElse(emptyList()))
            .build();
    }

    public SharedPolicyGroup update(UpdateSharedPolicyGroup updateSharedPolicyGroup) {
        return this.toBuilder()
            .crossId(updateSharedPolicyGroup.getCrossId() == null ? this.getCrossId() : updateSharedPolicyGroup.getCrossId())
            .name(updateSharedPolicyGroup.getName() == null ? this.getName() : updateSharedPolicyGroup.getName())
            .description(
                updateSharedPolicyGroup.getDescription() == null ? this.getDescription() : updateSharedPolicyGroup.getDescription()
            )
            .prerequisiteMessage(
                updateSharedPolicyGroup.getPrerequisiteMessage() == null
                    ? this.getPrerequisiteMessage()
                    : updateSharedPolicyGroup.getPrerequisiteMessage()
            )
            .steps(updateSharedPolicyGroup.getSteps() == null ? this.getSteps() : updateSharedPolicyGroup.getSteps())
            .updatedAt(TimeProvider.now())
            .lifecycleState(
                this.lifecycleState == SharedPolicyGroupLifecycleState.DEPLOYED
                    ? SharedPolicyGroupLifecycleState.PENDING
                    : this.lifecycleState
            )
            .build();
    }

    public boolean hasName() {
        return this.name != null && !this.name.isEmpty();
    }

    public boolean isDeployed() {
        return this.lifecycleState == SharedPolicyGroupLifecycleState.DEPLOYED;
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

    public static class SharedPolicyGroupBuilder {

        private String id;
        private String environmentId;
        private String organizationId;
        private String crossId;
        private String name;
        private String description;
        private String prerequisiteMessage;
        private Integer version;
        private ApiType apiType;
        private PolicyPlugin.ExecutionPhase phase;
        private List<Step> steps;
        private ZonedDateTime deployedAt;
        private ZonedDateTime createdAt;
        private ZonedDateTime updatedAt;
        private SharedPolicyGroupLifecycleState lifecycleState;

        SharedPolicyGroupBuilder() {}

        public SharedPolicyGroupBuilder id(String id) {
            this.id = id;
            return this;
        }

        public SharedPolicyGroupBuilder environmentId(String environmentId) {
            this.environmentId = environmentId;
            return this;
        }

        public SharedPolicyGroupBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public SharedPolicyGroupBuilder crossId(String crossId) {
            this.crossId = crossId;
            return this;
        }

        public SharedPolicyGroupBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SharedPolicyGroupBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SharedPolicyGroupBuilder prerequisiteMessage(String prerequisiteMessage) {
            this.prerequisiteMessage = prerequisiteMessage;
            return this;
        }

        public SharedPolicyGroupBuilder version(Integer version) {
            this.version = version;
            return this;
        }

        public SharedPolicyGroupBuilder apiType(ApiType apiType) {
            this.apiType = apiType;
            return this;
        }

        public SharedPolicyGroupBuilder phase(PolicyPlugin.ExecutionPhase phase) {
            this.phase = phase;
            return this;
        }

        public SharedPolicyGroupBuilder steps(List<Step> steps) {
            this.steps = steps;
            return this;
        }

        public SharedPolicyGroupBuilder deployedAt(ZonedDateTime deployedAt) {
            this.deployedAt = deployedAt;
            return this;
        }

        public SharedPolicyGroupBuilder createdAt(ZonedDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SharedPolicyGroupBuilder updatedAt(ZonedDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SharedPolicyGroupBuilder lifecycleState(SharedPolicyGroupLifecycleState lifecycleState) {
            this.lifecycleState = lifecycleState;
            return this;
        }

        public SharedPolicyGroup build() {
            return new SharedPolicyGroup(
                this.id,
                this.environmentId,
                this.organizationId,
                this.crossId,
                this.name,
                this.description,
                this.prerequisiteMessage,
                this.version,
                this.apiType,
                this.phase,
                this.steps,
                this.deployedAt,
                this.createdAt,
                this.updatedAt,
                this.lifecycleState
            );
        }

        public String toString() {
            return (
                "SharedPolicyGroup.SharedPolicyGroupBuilder(id=" +
                this.id +
                ", environmentId=" +
                this.environmentId +
                ", organizationId=" +
                this.organizationId +
                ", crossId=" +
                this.crossId +
                ", name=" +
                this.name +
                ", description=" +
                this.description +
                ", prerequisiteMessage=" +
                this.prerequisiteMessage +
                ", version=" +
                this.version +
                ", apiType=" +
                this.apiType +
                ", phase=" +
                this.phase +
                ", steps=" +
                this.steps +
                ", deployedAt=" +
                this.deployedAt +
                ", createdAt=" +
                this.createdAt +
                ", updatedAt=" +
                this.updatedAt +
                ", lifecycleState=" +
                this.lifecycleState +
                ")"
            );
        }
    }
}
