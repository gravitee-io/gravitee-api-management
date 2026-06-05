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
package io.gravitee.apim.rest.api.automation.mapper;

import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRDStatus;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.LegacySharedPolicyGroupSpec;
import io.gravitee.apim.rest.api.automation.model.SharedPolicyGroupApiType;
import io.gravitee.apim.rest.api.automation.model.SharedPolicyGroupState;
import io.gravitee.apim.rest.api.automation.model.StepV4;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.management.v2.rest.mapper.ConfigurationSerializationMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.DateMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.OriginContextMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class, OriginContextMapper.class })
public interface SharedPolicyGroupMapper {
    SharedPolicyGroupMapper INSTANCE = Mappers.getMapper(SharedPolicyGroupMapper.class);

    ApiType mapApiType(SharedPolicyGroupApiType apiType);

    FlowPhase mapFlowPhase(io.gravitee.apim.rest.api.automation.model.FlowPhase phase);

    @ValueMapping(source = "PROXY", target = "PROXY")
    @ValueMapping(source = "MESSAGE", target = "MESSAGE")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    SharedPolicyGroupApiType mapSharedPolicyGroupApiType(ApiType apiType);

    @ValueMapping(source = "ENTRYPOINT_CONNECT", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "INTERACT", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    io.gravitee.apim.rest.api.automation.model.FlowPhase mapAutomationFlowPhase(FlowPhase phase);

    @Mapping(target = "sharedPolicyGroupId", ignore = true)
    @Mapping(target = "originContext", ignore = true)
    io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD map(LegacySharedPolicyGroupSpec spec);

    default SharedPolicyGroupState toState(SharedPolicyGroup spg) {
        var state = new SharedPolicyGroupState(spg.getId(), spg.getEnvironmentId(), spg.getOrganizationId(), null, spg.getCrossId());
        mapToState(spg, state);
        return state;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "crossId", ignore = true)
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    void mapToState(SharedPolicyGroup spg, @MappingTarget SharedPolicyGroupState state);

    default SharedPolicyGroupState toState(LegacySharedPolicyGroupSpec spec, SharedPolicyGroupCRDStatus status) {
        var state = new SharedPolicyGroupState(
            status.getId(),
            status.getEnvironmentId(),
            status.getOrganizationId(),
            toErrors(status.getErrors()),
            status.getCrossId()
        );
        mapSpecToState(spec, state);
        return state;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "crossId", ignore = true)
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    void mapSpecToState(LegacySharedPolicyGroupSpec spec, @MappingTarget SharedPolicyGroupState state);

    Errors toErrors(SharedPolicyGroupCRDStatus.Errors errors);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Step mapStep(StepV4 step);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    StepV4 mapStep(Step step);
}
