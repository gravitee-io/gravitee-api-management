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

import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.management.v2.rest.model.CreateSharedPolicyGroup;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroup;
import io.gravitee.rest.api.management.v2.rest.model.StepV4;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSharedPolicyGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class })
public interface SharedPolicyGroupMapper {
    SharedPolicyGroupMapper INSTANCE = Mappers.getMapper(SharedPolicyGroupMapper.class);

    io.gravitee.apim.core.shared_policy_group.model.CreateSharedPolicyGroup map(CreateSharedPolicyGroup sharedPolicyGroup);

    SharedPolicyGroup map(io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup sharedPolicyGroup);

    io.gravitee.apim.core.shared_policy_group.model.UpdateSharedPolicyGroup map(UpdateSharedPolicyGroup sharedPolicyGroup);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    StepV4 mapStep(Step step);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Step mapStep(StepV4 stepV4);
}
