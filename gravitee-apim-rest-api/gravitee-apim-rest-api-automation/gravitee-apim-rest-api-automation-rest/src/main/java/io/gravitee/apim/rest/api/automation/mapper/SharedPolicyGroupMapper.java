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

import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRDStatus;
import io.gravitee.apim.rest.api.automation.model.FlowStep;
import io.gravitee.apim.rest.api.automation.model.LegacySharedPolicyGroupSpec;
import io.gravitee.apim.rest.api.automation.model.SharedPolicyGroupState;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.management.v2.rest.mapper.ConfigurationSerializationMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.DateMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.OriginContextMapper;
import io.gravitee.rest.api.management.v2.rest.model.StepV4;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class, OriginContextMapper.class })
public interface SharedPolicyGroupMapper {
    SharedPolicyGroupMapper INSTANCE = Mappers.getMapper(SharedPolicyGroupMapper.class);

    @Mapping(target = "sharedPolicyGroupId", ignore = true)
    @Mapping(target = "originContext", ignore = true)
    io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD map(LegacySharedPolicyGroupSpec spec);

    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    SharedPolicyGroupState toState(LegacySharedPolicyGroupSpec spec);

    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "prerequisiteMessage", ignore = true)
    @Mapping(target = "phase", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "hrid", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "apiType", ignore = true)
    @Mapping(target = "organizationId", source = "status.organizationId")
    @Mapping(target = "environmentId", source = "status.environmentId")
    SharedPolicyGroupState withStatusInfos(@MappingTarget SharedPolicyGroupState state, SharedPolicyGroupCRDStatus status);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Step mapStep(FlowStep step);
}
