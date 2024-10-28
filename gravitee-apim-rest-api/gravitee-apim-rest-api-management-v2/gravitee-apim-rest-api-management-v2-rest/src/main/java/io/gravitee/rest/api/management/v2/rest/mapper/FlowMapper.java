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

import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.management.v2.rest.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class })
public interface FlowMapper {
    FlowMapper INSTANCE = Mappers.getMapper(FlowMapper.class);

    // Flow V4
    @Mapping(target = "selectors", qualifiedByName = "mapToSelectorApiModelList")
    FlowV4 mapFromHttpV4(io.gravitee.definition.model.v4.flow.Flow flow);

    @Mapping(target = "selectors", qualifiedByName = "mapToSelectorEntityList")
    io.gravitee.definition.model.v4.flow.Flow mapToHttpV4(FlowV4 flow);

    FlowV4 mapFromNativeV4(io.gravitee.definition.model.v4.nativeapi.NativeFlow flow);

    io.gravitee.definition.model.v4.nativeapi.NativeFlow mapToNativeV4(FlowV4 flow);

    List<io.gravitee.definition.model.v4.flow.Flow> mapToHttpV4(List<FlowV4> flows);
    List<io.gravitee.definition.model.v4.nativeapi.NativeFlow> mapToNativeV4(List<FlowV4> flows);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    StepV4 mapStep(Step step);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Step mapStep(StepV4 stepV4);

    // Selectors
    HttpSelector mapSelector(io.gravitee.definition.model.v4.flow.selector.HttpSelector selector);
    ConditionSelector mapSelector(io.gravitee.definition.model.v4.flow.selector.ConditionSelector selector);
    ChannelSelector mapSelector(io.gravitee.definition.model.v4.flow.selector.ChannelSelector selector);

    io.gravitee.definition.model.v4.flow.selector.HttpSelector mapSelector(HttpSelector selector);
    io.gravitee.definition.model.v4.flow.selector.ConditionSelector mapSelector(ConditionSelector selector);
    io.gravitee.definition.model.v4.flow.selector.ChannelSelector mapSelector(ChannelSelector selector);

    @Named("mapToSelectorEntityList")
    default List<io.gravitee.definition.model.v4.flow.selector.Selector> mapToSelectorEntityList(List<Selector> selectors) {
        if (Objects.isNull(selectors)) {
            return new ArrayList<>();
        }
        return selectors
            .stream()
            .map(selector -> {
                if (selector == null) {
                    return null;
                }
                if (selector.getActualInstance() instanceof HttpSelector) {
                    return this.mapSelector(selector.getHttpSelector());
                }
                if (selector.getActualInstance() instanceof ConditionSelector) {
                    return this.mapSelector(selector.getConditionSelector());
                }
                if (selector.getActualInstance() instanceof ChannelSelector) {
                    return this.mapSelector(selector.getChannelSelector());
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    @Named("mapToSelectorApiModelList")
    default List<Selector> mapToSelectorApiModelList(List<io.gravitee.definition.model.v4.flow.selector.Selector> selectors) {
        if (Objects.isNull(selectors)) {
            return new ArrayList<>();
        }
        return selectors
            .stream()
            .map(selector -> {
                if (selector == null) {
                    return null;
                }
                if (selector.getType() == SelectorType.HTTP) {
                    return new Selector(this.mapSelector((io.gravitee.definition.model.v4.flow.selector.HttpSelector) selector));
                }
                if (selector.getType() == SelectorType.CONDITION) {
                    return new Selector(this.mapSelector((io.gravitee.definition.model.v4.flow.selector.ConditionSelector) selector));
                }
                if (selector.getType() == SelectorType.CHANNEL) {
                    return new Selector(this.mapSelector((io.gravitee.definition.model.v4.flow.selector.ChannelSelector) selector));
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    // Flow V2
    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.flow.Step mapStep(StepV2 stepV2);

    io.gravitee.definition.model.flow.Flow map(FlowV2 flowV2);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    StepV2 mapStep(io.gravitee.definition.model.flow.Step stepV2);

    FlowV2 map(io.gravitee.definition.model.flow.Flow flowV2);
}
