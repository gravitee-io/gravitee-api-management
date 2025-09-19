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

import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.model.context.OriginContext;
import io.swagger.models.Model;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder(toBuilder = true)
public class SharedPolicyGroupCRD {

    /**
     * The shared policy group hrid uniquely identifies a shared policy group across environments.
     * Apis promoted between environments will share the same hrid.
     */
    private String hrid;

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
     * Tha API type compatible with the shared policy group
     */
    private ApiType apiType;
    /**
     * Tha the shared policy group Origin Context
     */
    private OriginContext originContext;
    /**
     * The shared policy group phase
     */
    private FlowPhase phase;
    /**
     * The shared policy group steps
     */
    private List<Step> steps;

    // Only for update
    private String sharedPolicyGroupId;

    public SharedPolicyGroup toSharedPolicyGroup() {
        return SharedPolicyGroup.builder()
            .hrid(hrid)
            .crossId(crossId)
            .id(sharedPolicyGroupId)
            .name(name)
            .description(description)
            .apiType(apiType)
            .originContext(new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED))
            .prerequisiteMessage(prerequisiteMessage)
            .steps(steps)
            .phase(phase)
            .build();
    }

    public CreateSharedPolicyGroup toCreateSharedPolicyGroup() {
        return CreateSharedPolicyGroup.builder()
            .id(sharedPolicyGroupId)
            .hrid(hrid)
            .crossId(crossId)
            .name(name)
            .description(description)
            .apiType(apiType)
            .originContext(new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED))
            .prerequisiteMessage(prerequisiteMessage)
            .steps(steps)
            .phase(phase)
            .build();
    }

    public SharedPolicyGroupCRD fromCreateSharedPolicyGroup(CreateSharedPolicyGroup createSharedPolicyGroup) {
        return SharedPolicyGroupCRD.builder()
            .hrid(createSharedPolicyGroup.getHrid())
            .crossId(createSharedPolicyGroup.getCrossId())
            .name(createSharedPolicyGroup.getName())
            .description(createSharedPolicyGroup.getDescription())
            .apiType(createSharedPolicyGroup.getApiType())
            .originContext(createSharedPolicyGroup.getOriginContext())
            .prerequisiteMessage(createSharedPolicyGroup.getPrerequisiteMessage())
            .steps(createSharedPolicyGroup.getSteps())
            .phase(createSharedPolicyGroup.getPhase())
            .build();
    }

    public UpdateSharedPolicyGroup toUpdateSharedPolicyGroup() {
        return UpdateSharedPolicyGroup.builder()
            .hrid(hrid)
            .crossId(crossId)
            .name(name)
            .description(description)
            .prerequisiteMessage(prerequisiteMessage)
            .steps(steps)
            .build();
    }

    public SharedPolicyGroupCRD fromUpdateSharedPolicyGroup(UpdateSharedPolicyGroup updateSharedPolicyGroup) {
        return SharedPolicyGroupCRD.builder()
            .hrid(updateSharedPolicyGroup.getHrid())
            .crossId(updateSharedPolicyGroup.getCrossId())
            .name(updateSharedPolicyGroup.getName())
            .description(updateSharedPolicyGroup.getDescription())
            .prerequisiteMessage(updateSharedPolicyGroup.getPrerequisiteMessage())
            .steps(updateSharedPolicyGroup.getSteps())
            .build();
    }
}
