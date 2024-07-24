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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupDefinition;
import java.io.IOException;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = { JsonDeserializer.class, JsonDeserializer.class },
    injectionStrategy = InjectionStrategy.FIELD,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL
)
public abstract class SharedPolicyGroupAdapter {

    private final Logger LOGGER = LoggerFactory.getLogger(SharedPolicyGroupAdapter.class);

    public abstract SharedPolicyGroup toEntity(io.gravitee.repository.management.model.SharedPolicyGroup sharedPolicyGroup);

    @Mapping(target = "definition", source = ".", qualifiedByName = "serializeDefinition")
    public abstract io.gravitee.repository.management.model.SharedPolicyGroup fromEntity(SharedPolicyGroup sharedPolicyGroupEntity);

    @AfterMapping
    protected void addPolicyGroupDefinition(
        @MappingTarget SharedPolicyGroup result,
        io.gravitee.repository.management.model.SharedPolicyGroup sharedPolicyGroup
    ) {
        var definition = sharedPolicyGroup.getDefinition();
        if (definition == null || definition.isEmpty()) {
            return;
        }
        try {
            var sharedPolicyGroupDefinition = GraviteeJacksonMapper.getInstance().readValue(definition, SharedPolicyGroupDefinition.class);
            result.setSteps(sharedPolicyGroupDefinition.getSteps());
            result.setPhase(sharedPolicyGroupDefinition.getPhase());
        } catch (IOException ioe) {
            LOGGER.error("Unexpected error while deserializing SharedPolicyGroup definition with id:" + result.getId(), ioe);
        }
    }

    @Named("serializeDefinition")
    public String serializeDefinition(SharedPolicyGroup sharedPolicyGroupEntity) {
        if (sharedPolicyGroupEntity == null) {
            return null;
        }

        try {
            var sharedPolicyGroupDefinition = SharedPolicyGroupDefinition
                .builder()
                .steps(sharedPolicyGroupEntity.getSteps())
                .phase(sharedPolicyGroupEntity.getPhase())
                .build();

            return GraviteeJacksonMapper.getInstance().writeValueAsString(sharedPolicyGroupDefinition);
        } catch (IOException ioe) {
            throw new RuntimeException(
                "Unexpected error while serializing SharedPolicyGroup definition with id:" + sharedPolicyGroupEntity.getId(),
                ioe
            );
        }
    }
}
