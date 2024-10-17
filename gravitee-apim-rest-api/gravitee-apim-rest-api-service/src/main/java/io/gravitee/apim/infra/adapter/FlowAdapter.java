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

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.selector.FlowChannelSelector;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowSelector;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(imports = { UuidString.class, TimeProvider.class })
public interface FlowAdapter {
    FlowAdapter INSTANCE = Mappers.getMapper(FlowAdapter.class);
    Logger LOGGER = LoggerFactory.getLogger(FlowAdapter.class);

    @Mapping(target = "id", expression = "java(UuidString.generateRandom())")
    @Mapping(target = "createdAt", expression = "java(java.util.Date.from(TimeProvider.instantNow()))")
    @Mapping(target = "updatedAt", expression = "java(java.util.Date.from(TimeProvider.instantNow()))")
    Flow toRepository(io.gravitee.definition.model.v4.flow.Flow source, FlowReferenceType referenceType, String referenceId, int order);

    @Mapping(target = "id", expression = "java(UuidString.generateRandom())")
    @Mapping(target = "createdAt", expression = "java(java.util.Date.from(TimeProvider.instantNow()))")
    @Mapping(target = "updatedAt", expression = "java(java.util.Date.from(TimeProvider.instantNow()))")
    Flow toRepository(NativeFlow source, FlowReferenceType referenceType, String referenceId, int order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.util.Date.from(TimeProvider.instantNow()))")
    Flow toRepositoryUpdate(@MappingTarget Flow repository, io.gravitee.definition.model.v4.flow.Flow source, int order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.util.Date.from(TimeProvider.instantNow()))")
    Flow toRepositoryUpdate(@MappingTarget Flow repository, NativeFlow source, int order);

    @Mapping(target = "id", expression = "java(UuidString.generateRandom())")
    @Mapping(target = "createdAt", expression = "java(java.util.Date.from(TimeProvider.instantNow()))")
    @Mapping(target = "updatedAt", expression = "java(java.util.Date.from(TimeProvider.instantNow()))")
    Flow toRepository(io.gravitee.definition.model.flow.Flow source, FlowReferenceType referenceType, String referenceId, int order);

    io.gravitee.definition.model.v4.flow.Flow toFlowV4(Flow source);
    List<io.gravitee.definition.model.v4.flow.Flow> toFlowV4(List<Flow> source);

    NativeFlow toNativeFlow(Flow source);
    List<NativeFlow> toNativeFlow(List<Flow> source);

    @Mapping(target = "pathOperator.path", source = "path")
    @Mapping(target = "pathOperator.operator", source = "operator")
    io.gravitee.definition.model.flow.Flow toFlowV2(Flow source);

    default FlowSelector toRepository(Selector source) {
        if (source instanceof HttpSelector) {
            return toRepository((HttpSelector) source);
        } else if (source instanceof ChannelSelector) {
            return toRepository((ChannelSelector) source);
        } else if (source instanceof ConditionSelector) {
            return toRepository((ConditionSelector) source);
        } else {
            throw new IllegalArgumentException("Unknown selector type: " + source.getClass());
        }
    }

    default Selector toModel(FlowSelector source) {
        if (source instanceof FlowHttpSelector) {
            return toModel((FlowHttpSelector) source);
        } else if (source instanceof FlowChannelSelector) {
            return toModel((FlowChannelSelector) source);
        } else if (source instanceof FlowConditionSelector) {
            return toModel((FlowConditionSelector) source);
        } else {
            throw new IllegalArgumentException("Unknown selector type: " + source.getClass());
        }
    }

    FlowHttpSelector toRepository(HttpSelector source);

    HttpSelector toModel(FlowHttpSelector source);

    FlowChannelSelector toRepository(ChannelSelector source);

    ChannelSelector toModel(FlowChannelSelector source);

    FlowConditionSelector toRepository(ConditionSelector source);

    ConditionSelector toModel(FlowConditionSelector source);

    default Flow toRepositoryFromAbstract(AbstractFlow flow, FlowReferenceType referenceType, String referenceId, int order) {
        if (flow instanceof io.gravitee.definition.model.v4.flow.Flow) {
            return this.toRepository((io.gravitee.definition.model.v4.flow.Flow) flow, referenceType, referenceId, order);
        } else if (flow instanceof NativeFlow) {
            return this.toRepository((NativeFlow) flow, referenceType, referenceId, order);
        }
        throw new IllegalArgumentException("Unknown flow: " + flow.getClass());
    }

    default Flow toRepositoryUpdateFromAbstract(Flow repository, AbstractFlow source, int order) {
        if (source instanceof io.gravitee.definition.model.v4.flow.Flow) {
            return this.toRepositoryUpdate(repository, (io.gravitee.definition.model.v4.flow.Flow) source, order);
        } else if (source instanceof NativeFlow) {
            return this.toRepositoryUpdate(repository, (NativeFlow) source, order);
        }
        throw new IllegalArgumentException("Unknown flow: " + source.getClass());
    }

    default AbstractFlow toAbstractFlow(Flow flow) {
        return null;
    }
}
