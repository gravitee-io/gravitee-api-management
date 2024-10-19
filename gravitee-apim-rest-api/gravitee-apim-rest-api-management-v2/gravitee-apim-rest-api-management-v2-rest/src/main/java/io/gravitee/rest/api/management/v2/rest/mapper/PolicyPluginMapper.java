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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.FlowPhase;
import io.gravitee.rest.api.management.v2.rest.model.PolicyPlugin;
import io.gravitee.rest.api.management.v2.rest.model.PolicyPluginAllOfFlowPhaseCompatibility;
import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import java.util.Map;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PolicyPluginMapper {
    PolicyPluginMapper INSTANCE = Mappers.getMapper(PolicyPluginMapper.class);

    PolicyPlugin map(PolicyPluginEntity policyEntity);

    Set<PolicyPlugin> map(Set<PolicyPluginEntity> policyEntitySet);

    @Mapping(target = "flowPhaseCompatibility", qualifiedByName = "flowPhaseCompatibility")
    PolicyPlugin mapFromCore(io.gravitee.apim.core.plugin.model.PolicyPlugin policyEntity);

    Set<PolicyPlugin> mapFromCore(Set<io.gravitee.apim.core.plugin.model.PolicyPlugin> policyEntitySet);

    FlowPhase map(io.gravitee.rest.api.model.v4.policy.FlowPhase flowPhase);

    Set<FlowPhase> mapToFlowPhase(Set<io.gravitee.rest.api.model.v4.policy.FlowPhase> flowPhases);

    @Named("flowPhaseCompatibility")
    default PolicyPluginAllOfFlowPhaseCompatibility mapToFlowPhaseCompatibility(
        Map<io.gravitee.rest.api.model.v4.policy.ApiProtocolType, Set<io.gravitee.rest.api.model.v4.policy.FlowPhase>> flowPhaseCompatibility
    ) {
        if (flowPhaseCompatibility == null) {
            return null;
        }
        var policyPluginAllOfFlowPhaseCompatibility = new PolicyPluginAllOfFlowPhaseCompatibility();
        if (flowPhaseCompatibility.get(ApiProtocolType.HTTP_PROXY) != null) {
            policyPluginAllOfFlowPhaseCompatibility.HTTP_PROXY(mapToFlowPhase(flowPhaseCompatibility.get(ApiProtocolType.HTTP_PROXY)));
        }
        if (flowPhaseCompatibility.get(ApiProtocolType.HTTP_MESSAGE) != null) {
            policyPluginAllOfFlowPhaseCompatibility.HTTP_MESSAGE(mapToFlowPhase(flowPhaseCompatibility.get(ApiProtocolType.HTTP_MESSAGE)));
        }
        if (flowPhaseCompatibility.get(ApiProtocolType.NATIVE_KAFKA) != null) {
            policyPluginAllOfFlowPhaseCompatibility.NATIVE_KAFKA(mapToFlowPhase(flowPhaseCompatibility.get(ApiProtocolType.NATIVE_KAFKA)));
        }
        return policyPluginAllOfFlowPhaseCompatibility;
    }
}
