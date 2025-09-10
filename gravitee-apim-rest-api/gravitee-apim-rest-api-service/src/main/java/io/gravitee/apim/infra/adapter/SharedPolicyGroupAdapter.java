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
import io.gravitee.rest.api.model.context.OriginContext;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;

@Slf4j
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = { JsonDeserializer.class, JsonDeserializer.class },
    injectionStrategy = InjectionStrategy.FIELD,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL
)
public abstract class SharedPolicyGroupAdapter {

    @Mapping(target = "originContext", expression = "java(toOriginContextEnum(sharedPolicyGroup.getOrigin()))")
    public abstract SharedPolicyGroup toEntity(io.gravitee.repository.management.model.SharedPolicyGroup sharedPolicyGroup);

    @Mapping(target = "origin", expression = "java(toOriginString(sharedPolicyGroupEntity.getOriginContext()))")
    @Mapping(target = "definition", source = ".", qualifiedByName = "serializeDefinition")
    public abstract io.gravitee.repository.management.model.SharedPolicyGroup fromEntity(SharedPolicyGroup sharedPolicyGroupEntity);

    public abstract io.gravitee.repository.management.model.SharedPolicyGroupLifecycleState fromEntity(
        SharedPolicyGroup.SharedPolicyGroupLifecycleState sharedPolicyGroupLifecycleState
    );

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
        } catch (IOException ioe) {
            log.error("Unexpected error while deserializing SharedPolicyGroup definition with id:" + result.getId(), ioe);
        }
    }

    @Named("serializeDefinition")
    public String serializeDefinition(SharedPolicyGroup sharedPolicyGroupEntity) {
        if (sharedPolicyGroupEntity == null) {
            return null;
        }

        try {
            var sharedPolicyGroupDefinition = SharedPolicyGroupDefinition.builder().steps(sharedPolicyGroupEntity.getSteps()).build();

            return GraviteeJacksonMapper.getInstance().writeValueAsString(sharedPolicyGroupDefinition);
        } catch (IOException ioe) {
            throw new RuntimeException(
                "Unexpected error while serializing SharedPolicyGroup definition with id:" + sharedPolicyGroupEntity.getId(),
                ioe
            );
        }
    }

    public OriginContext toOriginContextEnum(String origin) {
        if (origin == null) {
            return null;
        }

        if ("MANAGEMENT".equalsIgnoreCase(origin)) {
            return new OriginContext.Management();
        } else if ("KUBERNETES".equalsIgnoreCase(origin)) {
            return new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED);
        }

        return null;
    }

    public String toOriginString(OriginContext originContext) {
        return originContext.name();
    }
}
