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

import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD;
import io.gravitee.definition.model.v4.flow.step.StepV4;
import io.gravitee.rest.api.management.v2.rest.model.CreateSharedPolicyGroup;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroup;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSharedPolicyGroup;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class, OriginContextMapper.class })
public interface SharedPolicyGroupMapper {
    SharedPolicyGroupMapper INSTANCE = Mappers.getMapper(SharedPolicyGroupMapper.class);

    io.gravitee.apim.core.shared_policy_group.model.CreateSharedPolicyGroup map(CreateSharedPolicyGroup sharedPolicyGroup);

    io.gravitee.apim.core.shared_policy_group.model.CreateSharedPolicyGroup map(SharedPolicyGroupCRD spec);

    List<SharedPolicyGroup> map(List<io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup> sharedPolicyGroups);

    SharedPolicyGroup map(io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup sharedPolicyGroup);

    io.gravitee.apim.core.shared_policy_group.model.UpdateSharedPolicyGroup map(UpdateSharedPolicyGroup sharedPolicyGroup);

    SharedPolicyGroupCRD map(io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupCRD spec);

    io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup mapToModel(
        io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupCRD spec
    );

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    io.gravitee.rest.api.management.v2.rest.model.StepV4 mapStep(StepV4 step);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    StepV4 mapStep(io.gravitee.rest.api.management.v2.rest.model.StepV4 stepV4);

    List<SharedPolicyGroupPolicyPlugin> mapToSharedPolicyGroupPolicyPlugins(
        List<io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin> sharedPolicyGroupPolicyPlugins
    );

    SharedPolicyGroupPolicyPlugin mapToSharedPolicyGroupPolicyPlugin(
        io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin sharedPolicyGroupPolicyPlugin
    );
}
